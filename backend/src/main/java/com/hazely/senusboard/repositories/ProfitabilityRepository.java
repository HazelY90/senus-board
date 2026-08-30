package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.ProfitabilityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for profitability values. */
public interface ProfitabilityRepository extends JpaRepository<ProfitabilityEntity, Long> {

    /** Finds profitability values for one reporting period. */
    Optional<ProfitabilityEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
