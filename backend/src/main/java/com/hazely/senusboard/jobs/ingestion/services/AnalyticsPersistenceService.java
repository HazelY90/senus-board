package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.AnalyticsEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.jobs.ingestion.dtos.AiAnalyticsResult;
import com.hazely.senusboard.repositories.AnalyticsRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates and stores AI analytics independently from numeric ingestion. */
public class AnalyticsPersistenceService {

    private final ReportingPeriodRepository periodRepo;
    private final AnalyticsRepository analyticsRepo;

    public AnalyticsPersistenceService(
            ReportingPeriodRepository periodRepo,
            AnalyticsRepository analyticsRepo
    ) {
        this.periodRepo = periodRepo;
        this.analyticsRepo = analyticsRepo;
    }

    /** Upserts analytics returned for known reporting periods. */
    @Transactional
    public void save(AiAnalyticsResult result) {
        if (result == null || result.periods() == null) {
            throw new IllegalArgumentException("analytics periods are required");
        }
        savePeriods(result.periods());
    }

    private void savePeriods(List<AiAnalyticsResult.PeriodAnalytics> items) {
        Set<String> codes = new HashSet<>();
        for (AiAnalyticsResult.PeriodAnalytics item : items) {
            String code = requireText(item.periodCode(), "periodCode");
            if (!codes.add(code)) {
                throw new IllegalArgumentException("Duplicate analytics period: " + code);
            }
            ReportingPeriodEntity period = periodRepo.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown analytics period: " + code));
            AnalyticsEntity row = analyticsRepo.findByReportingPeriod(period)
                    .orElseGet(AnalyticsEntity::new);
            row.setReportingPeriod(period);
            row.setGrowthAnalytics(item.growthAnalytics());
            row.setProfitabilityAnalytics(item.profitabilityAnalytics());
            row.setLiquidityAnalytics(item.liquidityAnalytics());
            row.setCapitalAnalytics(item.capitalAnalytics());
            row.setTotalAnalytics(item.totalAnalytics());
            analyticsRepo.save(row);
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
