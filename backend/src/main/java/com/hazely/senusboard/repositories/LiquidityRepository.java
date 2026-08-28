package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.LiquidityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for liquidity values. */
public interface LiquidityRepository extends JpaRepository<LiquidityEntity, Long> {

    /** Finds liquidity values for one reporting period. */
    Optional<LiquidityEntity> findByReportingPeriod(ReportingPeriodEntity period);
}
