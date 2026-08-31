package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.CapitalEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for capital values. */
public interface CapitalRepository extends JpaRepository<CapitalEntity, Long> {

    /** Finds capital values for one reporting period. */
    Optional<CapitalEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
