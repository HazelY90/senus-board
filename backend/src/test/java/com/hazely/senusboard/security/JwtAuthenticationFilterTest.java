package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepo;
    @Mock
    private Jwt jwt;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userRepo);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterAuthenticatesAvailableUser() throws Exception {
        UserEntity user = user(Status.ACTIVE);
        when(jwtService.parseToken("access-token")).thenReturn(jwt);
        when(jwt.isAccess()).thenReturn(true);
        when(jwt.getId()).thenReturn(7L);
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));

        filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(7L, authentication.getPrincipal());
        assertEquals("ROLE_BOARD", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void filterRejectsDisabledUserWithValidToken() throws Exception {
        when(jwtService.parseToken("access-token")).thenReturn(jwt);
        when(jwt.isAccess()).thenReturn(true);
        when(jwt.getId()).thenReturn(7L);
        when(userRepo.findById(7L)).thenReturn(Optional.of(user(Status.DISABLED)));

        filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        return request;
    }

    private UserEntity user(Status status) {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setRole(Role.BOARD);
        user.setStatus(status);
        return user;
    }
}
