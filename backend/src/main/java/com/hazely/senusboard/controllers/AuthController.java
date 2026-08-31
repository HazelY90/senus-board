package com.hazely.senusboard.controllers;

import com.hazely.senusboard.dtos.AccessTokenDto;
import com.hazely.senusboard.dtos.ChangePasswordRequestDto;
import com.hazely.senusboard.dtos.LoginRequestDto;
import com.hazely.senusboard.dtos.RegisterRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.security.AccountEventService;
import com.hazely.senusboard.security.JwtConfig;
import com.hazely.senusboard.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

/** Exposes public authentication operations. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final JwtConfig jwtConfig;
    private final AccountEventService eventService;

    /** Registers an ordinary account in the pending review state. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequestDto request) {
        return service.register(request);
    }

    /** Authenticates an account and returns access and refresh credentials. */
    @PostMapping("/login")
    public ResponseEntity<AccessTokenDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        AuthService.LoginTokens tokens = service.login(request);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(httpRequest.isSecure())
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(jwtConfig.getRefreshTokenExpiration()))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AccessTokenDto(tokens.accessToken()));
    }

    /** Issues a new access token from the refresh-token cookie. */
    @PostMapping("/refresh")
    public AccessTokenDto refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        return service.refresh(refreshToken);
    }

    /** Clears the browser refresh-token cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        return noContentWithExpiredCookie(request);
    }

    /** Permanently deletes the authenticated account and clears its refresh cookie. */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            HttpServletRequest request
    ) {
        service.delete(getId(authentication));
        return noContentWithExpiredCookie(request);
    }

    /** Returns the current account profile from the authenticated identity. */
    @GetMapping("/me")
    public UserDto getMe(Authentication authentication) {
        return service.getMe(getId(authentication));
    }

    /** Opens an authenticated stream for account-access events. */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(Authentication authentication) {
        return eventService.connect(getId(authentication));
    }

    /** Replaces the current account password after verifying the existing password. */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            Authentication authentication
    ) {
        service.changePassword(getId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> noContentWithExpiredCookie(HttpServletRequest request) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private Long getId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return id;
    }
}
