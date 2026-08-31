package com.hazely.senusboard.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Carries credentials for an authentication request. */
public record LoginRequestDto(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password
) {
}
