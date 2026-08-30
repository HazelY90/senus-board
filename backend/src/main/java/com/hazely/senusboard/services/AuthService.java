package com.hazely.senusboard.services;

import com.hazely.senusboard.dtos.AccessTokenDto;
import com.hazely.senusboard.dtos.ChangePasswordRequestDto;
import com.hazely.senusboard.dtos.LoginRequestDto;
import com.hazely.senusboard.dtos.RegisterRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.repositories.UserRepository;
import com.hazely.senusboard.security.Jwt;
import com.hazely.senusboard.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Handles registration and other authentication workflows. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserPolicy policy;
    private final UserMapper mapper;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    /** Validates and stores a new ordinary account. */
    @Transactional
    public UserDto register(RegisterRequestDto request) {
        String email = policy.cleanEmail(request.email());
        policy.validateRole(request.role());
        policy.validateDomain(email);
        policy.validatePassword(request.password(), request.role());

        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(encoder.encode(request.password()));
        user.setRole(request.role());
        user.setOrganization(request.organization().trim());
        user.setDescription(clean(request.description()));
        user.setStatus(Status.PENDING);

        UserEntity saved = userRepo.save(user);
        return mapper.toDto(saved);
    }

    /** Authenticates an available account and issues a token pair. */
    @Transactional(readOnly = true)
    public LoginTokens login(LoginRequestDto request) {
        String email = policy.cleanEmail(request.email());
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException ex) {
            throw unauthorized();
        }

        UserEntity user = userRepo.findByEmail(email).orElseThrow(this::unauthorized);
        if (!policy.isAvailable(user.getStatus())) {
            throw unauthorized();
        }

        return new LoginTokens(
                jwtService.generateAccessToken(user).toString(),
                jwtService.generateRefreshToken(user).toString()
        );
    }

    /** Validates a refresh token and issues a new access token. */
    @Transactional(readOnly = true)
    public AccessTokenDto refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw unauthorized();
        }

        Jwt jwt = jwtService.parseToken(refreshToken);
        if (jwt == null || jwt.isExpired() || !jwt.isRefresh()) {
            throw unauthorized();
        }

        UserEntity user = userRepo.findById(jwt.getId()).orElseThrow(this::unauthorized);
        if (!policy.isAvailable(user.getStatus())) {
            throw unauthorized();
        }

        String accessToken = jwtService.generateAccessToken(user).toString();
        return new AccessTokenDto(accessToken);
    }

    /** Returns the latest profile for an available authenticated account. */
    @Transactional(readOnly = true)
    public UserDto getMe(Long id) {
        if (id == null || id <= 0) {
            throw unauthorized();
        }

        UserEntity user = userRepo.findById(id).orElseThrow(this::unauthorized);
        if (!policy.isAvailable(user.getStatus())) {
            throw unauthorized();
        }
        return mapper.toDto(user);
    }

    /** Verifies and replaces the password for an available account. */
    @Transactional
    public void changePassword(Long id, ChangePasswordRequestDto request) {
        if (id == null || id <= 0) {
            throw unauthorized();
        }

        UserEntity user = userRepo.findById(id).orElseThrow(this::unauthorized);
        if (!policy.isAvailable(user.getStatus())) {
            throw unauthorized();
        }
        if (!encoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        policy.validatePassword(request.newPassword(), user.getRole());
        user.setPassword(encoder.encode(request.newPassword()));
        userRepo.save(user);
    }

    /** Permanently deletes the authenticated account. */
    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw unauthorized();
        }

        UserEntity user = userRepo.findById(id).orElseThrow(this::unauthorized);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be deleted through this endpoint"
            );
        }
        userRepo.delete(user);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    /** Carries tokens from the service without exposing the refresh token in JSON. */
    public record LoginTokens(String accessToken, String refreshToken) {
    }
}
