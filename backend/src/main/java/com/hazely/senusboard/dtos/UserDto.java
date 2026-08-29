package com.hazely.senusboard.dtos;

import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;

/** Returns account profile data without exposing credentials. */
public record UserDto(
        Long id,
        String name,
        String email,
        String organization,
        String description,
        Role role,
        Status status
) {
}
