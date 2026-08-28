package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.CalculatedGrowthEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for calculated growth values. */
public interface CalculatedGrowthRepository extends JpaRepository<CalculatedGrowthEntity, Long> {

    /** Finds calculated growth values for one reporting period. */
    Optional<CalculatedGrowthEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
