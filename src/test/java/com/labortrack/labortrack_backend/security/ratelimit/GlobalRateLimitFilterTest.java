package com.labortrack.labortrack_backend.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.rate-limit.enabled=true",
        "security.rate-limit.global.requests=2",
        "security.rate-limit.global.duration=1m",

        "security.rate-limit.login.requests=100",
        "security.rate-limit.login.duration=1m",

        "security.rate-limit.register.requests=100",
        "security.rate-limit.register.duration=1h",

        "security.rate-limit.change-password.requests=100",
        "security.rate-limit.change-password.duration=15m"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * This method-test verifies that normal API requests from
     * the same client IP are rejected with HTTP 429 after the
     * configured global API rate limit has been reached.
     */
    @Test
    void repeatedApiRequestsReturnTooManyRequestsFromGlobalLimit()
            throws Exception
    {

        /*
        for this special test we change global rate limit
        request-per-min. The global request limit is 2r X 1m.
        Using same IP for request, first and second requests
        must not receive a 429 status code back but the third
        must receive the 429 status back because it exceeds
        the rate limit request per time frame.
         */

        /*
         send first request through API using given IP and expect
         Unauthorized back because haven't authenticated.
         */
        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.50");
                                    return request;
                                })
                )
                .andExpect(status().isUnauthorized());

        /*
         send second request through API using given IP and expect
         Unauthorized back because haven't authenticated
         */
        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.50");
                                    return request;
                                })
                )
                .andExpect(status().isUnauthorized());

        /*
        send third request through API using given IP and
        this time expect 429 status back because it exceeds
        the request per time frame limit.
         */
        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .with(request -> {
                                    request.setRemoteAddr("10.0.0.50");
                                    return request;
                                })
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
                        "/api/admin/dashboard"
                ))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }
}
