package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.DimensionEntity;
import com.hazely.senusboard.entities.MetricEntity;
import com.hazely.senusboard.entities.MetricValueEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for validated metric values. */
public interface MetricValueRepository extends JpaRepository<MetricValueEntity, Long> {

    /** Finds the formal value identified by the database uniqueness key. */
    Optional<MetricValueEntity> findByPeriodAndMetricAndDimension(
            ReportingPeriodEntity period,
            MetricEntity metric,
            DimensionEntity dimension
    );
}
