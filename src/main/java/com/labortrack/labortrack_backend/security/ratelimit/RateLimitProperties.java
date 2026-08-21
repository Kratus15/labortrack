package com.labortrack.labortrack_backend.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * This DTO holds the configurable rate-limit settings
 * /structure loaded from application properties using
 * the "security.rate-limit" prefix. Each limit defines
 * how many requests are allowed within a specific
 */
@Validated
@ConfigurationProperties(prefix = "security.rate-limit")
public record RateLimitProperties(
        boolean enabled,

        @Valid
        Limit global,

        @Valid
        Limit register,

        @Valid
        Limit login,

        @Valid
        Limit changePassword
) {

    /**
     * This nested DTO represents the configuration for
     * one rate limit, including the maximum number or
     * requests allowed and the duration of the rate-limit
     * window.
     */
    public record Limit(
            @Min(value = 1, message = "Rate-limit requests must be at least 1.")
            int requests,

            @NotNull(message = "Rate-limit duration is required.")
            Duration duration
    ) {

    }
}
