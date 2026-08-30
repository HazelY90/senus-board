package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.ComparisonAnalyticsEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.jobs.ingestion.dtos.AiComparisonResult;
import com.hazely.senusboard.jobs.ingestion.dtos.ComparisonPair;
import com.hazely.senusboard.repositories.ComparisonAnalyticsRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Validates and stores AI analytics for ordered reporting-period comparisons. */
public class ComparisonPersistenceService {

    private final ReportingPeriodRepository periodRepo;
    private final ComparisonAnalyticsRepository comparisonRepo;

    public ComparisonPersistenceService(
            ReportingPeriodRepository periodRepo,
            ComparisonAnalyticsRepository comparisonRepo
    ) {
        this.periodRepo = periodRepo;
        this.comparisonRepo = comparisonRepo;
    }

    /** Checks whether a stored comparison already represents the supplied input. */
    @Transactional(readOnly = true)
    public boolean isCurrent(
            ReportingPeriodEntity base,
            ReportingPeriodEntity target,
            String hash
    ) {
        return comparisonRepo.findByBasePeriodAndTargetPeriod(base, target)
                .map(ComparisonAnalyticsEntity::getInputHash)
                .filter(hash::equals)
                .isPresent();
    }

    /** Upserts a complete AI response for the submitted comparison inputs. */
    @Transactional
    public void save(AiComparisonResult result, Map<ComparisonPair, String> hashes) {
        if (result == null || result.comparisons() == null) {
            throw new IllegalArgumentException("comparison analytics are required");
        }
        if (hashes == null || hashes.isEmpty()) {
            throw new IllegalArgumentException("comparison input hashes are required");
        }

        Set<ComparisonPair> pairs = new HashSet<>();
        for (AiComparisonResult.ComparisonAnalytics item : result.comparisons()) {
            if (item == null) {
                throw new IllegalArgumentException("comparison analytics item is required");
            }
            ComparisonPair pair = new ComparisonPair(
                    requireText(item.basePeriodCode(), "basePeriodCode"),
                    requireText(item.targetPeriodCode(), "targetPeriodCode")
            );
            if (!pairs.add(pair)) {
                throw new IllegalArgumentException("Duplicate analytics comparison: " + pair);
            }
            String hash = hashes.get(pair);
            if (hash == null) {
                throw new IllegalArgumentException("Unexpected analytics comparison: " + pair);
            }

            ReportingPeriodEntity base = period(pair.basePeriodCode());
            ReportingPeriodEntity target = period(pair.targetPeriodCode());
            validate(base, target);
            ComparisonAnalyticsEntity row = comparisonRepo
                    .findByBasePeriodAndTargetPeriod(base, target)
                    .orElseGet(ComparisonAnalyticsEntity::new);
            row.setBasePeriod(base);
            row.setTargetPeriod(target);
            row.setGrowthAnalytics(item.growthAnalytics());
            row.setProfitabilityAnalytics(item.profitabilityAnalytics());
            row.setLiquidityAnalytics(item.liquidityAnalytics());
            row.setCapitalAnalytics(item.capitalAnalytics());
            row.setTotalAnalytics(item.totalAnalytics());
            row.setInputHash(hash);
            comparisonRepo.save(row);
        }

        if (!pairs.equals(hashes.keySet())) {
            throw new IllegalArgumentException("Comparison analytics response is incomplete");
        }
    }

    private ReportingPeriodEntity period(String code) {
        return periodRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown comparison period: " + code
                ));
    }

    private void validate(ReportingPeriodEntity base, ReportingPeriodEntity target) {
        if (base.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Comparison periods must be different");
        }
        if (base.getPeriodType() != target.getPeriodType()) {
            throw new IllegalArgumentException("Comparison period types must match");
        }
        if (!base.getEndDate().isBefore(target.getEndDate())) {
            throw new IllegalArgumentException("Comparison periods are not ordered");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
