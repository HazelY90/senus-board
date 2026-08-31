package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.IngestionRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for ingestion runs. */
public interface IngestionRunRepository extends JpaRepository<IngestionRunEntity, Long> {
}
