package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.CalculatedLiquidityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for calculated liquidity values. */
public interface CalculatedLiquidityRepository
        extends JpaRepository<CalculatedLiquidityEntity, Long> {

    /** Finds calculated liquidity values for one reporting period. */
    Optional<CalculatedLiquidityEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
