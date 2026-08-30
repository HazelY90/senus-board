package com.hazely.senusboard.bootstrap;

import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.repositories.UserRepository;
import com.hazely.senusboard.security.AuthProperties;
import com.hazely.senusboard.services.UserPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder encoder;

    private AdminBootstrapProperties props;
    private AdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        props = new AdminBootstrapProperties();
        props.setEnabled(true);
        props.setEmail(" Admin@Senus.ie ");
        props.setPassword("StrongAdmin!Pass123");
        props.setName(" System Admin ");
        props.setOrganization(" Senus ");

        AuthProperties authProps = new AuthProperties();
        authProps.setEmailDomains(List.of("senus.ie"));
        bootstrap = new AdminBootstrap(
                props,
                userRepo,
                encoder,
                new UserPolicy(authProps)
        );
    }

    @Test
    void runCreatesInitialAdmin() throws Exception {
        when(userRepo.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepo.existsByEmail("admin@senus.ie")).thenReturn(false);
        when(encoder.encode("StrongAdmin!Pass123")).thenReturn("encoded-password");

        bootstrap.run(null);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("System Admin", saved.getName());
        assertEquals("admin@senus.ie", saved.getEmail());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals(Role.ADMIN, saved.getRole());
        assertEquals(Status.ACTIVE, saved.getStatus());
        assertEquals("Senus", saved.getOrganization());
        assertNull(props.getPassword());
    }

    @Test
    void runSkipsExistingAdmin() throws Exception {
        when(userRepo.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrap.run(null);

        verify(userRepo, never()).save(any(UserEntity.class));
        verify(encoder, never()).encode(any());
        assertNull(props.getPassword());
    }
}
