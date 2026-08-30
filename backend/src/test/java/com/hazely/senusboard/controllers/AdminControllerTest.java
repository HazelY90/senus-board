package com.hazely.senusboard.controllers;

import com.hazely.senusboard.dtos.CreateUserRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.services.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(service)).build();
    }

    @Test
    void createUserReturnsCreatedAccount() throws Exception {
        UserDto user = new UserDto(
                8L,
                "Test User",
                "user@example.com",
                "Example Ltd",
                null,
                Role.EQUITY_INVESTOR,
                Status.ACTIVE
        );
        when(service.createUser(any(CreateUserRequestDto.class))).thenReturn(user);

        mvc.perform(post("/api/v1/admin/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "user@example.com",
                                  "password": "Strong!Pass1",
                                  "role": "EQUITY_INVESTOR",
                                  "organization": "Example Ltd"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("EQUITY_INVESTOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getPendingReturnsUsers() throws Exception {
        UserDto user = new UserDto(
                9L,
                "Pending User",
                "pending@example.com",
                "Example Ltd",
                "Awaiting review",
                Role.BOARD,
                Status.PENDING
        );
        when(service.getPending()).thenReturn(List.of(user));

        mvc.perform(get("/api/v1/admin/get-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].email").value("pending@example.com"))
                .andExpect(jsonPath("$[0].description").value("Awaiting review"))
                .andExpect(jsonPath("$[0].role").value("BOARD"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void searchUserReturnsAccount() throws Exception {
        UserDto user = new UserDto(
                9L,
                "Active User",
                "active@example.com",
                "Example Ltd",
                null,
                Role.BOARD,
                Status.ACTIVE
        );
        when(service.searchUser(" Active@Example.com ")).thenReturn(user);

        mvc.perform(get("/api/v1/admin/search-user")
                        .param("email", " Active@Example.com "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.email").value("active@example.com"))
                .andExpect(jsonPath("$.role").value("BOARD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(service).searchUser(" Active@Example.com ");
    }

    @Test
    void verifyUserReturnsNoContent() throws Exception {
        mvc.perform(post("/api/v1/admin/verify-user/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isApproved": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(service).verifyUser(9L, true);
    }

    @Test
    void verifyUserPassesRejectDecision() throws Exception {
        mvc.perform(post("/api/v1/admin/verify-user/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isApproved": false
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(service).verifyUser(9L, false);
    }

    @Test
    void disableUserReturnsNoContent() throws Exception {
        mvc.perform(post("/api/v1/admin/disable-user/9"))
                .andExpect(status().isNoContent());

        verify(service).disableUser(9L);
    }
}
