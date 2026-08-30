package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.CalculatedProfitabilityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for calculated profitability values. */
public interface CalculatedProfitabilityRepository
        extends JpaRepository<CalculatedProfitabilityEntity, Long> {

    /** Finds calculated profitability values for one reporting period. */
    Optional<CalculatedProfitabilityEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
