package com.hazely.senusboard.controllers;

import com.hazely.senusboard.dtos.DataDto;
import com.hazely.senusboard.dtos.DocumentDownloadDto;
import com.hazely.senusboard.dtos.DocumentsDto;
import com.hazely.senusboard.dtos.ReportingPeriodsDto;
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.exceptions.GlobalHandler;
import com.hazely.senusboard.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DataControllerTest {

    @Mock
    private DataService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new DataController(service))
                .setControllerAdvice(new GlobalHandler())
                .build();
    }

    @Test
    void getDataReturnsMergedResponse() throws Exception {
        DataDto data = data();
        when(service.getData("FY2025")).thenReturn(data);

        mvc.perform(get("/api/v1/data/FY2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.code").value("FY2025"))
                .andExpect(jsonPath("$.period.type").value("FULL_YEAR"))
                .andExpect(jsonPath("$.growth.revenue").value(836991))
                .andExpect(jsonPath("$.growth.calculated.revenueGrowth").value(21.5996))
                .andExpect(jsonPath("$.profitability.calculated").exists())
                .andExpect(jsonPath("$.analytics.growthAnalytics").value("Revenue increased."));
    }

    @Test
    void getDataReturnsServiceError() throws Exception {
        when(service.getData("FY2099")).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reporting period not found: FY2099"
        ));

        mvc.perform(get("/api/v1/data/FY2099"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Reporting period not found: FY2099"))
                .andExpect(jsonPath("$.path").value("/api/v1/data/FY2099"));
    }

    @Test
    void getPeriodsReturnsAvailablePeriods() throws Exception {
        ReportingPeriodsDto data = new ReportingPeriodsDto(List.of(
                new ReportingPeriodsDto.PeriodDto(
                        "HY2026",
                        "Half Year 2026",
                        PeriodType.HALF_YEAR,
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2025, 12, 31),
                        true
                )
        ));
        when(service.getPeriods()).thenReturn(data);

        mvc.perform(get("/api/v1/data/reporting-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods[0].code").value("HY2026"))
                .andExpect(jsonPath("$.periods[0].isDefault").value(true));
    }

    @Test
    void getDocumentsReturnsMetadata() throws Exception {
        DocumentsDto data = new DocumentsDto(List.of(
                new DocumentsDto.DocumentDto(
                        "report.pdf",
                        "application/pdf",
                        LocalDate.of(2025, 12, 18),
                        "Summary",
                        "/api/v1/data/documents/7/download"
                )
        ));
        when(service.getDocuments()).thenReturn(data);

        mvc.perform(get("/api/v1/data/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].name").value("report.pdf"))
                .andExpect(jsonPath("$.documents[0].localPath").doesNotExist())
                .andExpect(jsonPath("$.documents[0].downloadUrl")
                        .value("/api/v1/data/documents/7/download"));
    }

    @Test
    void downloadReturnsFileHeadersAndBody() throws Exception {
        byte[] content = "document".getBytes();
        DocumentDownloadDto data = new DocumentDownloadDto(
                new ByteArrayResource(content),
                "report.pdf",
                MediaType.APPLICATION_PDF,
                content.length
        );
        when(service.getDownload(7L)).thenReturn(data);

        mvc.perform(get("/api/v1/data/documents/7/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("report.pdf")))
                .andExpect(content().bytes(content));
    }

    private DataDto data() {
        return new DataDto(
                new DataDto.PeriodDto(
                        "FY2025",
                        "Full Year 2025",
                        PeriodType.FULL_YEAR,
                        LocalDate.of(2024, 7, 1),
                        LocalDate.of(2025, 6, 30)
                ),
                new DataDto.GrowthDto(
                        new BigDecimal("836991.0000"),
                        new DataDto.GrowthCalcDto(new BigDecimal("21.5996"))
                ),
                new DataDto.ProfitabilityDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        new DataDto.ProfitabilityCalcDto(null, null, null, null)
                ),
                new DataDto.LiquidityDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new DataDto.LiquidityCalcDto(null, null, null, null, null)
                ),
                new DataDto.CapitalDto(
                        null,
                        null,
                        null,
                        null,
                        new DataDto.CapitalCalcDto(null)
                ),
                new DataDto.AnalyticsDto("Revenue increased.", null, null, null, null)
        );
    }
}
