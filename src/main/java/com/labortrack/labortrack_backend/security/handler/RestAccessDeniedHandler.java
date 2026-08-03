package com.labortrack.labortrack_backend.security.handler;

import com.labortrack.labortrack_backend.common.api.error.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * This class returns a custom JSON DTO error response when
 * an authenticated user attempts to access a resource they
 * are not authorized to access. These authorization failures
 * produce a 403 forbidden response.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final String DEFAULT_ACCESS_DENIED_MESSAGE =
            "You do not have permission to access this resource.";

    private final JsonMapper jsonMapper;

    public RestAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * This method handles normal Spring Security authorization failures using
     * the standard generic access-denied message.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        writeForbiddenResponse(
                request,
                response,
                DEFAULT_ACCESS_DENIED_MESSAGE
        );
    }

    /**
     * Handles a known authorization failure using a specific,
     * application-controlled message.
     */
    public void handleWithMessage(
            HttpServletRequest request,
            HttpServletResponse response,
            String message)
            throws IOException {

        writeForbiddenResponse(
                request,
                response,
                message
        );
    }

    // HELPER METHODS
    /**
     * This method writes the standard DTO error response for
     * 403 Forbidden status.
     */
    private void writeForbiddenResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String message)
            throws IOException {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        jsonMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}