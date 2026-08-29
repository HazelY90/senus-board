package com.hazely.senusboard.services;

import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.entities.UserEntity;
import org.springframework.stereotype.Component;

/** Maps user persistence models to safe API responses. */
@Component
public class UserMapper {

    /** Returns a user response without exposing the password hash. */
    public UserDto toDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getOrganization(),
                user.getDescription(),
                user.getRole(),
                user.getStatus()
        );
    }
}
