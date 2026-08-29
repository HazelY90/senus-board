package com.hazely.senusboard.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAccessDeniedHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(mapper);

    @Test
    void handleReturnsForbiddenJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/get-pending");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Denied"));

        JsonNode body = mapper.readTree(response.getContentAsByteArray());
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(HttpStatus.FORBIDDEN.value(), body.get("status").asInt());
        assertEquals("Forbidden", body.get("error").asText());
        assertEquals("Access denied", body.get("message").asText());
        assertEquals("/api/v1/admin/get-pending", body.get("path").asText());
    }
}
