package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.SourceDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for source documents. */
public interface SourceDocumentRepository extends JpaRepository<SourceDocumentEntity, Long> {

    Optional<SourceDocumentEntity> findByFileHash(String fileHash);
}
