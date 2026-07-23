package com.labortrack.labortrack_backend.company.dto.response;

import java.time.OffsetDateTime;

public record CompanyRegistrationResponse(
        Long companyId,
        String companyName,
        Long adminUserId,
        String adminEmail,
        String timezone,
        boolean active,
        OffsetDateTime createdAt
        ) {
}
