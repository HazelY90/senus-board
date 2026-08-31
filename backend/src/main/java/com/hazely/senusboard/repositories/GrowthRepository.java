package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.GrowthEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for growth values. */
public interface GrowthRepository extends JpaRepository<GrowthEntity, Long> {

    /** Finds growth values for one reporting period. */
    Optional<GrowthEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
