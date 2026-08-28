package com.hazely.senusboard.services;

import com.hazely.senusboard.dtos.DataDto;
import com.hazely.senusboard.dtos.DocumentDownloadDto;
import com.hazely.senusboard.dtos.DocumentsDto;
import com.hazely.senusboard.dtos.ReportingPeriodsDto;
import com.hazely.senusboard.entities.AnalyticsEntity;
import com.hazely.senusboard.entities.CalculatedGrowthEntity;
import com.hazely.senusboard.entities.GrowthEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.entities.SourceDocumentEntity;
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.repositories.AnalyticsRepository;
import com.hazely.senusboard.repositories.CalculatedCapitalRepository;
import com.hazely.senusboard.repositories.CalculatedGrowthRepository;
import com.hazely.senusboard.repositories.CalculatedLiquidityRepository;
import com.hazely.senusboard.repositories.CalculatedProfitabilityRepository;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import com.hazely.senusboard.repositories.SourceDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataServiceTest {

    @Mock
    private ReportingPeriodRepository periodRepo;
    @Mock
    private GrowthRepository growthRepo;
    @Mock
    private CalculatedGrowthRepository growthCalcRepo;
    @Mock
    private ProfitabilityRepository profitabilityRepo;
    @Mock
    private CalculatedProfitabilityRepository profitabilityCalcRepo;
    @Mock
    private LiquidityRepository liquidityRepo;
    @Mock
    private CalculatedLiquidityRepository liquidityCalcRepo;
    @Mock
    private CapitalRepository capitalRepo;
    @Mock
    private CalculatedCapitalRepository capitalCalcRepo;
    @Mock
    private AnalyticsRepository analyticsRepo;
    @Mock
    private SourceDocumentRepository sourceRepo;

    @TempDir
    private Path base;

    private DataService service;

    @BeforeEach
    void setUp() {
        service = new DataService(
                periodRepo,
                growthRepo,
                growthCalcRepo,
                profitabilityRepo,
                profitabilityCalcRepo,
                liquidityRepo,
                liquidityCalcRepo,
                capitalRepo,
                capitalCalcRepo,
                analyticsRepo,
                sourceRepo
        );
        ReflectionTestUtils.setField(service, "baseUrl", base.toString());
    }

    @Test
    void getDataCombinesStoredRows() {
        ReportingPeriodEntity period = period();
        GrowthEntity growth = new GrowthEntity();
        growth.setRevenue(new BigDecimal("836991.0000"));
        CalculatedGrowthEntity calc = new CalculatedGrowthEntity();
        calc.setRevenueGrowth(new BigDecimal("21.5996"));
        AnalyticsEntity analytics = new AnalyticsEntity();
        analytics.setGrowthAnalytics("Revenue increased against FY2024.");

        when(periodRepo.findByCode("FY2025")).thenReturn(Optional.of(period));
        when(growthRepo.findByReportingPeriod(period)).thenReturn(Optional.of(growth));
        when(growthCalcRepo.findByReportingPeriod(period)).thenReturn(Optional.of(calc));
        when(analyticsRepo.findByReportingPeriod(period)).thenReturn(Optional.of(analytics));

        DataDto data = service.getData(" FY2025 ");

        assertEquals("FY2025", data.period().code());
        assertEquals(PeriodType.FULL_YEAR, data.period().type());
        assertEquals(new BigDecimal("836991.0000"), data.growth().revenue());
        assertEquals(new BigDecimal("21.5996"), data.growth().calculated().revenueGrowth());
        assertEquals("Revenue increased against FY2024.", data.analytics().growthAnalytics());
        assertNotNull(data.profitability());
        assertNotNull(data.profitability().calculated());
        assertNull(data.profitability().grossProfit());
        assertNotNull(data.liquidity());
        assertNotNull(data.capital());
    }

    @Test
    void getDataRejectsBlankCode() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getData(" ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(periodRepo);
    }

    @Test
    void getDataRejectsUnknownCode() {
        when(periodRepo.findByCode("FY2099")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getData("FY2099")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getPeriodsSortsLatestFirst() {
        ReportingPeriodEntity fy = period();
        ReportingPeriodEntity hy = new ReportingPeriodEntity();
        hy.setCode("HY2026");
        hy.setLabel("Half Year 2026");
        hy.setPeriodType(PeriodType.HALF_YEAR);
        hy.setStartDate(LocalDate.of(2025, 7, 1));
        hy.setEndDate(LocalDate.of(2025, 12, 31));
        when(periodRepo.findAll()).thenReturn(List.of(fy, hy));

        ReportingPeriodsDto data = service.getPeriods();

        assertEquals(2, data.periods().size());
        assertEquals("HY2026", data.periods().getFirst().code());
        assertEquals(true, data.periods().getFirst().isDefault());
        assertEquals(false, data.periods().getLast().isDefault());
    }

    @Test
    void getDocumentsSortsAndHidesUnavailablePaths() throws Exception {
        Files.writeString(base.resolve("latest.pdf"), "document", UTF_8);
        SourceDocumentEntity latest = document(
                2L,
                "latest.pdf",
                LocalDate.of(2025, 12, 18),
                "latest.pdf",
                Instant.parse("2025-12-18T10:00:00Z")
        );
        SourceDocumentEntity older = document(
                1L,
                "older.pdf",
                LocalDate.of(2024, 12, 18),
                "missing.pdf",
                Instant.parse("2024-12-18T10:00:00Z")
        );
        when(sourceRepo.findAll()).thenReturn(List.of(older, latest));

        DocumentsDto data = service.getDocuments();

        assertEquals("latest.pdf", data.documents().getFirst().name());
        assertEquals(
                "/api/v1/data/documents/2/download",
                data.documents().getFirst().downloadUrl()
        );
        assertNull(data.documents().getLast().downloadUrl());
    }

    @Test
    void getDownloadReturnsValidatedFile() throws Exception {
        byte[] content = "document".getBytes(UTF_8);
        Files.write(base.resolve("report.pdf"), content);
        SourceDocumentEntity document = document(
                7L,
                "report.pdf",
                LocalDate.of(2025, 12, 18),
                "report.pdf",
                Instant.parse("2025-12-18T10:00:00Z")
        );
        when(sourceRepo.findById(7L)).thenReturn(Optional.of(document));

        DocumentDownloadDto data = service.getDownload(7L);

        assertEquals("report.pdf", data.name());
        assertEquals(content.length, data.size());
        assertArrayEquals(content, data.resource().getInputStream().readAllBytes());
    }

    @Test
    void getDownloadRejectsUnavailableFile() {
        SourceDocumentEntity document = document(
                7L,
                "report.pdf",
                LocalDate.of(2025, 12, 18),
                "../report.pdf",
                Instant.parse("2025-12-18T10:00:00Z")
        );
        when(sourceRepo.findById(7L)).thenReturn(Optional.of(document));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getDownload(7L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private ReportingPeriodEntity period() {
        ReportingPeriodEntity period = new ReportingPeriodEntity();
        period.setCode("FY2025");
        period.setLabel("Full Year 2025");
        period.setPeriodType(PeriodType.FULL_YEAR);
        period.setStartDate(LocalDate.of(2024, 7, 1));
        period.setEndDate(LocalDate.of(2025, 6, 30));
        return period;
    }

    private SourceDocumentEntity document(
            Long id,
            String name,
            LocalDate publicationDate,
            String localPath,
            Instant createdAt
    ) {
        SourceDocumentEntity document = new SourceDocumentEntity();
        document.setId(id);
        document.setName(name);
        document.setDocumentType("application/pdf");
        document.setPublicationDate(publicationDate);
        document.setLocalPath(localPath);
        document.setAiSummary("Summary");
        document.setCreatedAt(createdAt);
        return document;
    }
}
