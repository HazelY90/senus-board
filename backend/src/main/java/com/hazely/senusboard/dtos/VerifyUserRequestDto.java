package com.hazely.senusboard.dtos;

import jakarta.validation.constraints.NotNull;

/** Carries an Admin registration-review decision. */
public record VerifyUserRequestDto(@NotNull Boolean isApproved) {
}
