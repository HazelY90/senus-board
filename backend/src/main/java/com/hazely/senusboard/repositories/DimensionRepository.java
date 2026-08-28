package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.DimensionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for dimension members. */
public interface DimensionRepository extends JpaRepository<DimensionEntity, Long> {
}
