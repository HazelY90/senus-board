package com.hazely.senusboard.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAuthenticationEntryPointTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(mapper);

    @Test
    void commenceReturnsUnauthorizedJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Invalid"));

        JsonNode body = mapper.readTree(response.getContentAsByteArray());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status").asInt());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("Authentication is required", body.get("message").asText());
        assertEquals("/api/v1/auth/me", body.get("path").asText());
    }
}
