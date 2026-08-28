package com.hazely.senusboard.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/** Stores source-document metadata used for data provenance. */
@Entity
@Table(
        name = "source_documents",
        uniqueConstraints = @UniqueConstraint(name = "uq_source_documents_file_hash", columnNames = "file_hash")
)
@Getter
@Setter
@NoArgsConstructor
public class SourceDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "local_path", length = 1000)
    private String localPath;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
