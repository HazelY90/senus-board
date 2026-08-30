package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.AnalyticsEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for period analytics. */
public interface AnalyticsRepository extends JpaRepository<AnalyticsEntity, Long> {

    /** Finds analytics for one reporting period. */
    Optional<AnalyticsEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
