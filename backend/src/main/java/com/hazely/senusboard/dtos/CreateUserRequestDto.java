package com.hazely.senusboard.dtos;

import com.hazely.senusboard.entities.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Carries an Admin request to create an ordinary account. */
public record CreateUserRequestDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password,
        @NotNull Role role,
        @NotBlank @Size(max = 255) String organization
) {
}
