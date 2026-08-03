package com.labortrack.labortrack_backend.security.handler;

import com.labortrack.labortrack_backend.common.api.error.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * This class basically returns a JSON response when a user is trying
 * to access a protected API endpoint. Normally this error produce
 * 401 status code unauthorized when request does not have, an
 * invalid, or expired JWT.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;
    public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }


    /**
     * This method response with a 401 status code (unauthorized) using custom
     * dto ApiErrorResponse. It is used when user aims to access a protected
     * endpoint and does not have, is invalid or expired JWT.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication is required to access this resource.",
                request.getRequestURI(),
                Map.of()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        jsonMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}
