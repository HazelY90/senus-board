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
import com.hazely.senusboard.security.AuthProperties;
import com.hazely.senusboard.security.Jwt;
import com.hazely.senusboard.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.setEmailDomains(List.of("example.com", "company.test"));
        service = new AuthService(
                userRepo,
                encoder,
                new UserPolicy(props),
                new UserMapper(),
                authManager,
                jwtService
        );
    }

    @Test
    void registerStoresPendingUser() {
        RegisterRequestDto request = request(Role.BOARD, " User@Example.com ", "Strong!Pass1");
        when(userRepo.existsByEmail("user@example.com")).thenReturn(false);
        when(encoder.encode("Strong!Pass1")).thenReturn("encoded-password");
        when(userRepo.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });

        UserDto result = service.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals(Status.PENDING, saved.getStatus());
        assertNull(saved.getDescription());
        assertEquals(7L, result.id());
        assertEquals(Role.BOARD, result.role());
        assertEquals(Status.PENDING, result.status());
    }

    @Test
    void registerRejectsAdminRole() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.register(request(Role.ADMIN, "user@example.com", "Strong!Pass1"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(userRepo, encoder);
    }

    @Test
    void registerRejectsDisallowedDomain() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.register(request(Role.BOARD, "user@example.com.test", "Strong!Pass1"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(userRepo, encoder);
    }

    @Test
    void registerRejectsWeakPassword() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.register(request(Role.BOARD, "user@example.com", "weakpassword"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(userRepo, encoder);
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepo.existsByEmail("user@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.register(request(Role.BOARD, "user@example.com", "Strong!Pass1"))
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(encoder);
    }

    @Test
    void loginReturnsTokensForPendingUser() {
        UserEntity user = user(Status.PENDING);
        Jwt access = org.mockito.Mockito.mock(Jwt.class);
        Jwt refresh = org.mockito.Mockito.mock(Jwt.class);
        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn(access);
        when(jwtService.generateRefreshToken(user)).thenReturn(refresh);
        when(access.toString()).thenReturn("access-token");
        when(refresh.toString()).thenReturn("refresh-token");

        AuthService.LoginTokens result = service.login(
                new LoginRequestDto(" User@Example.com ", "Strong!Pass1")
        );

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    void loginRejectsUnavailableUser() {
        UserEntity user = user(Status.REJECTED);
        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequestDto("user@example.com", "Strong!Pass1"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequestDto("user@example.com", "wrong-password"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void refreshReturnsNewAccessToken() {
        UserEntity user = user(Status.ACTIVE);
        Jwt refresh = org.mockito.Mockito.mock(Jwt.class);
        Jwt access = org.mockito.Mockito.mock(Jwt.class);
        when(jwtService.parseToken("refresh-token")).thenReturn(refresh);
        when(refresh.isRefresh()).thenReturn(true);
        when(refresh.getId()).thenReturn(7L);
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn(access);
        when(access.toString()).thenReturn("new-access-token");

        AccessTokenDto result = service.refresh("refresh-token");

        assertEquals("new-access-token", result.accessToken());
    }

    @Test
    void refreshRejectsAccessToken() {
        Jwt access = org.mockito.Mockito.mock(Jwt.class);
        when(jwtService.parseToken("access-token")).thenReturn(access);
        when(access.isRefresh()).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.refresh("access-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refreshRejectsUnavailableUser() {
        UserEntity user = user(Status.DISABLED);
        Jwt refresh = org.mockito.Mockito.mock(Jwt.class);
        when(jwtService.parseToken("refresh-token")).thenReturn(refresh);
        when(refresh.isRefresh()).thenReturn(true);
        when(refresh.getId()).thenReturn(7L);
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.refresh("refresh-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void getMeReturnsLatestUser() {
        UserEntity user = user(Status.ACTIVE);
        user.setOrganization("Example Ltd");
        user.setDescription("Investor account");
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));

        UserDto result = service.getMe(7L);

        assertEquals(7L, result.id());
        assertEquals("user@example.com", result.email());
        assertEquals("Example Ltd", result.organization());
        assertEquals("Investor account", result.description());
        assertEquals(Role.BOARD, result.role());
        assertEquals(Status.ACTIVE, result.status());
    }

    @Test
    void getMeRejectsUnavailableUser() {
        when(userRepo.findById(7L)).thenReturn(Optional.of(user(Status.DISABLED)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getMe(7L)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void changePasswordStoresEncodedPassword() {
        UserEntity user = user(Status.ACTIVE);
        user.setPassword("current-hash");
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));
        when(encoder.matches("Current!Pass1", "current-hash")).thenReturn(true);
        when(encoder.encode("NewStrong!1")).thenReturn("new-hash");

        service.changePassword(
                7L,
                new ChangePasswordRequestDto("Current!Pass1", "NewStrong!1")
        );

        assertEquals("new-hash", user.getPassword());
        verify(userRepo).save(user);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        UserEntity user = user(Status.ACTIVE);
        user.setPassword("current-hash");
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));
        when(encoder.matches("Wrong!Pass1", "current-hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.changePassword(
                        7L,
                        new ChangePasswordRequestDto("Wrong!Pass1", "NewStrong!1")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepo, never()).save(any(UserEntity.class));
    }

    @Test
    void changePasswordRequiresSixteenCharactersForAdmin() {
        UserEntity user = user(Status.ACTIVE);
        user.setRole(Role.ADMIN);
        user.setPassword("current-hash");
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));
        when(encoder.matches("Current!Admin123", "current-hash")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.changePassword(
                        7L,
                        new ChangePasswordRequestDto("Current!Admin123", "Short!Pass1")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepo, never()).save(any(UserEntity.class));
    }

    private RegisterRequestDto request(Role role, String email, String password) {
        return new RegisterRequestDto(
                " Test User ",
                email,
                password,
                role,
                " Example Ltd ",
                " "
        );
    }

    private UserEntity user(Status status) {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setName("Test User");
        user.setEmail("user@example.com");
        user.setRole(Role.BOARD);
        user.setStatus(status);
        return user;
    }
}
