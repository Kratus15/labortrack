package com.labortrack.labortrack_backend.security.ratelimit;

import com.labortrack.labortrack_backend.common.api.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * This class-filter applies rate limiting to incoming API requests.
 * Every API request must first pass the global rate limit (greater
 * of all), while sensitive endpoints such as login, registration,
 * and password change also have their own stricter limits. Requests
 * that exceed a limit receive a 429 JSON response. Otherwise, keep
 * consuming from the request type a token out of the bucket until
 * no more tokens left or reset again.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitProperties properties,
            RateLimitService rateLimitService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    /**
     * This method skips rate limiting when it is disabled,
     * for CORS preflight OPTIONS requests, requests outside
     * the API, and importantly requests related to health check
     * endpoints.
     */
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        return !properties.enabled()
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/")
                || path.startsWith("/actuator/health");
    }

    /**
     * This method-filter applies the global API rate limit
     * first, then applies a stricter limit when the request
     * targets a sensitive endpoint. Also send a custom error
     * response if the user exceeds it requests per duration
     * time limit.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException
    {

        String clientIp = request.getRemoteAddr();

        /*
         every API request must pass the global rate limit (general limit).
         If you can't consume a token from the bucket that means you reach
         the limit, send an error response.
         */
        if (!rateLimitService.tryConsume(
                "global:" + clientIp,
                properties.global()
        )) {
            // throw custom error response
            writeTooManyRequests(response, request);
            return;
        }

        // apply additional rate limit if this is a sensitive endpoint
        if (!passesSensitiveLimit(request, clientIp)) {
            writeTooManyRequests(response, request);
            return;
        }

        // continue through the remaining filter chain
        filterChain.doFilter(request, response);
    }

    // HELPER METHODS

    /**
     * This method helps check whether the request targets
     * one of the sensitive endpoints and consumes from that
     * endpoint's separate rate-limit bucket.
     */
    private boolean passesSensitiveLimit(
            HttpServletRequest request,
            String clientIp
    ) {

        // sensitive limits currently only applies to POST requests
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // login request
        if (path.equals("/api/auth/login")) {
            return rateLimitService.tryConsume(
                    "login:" + clientIp,
                    properties.login()
            );
        }

        // register request
        if (path.equals("/api/companies/register")) {
            return rateLimitService.tryConsume(
                    "register:" + clientIp,
                    properties.register()
            );
        }

        // change-password request
        if (path.equals("/api/auth/change-password")) {
            return rateLimitService.tryConsume(
                    "change-password:" + clientIp,
                    properties.changePassword()
            );
        }

        return true;
    }

    /**
     * This method helps write a clean JSON response when
     * a user exceeds its requests per time limit.
     */
    private void writeTooManyRequests(
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException
    {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse errorResponse =
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too Many Requests",
                        "Too many requests. Please try again later.",
                        request.getRequestURI(),
                        Map.of()
                );

        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}
