package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.ExtractionItemEntity;
import com.hazely.senusboard.entities.enums.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Provides persistence operations for extraction items. */
public interface ExtractionItemRepository extends JpaRepository<ExtractionItemEntity, Long> {

    /** Loads extraction items in a review state for one ingestion run. */
    List<ExtractionItemEntity> findAllByIngestionRunIdAndValidationStatus(
            Long runId,
            ValidationStatus status
    );
}
