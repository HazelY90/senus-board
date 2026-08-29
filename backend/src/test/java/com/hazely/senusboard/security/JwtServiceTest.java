package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-with-at-least-32-bytes");
        config.setAccessTokenExpiration(1800);
        config.setRefreshTokenExpiration(604800);
        service = new JwtService(config);
    }

    @Test
    void accessTokenContainsUserClaims() {
        Jwt jwt = service.parseToken(service.generateAccessToken(user()).toString());

        assertEquals(7L, ((Number) jwt.getClaims().get("id")).longValue());
        assertEquals("Test User", jwt.getClaims().get("name"));
        assertEquals("user@senus.ie", jwt.getClaims().get("email"));
        assertEquals(Role.BOARD, jwt.getRole());
        assertEquals(Status.PENDING, jwt.getStatus());
        assertTrue(jwt.isAccess());
    }

    @Test
    void refreshTokenHasRefreshType() {
        Jwt jwt = service.parseToken(service.generateRefreshToken(user()).toString());

        assertFalse(jwt.isAccess());
        assertEquals("REFRESH", jwt.getClaims().get("type"));
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setName("Test User");
        user.setEmail("user@senus.ie");
        user.setRole(Role.BOARD);
        user.setStatus(Status.PENDING);
        return user;
    }
}
