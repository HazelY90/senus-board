package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.ComparisonAnalyticsEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for reporting-period comparison analytics. */
public interface ComparisonAnalyticsRepository extends JpaRepository<ComparisonAnalyticsEntity, Long> {

    /** Finds analytics for one ordered reporting-period comparison. */
    Optional<ComparisonAnalyticsEntity> findByBasePeriodAndTargetPeriod(
            ReportingPeriodEntity basePeriod,
            ReportingPeriodEntity targetPeriod
    );
}
