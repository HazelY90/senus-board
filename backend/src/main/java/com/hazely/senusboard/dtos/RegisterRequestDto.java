package com.hazely.senusboard.dtos;

import com.hazely.senusboard.entities.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Carries an ordinary-user registration request. */
public record RegisterRequestDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password,
        @NotNull Role role,
        @NotBlank @Size(max = 255) String organization,
        String description
) {
}
