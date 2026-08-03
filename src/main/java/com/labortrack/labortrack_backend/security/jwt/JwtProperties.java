package com.labortrack.labortrack_backend.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT secret is required, cannot be blank or null.")
        String secret,

        @NotNull(message = "JWT expiration is required, cannot be null.")
        Duration expiration
) {
}
