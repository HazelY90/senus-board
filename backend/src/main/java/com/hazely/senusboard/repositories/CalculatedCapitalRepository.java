package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.CalculatedCapitalEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for calculated capital values. */
public interface CalculatedCapitalRepository extends JpaRepository<CalculatedCapitalEntity, Long> {

    /** Finds calculated capital values for one reporting period. */
    Optional<CalculatedCapitalEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
