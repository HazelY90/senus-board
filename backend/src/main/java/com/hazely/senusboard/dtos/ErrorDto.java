package com.hazely.senusboard.dtos;

import java.time.Instant;

/** Returns a consistent API error response. */
public record ErrorDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
