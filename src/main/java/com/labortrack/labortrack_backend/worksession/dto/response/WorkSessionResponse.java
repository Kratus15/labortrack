package com.labortrack.labortrack_backend.worksession.dto.response;

import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;

import java.time.OffsetDateTime;

public record WorkSessionResponse(
        Long workSessionId,
        Long companyId,
        Long employeeId,
        OffsetDateTime clockInTime,
        OffsetDateTime clockOutTime,
        WorkSessionStatus status,
        String note,
        Long workedMinutes
) {
}