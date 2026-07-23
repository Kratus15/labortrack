package com.labortrack.labortrack_backend.company.controller;

import com.labortrack.labortrack_backend.company.dto.request.CompanyRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * This test checks if company registration works correctly through the API.
     * It sends valid company and admin information to the register endpoint,
     * then checks that the response returns status 201 and contains the expected
     * company and admin user information. It also checks that password information
     * is not included in the response.
     */
    @Test
    void registerCompanyReturnsCreatedCompanyAndAdminUser() throws Exception {
        String email = "controller-admin-" + UUID.randomUUID() + "@labortrack.test";

        CompanyRegistrationRequest request = new CompanyRegistrationRequest(
                "Controller Test Construction",
                email,
                "StrongPassword123!"
        );

        mockMvc.perform(post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.companyId").isNumber())
                .andExpect(jsonPath("$.companyName")
                        .value("Controller Test Construction"))
                .andExpect(jsonPath("$.adminUserId").isNumber())
                .andExpect(jsonPath("$.adminEmail").value(email))
                .andExpect(jsonPath("$.timezone")
                        .value("America/New_York"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /**
     * This test checks what happens when invalid company registration data
     * is sent to the API. It sends a blank company name, invalid email,
     * and short password. Then it checks that the API returns status 400
     * with the standard error response and validation messages for each field.
     */
    @Test
    void registerCompanyReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest(
                "",
                "not-an-email",
                "123"
        );

        mockMvc.perform(post("/api/companies/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.path")
                        .value("/api/companies/register"))
                .andExpect(jsonPath("$.validationErrors.companyName")
                        .value("Company name is required, cannot be blank or null."))
                .andExpect(jsonPath("$.validationErrors.adminEmail")
                        .value("Admin email must be valid."))
                .andExpect(jsonPath("$.validationErrors.adminPassword")
                        .value("Admin password is required and must be between 8 and 72 characters."));
    }
}
