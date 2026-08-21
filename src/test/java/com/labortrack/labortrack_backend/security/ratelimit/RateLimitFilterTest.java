package com.labortrack.labortrack_backend.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.rate-limit.enabled=true",
        "security.rate-limit.global.requests=100",
        "security.rate-limit.global.duration=1m",
        "security.rate-limit.login.requests=2",
        "security.rate-limit.login.duration=1m",
        "security.rate-limit.register.requests=2",
        "security.rate-limit.register.duration=1h",
        "security.rate-limit.change-password.requests=2",
        "security.rate-limit.change-password.duration=15m"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * This method-test verifies that repeated login requests
     * from the same client IP are rejected with HTTP 429 after
     * the configured login limit has been reached.
     */
    @Test
    void repeatedLoginRequestsReturnTooManyRequests() throws Exception {

        String loginJson = """
                {
                  "email": "does-not-exist@labortrack.test",
                  "password": "WrongPassword123$"
                }
                """;

        /*
         Remember in test setting /login endpoint limit
         is 2 request-per-min [2rX1m]. Also, we are using same
         fake IP '10.0.0.10'.
         */

        /*
        send first login request to /login endpoint, the login request is false
        so expect an unauthorized status back. Using an artificial ip for all
        requests.
         */
        mockMvc.perform(
                        post("/api/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.10");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isUnauthorized());

        // send second request
        mockMvc.perform(
                        post("/api/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.10");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isUnauthorized());

        /*
        sending third request. This exceeds the configured
        login limit because /login endpoint was 2 requests
        per min.
         */
        mockMvc.perform(
                        post("/api/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.10");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(
                        "Too many requests. Please try again later."
                ))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    /**
     * This method-test verifies that repeated company registration
     * requests from the same client IP are rejected with HTTP 429
     * after the configured registration rate limit has been reached.
     */
    @Test
    void repeatedCompanyRegistrationRequestsReturnTooManyRequests() throws Exception {

        String adminEmail =
                "rate-limit-register-"
                        + UUID.randomUUID()
                        + "@labortrack.test";

        String registrationJson = """
            {
              "companyName": "Rate Limit Company",
              "adminEmail": "%s",
              "adminPassword": "StrongPassword123$"
            }
            """.formatted(adminEmail);

        /*
         Remember in test setting /register endpoint limit
         is 2 request-per-hr [2rX1h]. Also, we are using same
         fake IP '10.0.0.20'.
         */

        /*
        first send a registration request through API /register endpoint,
        and expect created status back. This request was success because
        it is a brand-new company (admin user).
         */
        mockMvc.perform(
                        post("/api/companies/register")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.20");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationJson)
                )
                .andExpect(status().isCreated());

        /*
         send the same registration request, expect this time to fail
         because obviously same email conflict. Request was allowed
         because it hasn't reached limit.
         */
        mockMvc.perform(
                        post("/api/companies/register")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.20");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationJson)
                )
                .andExpect(status().isConflict());

        /*
         send the same registration request, expect this time to fail
         because now it reaches its limit and status 429:too-many-requests
         was returned because exceeded the number of request-per-hour made
         to /register endpoint.
         */
        mockMvc.perform(
                        post("/api/companies/register")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.20");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationJson)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(
                        "Too many requests. Please try again later."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/companies/register"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    /**
     * This method-test verifies that CORS preflight OPTIONS requests
     * are skipped by the rate limiter and continue returning a
     * successful response even when repeated multiple times.
     */
    @Test
    void optionsRequestIsNotRateLimited() throws Exception {

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            options("/api/auth/login")
                                    .with(request -> {
                                        request.setRemoteAddr("10.0.0.30");
                                        return request;
                                    })
                                    .header("Origin", "http://localhost:5173")
                                    .header(
                                            "Access-Control-Request-Method",
                                            "POST"
                                    )
                    )
                    .andExpect(status().isOk());
        }
    }

    /**
     * This method-test verifies that repeated authenticated
     * change-password requests from the same client IP are
     * rejected with HTTP 429 after reaching the configured
     * change-password limit.
     */
    @Test
    void repeatedChangePasswordRequestsReturnTooManyRequests()
            throws Exception {

        String adminEmail =
                "rate-limit-password-"
                        + UUID.randomUUID()
                        + "@labortrack.test";
        String adminPassword = "CurrentPassword123$";

        String registrationJson = """
            {
              "companyName": "Password Rate Limit Company",
              "adminEmail": "%s",
              "adminPassword": "%s"
            }
            """.formatted(adminEmail, adminPassword);

        // send the registration request through API and expect created status back
        mockMvc.perform(
                        post("/api/companies/register")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.41");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationJson)
                )
                .andExpect(status().isCreated());

        String loginJson = """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(adminEmail, adminPassword);

        // send admin-login request through API and expect OK status back (different IP) and save results
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.42");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );

        // get the admin-access-token from results
        String accessToken =
                loginResponse.get("accessToken").asString();

    /*
     use an incorrect current password intentionally.
     The first two requests should pass the rate limiter
     and reach normal password-change validation.
     */
        String changePasswordJson = """
            {
              "currentPassword": "WrongCurrentPassword123$",
              "newPassword": "DifferentPassword123$"
            }
            """;

        // send first request should pass limiter
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.40");
                                    return request;
                                })
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changePasswordJson)
                )
                .andExpect(status().isBadRequest());

        // second request also passes limiter
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.40");
                                    return request;
                                })
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changePasswordJson)
                )
                .andExpect(status().isBadRequest());

        // third request exceeds the test-only limit.
        mockMvc.perform(
                        post("/api/auth/change-password")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.40");
                                    return request;
                                })
                                .header(
                                        AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changePasswordJson)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value(
                        "Too Many Requests"
                ))
                .andExpect(jsonPath("$.message").value(
                        "Too many requests. Please try again later."
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/auth/change-password"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

}

