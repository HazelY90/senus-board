package com.hazely.senusboard.dtos;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/** Carries a validated source document to the HTTP response layer. */
public record DocumentDownloadDto(
        Resource resource,
        String name,
        MediaType mediaType,
        long size
) {
}
