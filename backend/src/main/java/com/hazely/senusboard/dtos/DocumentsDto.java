package com.hazely.senusboard.dtos;

import java.time.LocalDate;
import java.util.List;

/** Returns source-document metadata without exposing local file paths. */
public record DocumentsDto(List<DocumentDto> documents) {

    /** Describes one source document available to the client. */
    public record DocumentDto(
            String name,
            String type,
            LocalDate publicationDate,
            String aiSummary,
            String downloadUrl
    ) {
    }
}
