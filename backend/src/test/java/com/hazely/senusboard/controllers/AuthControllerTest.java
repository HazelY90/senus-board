package com.hazely.senusboard.controllers;

import com.hazely.senusboard.dtos.AccessTokenDto;
import com.hazely.senusboard.dtos.ChangePasswordRequestDto;
import com.hazely.senusboard.dtos.LoginRequestDto;
import com.hazely.senusboard.dtos.RegisterRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.security.AccountEventService;
import com.hazely.senusboard.security.JwtConfig;
import com.hazely.senusboard.services.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    private MockMvc mvc;
    private AccountEventService eventService;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setRefreshTokenExpiration(604800);
        eventService = new AccountEventService();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service, config, eventService)).build();
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        UserDto user = new UserDto(
                7L,
                "Test User",
                "user@example.com",
                "Example Ltd",
                null,
                Role.BOARD,
                Status.PENDING
        );
        when(service.register(any(RegisterRequestDto.class))).thenReturn(user);

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "user@example.com",
                                  "password": "Strong!Pass1",
                                  "role": "BOARD",
                                  "organization": "Example Ltd"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("BOARD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginReturnsAccessTokenAndRefreshCookie() throws Exception {
        when(service.login(any(LoginRequestDto.class))).thenReturn(
                new AuthService.LoginTokens("access-token", "refresh-token")
        );

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "Strong!Pass1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(
                        "refreshToken=refresh-token"
                )))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(
                        "HttpOnly"
                )))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(
                        "SameSite=Strict"
                ))));
    }

    @Test
    void refreshReadsCookieAndReturnsAccessToken() throws Exception {
        when(service.refresh("refresh-token")).thenReturn(new AccessTokenDto("new-access-token"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void getMeReturnsAuthenticatedUser() throws Exception {
        UserDto user = new UserDto(
                7L,
                "Test User",
                "user@example.com",
                "Example Ltd",
                "Investor account",
                Role.BOARD,
                Status.ACTIVE
        );
        when(service.getMe(7L)).thenReturn(user);

        mvc.perform(get("/api/v1/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken(7L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.organization").value("Example Ltd"))
                .andExpect(jsonPath("$.description").value("Investor account"))
                .andExpect(jsonPath("$.role").value("BOARD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void eventsStreamsAccountRevocation() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/events")
                        .principal(new UsernamePasswordAuthenticationToken(7L, null)))
                .andExpect(request().asyncStarted())
                .andReturn();

        eventService.revoke(7L, Status.DISABLED);

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "event:account-access-revoked"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DISABLED")));
    }

    @Test
    void changePasswordReturnsNoContent() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password")
                        .principal(new UsernamePasswordAuthenticationToken(7L, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Current!Pass1",
                                  "newPassword": "NewStrong!1"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(service).changePassword(
                7L,
                new ChangePasswordRequestDto("Current!Pass1", "NewStrong!1")
        );
    }
}
