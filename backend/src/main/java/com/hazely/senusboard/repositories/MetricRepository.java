package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.MetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for metric definitions. */
public interface MetricRepository extends JpaRepository<MetricEntity, Long> {
}
