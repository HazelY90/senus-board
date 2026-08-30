package com.hazely.senusboard.services;

import com.hazely.senusboard.dtos.CreateUserRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.events.AccountStatusEvent;
import com.hazely.senusboard.repositories.UserRepository;
import com.hazely.senusboard.security.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
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
class AdminServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private ApplicationEventPublisher publisher;

    private AdminService service;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.setEmailDomains(List.of("example.com"));
        service = new AdminService(
                userRepo,
                encoder,
                new UserPolicy(props),
                new UserMapper(),
                publisher
        );
    }

    @Test
    void createUserStoresActiveAccount() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                " Test User ",
                " User@Example.com ",
                "Strong!Pass1",
                Role.EQUITY_INVESTOR,
                " Example Ltd "
        );
        when(userRepo.existsByEmail("user@example.com")).thenReturn(false);
        when(encoder.encode("Strong!Pass1")).thenReturn("encoded-password");
        when(userRepo.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(8L);
            return user;
        });

        UserDto result = service.createUser(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("Test User", saved.getName());
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals("Example Ltd", saved.getOrganization());
        assertEquals(Status.ACTIVE, saved.getStatus());
        assertNull(saved.getDescription());
        assertEquals(8L, result.id());
        assertEquals(Role.EQUITY_INVESTOR, result.role());
        assertEquals(Status.ACTIVE, result.status());
    }

    @Test
    void createUserRejectsAdminRole() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "Admin",
                "admin@example.com",
                "Strong!AdminPass123",
                Role.ADMIN,
                "Example Ltd"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createUser(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(userRepo, encoder);
    }

    @Test
    void getPendingReturnsMappedUsers() {
        UserEntity first = user(9L, "First User", "first@example.com");
        UserEntity second = user(10L, "Second User", "second@example.com");
        when(userRepo.findAllByStatus(Status.PENDING)).thenReturn(List.of(first, second));

        List<UserDto> result = service.getPending();

        assertEquals(2, result.size());
        assertEquals(9L, result.getFirst().id());
        assertEquals("first@example.com", result.getFirst().email());
        assertEquals(Status.PENDING, result.getFirst().status());
        assertEquals(10L, result.getLast().id());
    }

    @Test
    void getPendingReturnsEmptyList() {
        when(userRepo.findAllByStatus(Status.PENDING)).thenReturn(List.of());

        List<UserDto> result = service.getPending();

        assertEquals(List.of(), result);
    }

    @Test
    void searchUserReturnsNormalisedMatch() {
        UserEntity user = user(9L, "Active User", "active@example.com");
        user.setStatus(Status.ACTIVE);
        when(userRepo.findByEmail("active@example.com")).thenReturn(Optional.of(user));

        UserDto result = service.searchUser(" Active@Example.com ");

        assertEquals(9L, result.id());
        assertEquals("active@example.com", result.email());
        assertEquals(Status.ACTIVE, result.status());
    }

    @Test
    void searchUserRejectsMissingAccount() {
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.searchUser("missing@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void verifyUserApprovesPendingAccount() {
        UserEntity user = user(9L, "Pending User", "pending@example.com");
        when(userRepo.findById(9L)).thenReturn(java.util.Optional.of(user));

        service.verifyUser(9L, true);

        assertEquals(Status.ACTIVE, user.getStatus());
        verify(userRepo).save(user);
        verify(publisher, never()).publishEvent(any(AccountStatusEvent.class));
    }

    @Test
    void verifyUserRejectsPendingAccount() {
        UserEntity user = user(9L, "Pending User", "pending@example.com");
        when(userRepo.findById(9L)).thenReturn(java.util.Optional.of(user));

        service.verifyUser(9L, false);

        assertEquals(Status.REJECTED, user.getStatus());
        verify(userRepo).save(user);
        verify(publisher).publishEvent(new AccountStatusEvent(9L, Status.REJECTED));
    }

    @Test
    void verifyUserRejectsReviewedAccount() {
        UserEntity user = user(9L, "Active User", "active@example.com");
        user.setStatus(Status.ACTIVE);
        when(userRepo.findById(9L)).thenReturn(java.util.Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.verifyUser(9L, true)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void disableUserDisablesActiveAccount() {
        UserEntity user = user(9L, "Active User", "active@example.com");
        user.setStatus(Status.ACTIVE);
        when(userRepo.findById(9L)).thenReturn(java.util.Optional.of(user));

        service.disableUser(9L);

        assertEquals(Status.DISABLED, user.getStatus());
        verify(userRepo).save(user);
        verify(publisher).publishEvent(new AccountStatusEvent(9L, Status.DISABLED));
    }

    @Test
    void disableUserRejectsAdminAccount() {
        UserEntity user = user(9L, "Admin", "admin@example.com");
        user.setRole(Role.ADMIN);
        user.setStatus(Status.ACTIVE);
        when(userRepo.findById(9L)).thenReturn(java.util.Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.disableUser(9L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deleteUserRemovesOrdinaryAccount() {
        UserEntity user = user(9L, "Active User", "active@example.com");
        when(userRepo.findById(9L)).thenReturn(Optional.of(user));

        service.deleteUser(9L);

        verify(userRepo).delete(user);
    }

    @Test
    void deleteUserRejectsAdminAccount() {
        UserEntity user = user(9L, "Admin", "admin@example.com");
        user.setRole(Role.ADMIN);
        when(userRepo.findById(9L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteUser(9L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(userRepo, never()).delete(any(UserEntity.class));
    }

    private UserEntity user(Long id, String name, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setOrganization("Example Ltd");
        user.setRole(Role.BOARD);
        user.setStatus(Status.PENDING);
        return user;
    }
}
