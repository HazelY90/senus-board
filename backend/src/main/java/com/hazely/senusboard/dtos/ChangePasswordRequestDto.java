package com.hazely.senusboard.dtos;

import jakarta.validation.constraints.NotBlank;

/** Carries the current and replacement passwords for an authenticated account. */
public record ChangePasswordRequestDto(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
