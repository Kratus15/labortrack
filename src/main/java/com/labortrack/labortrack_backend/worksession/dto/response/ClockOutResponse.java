package com.labortrack.labortrack_backend.worksession.dto.response;

import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;

import java.time.OffsetDateTime;

public record ClockOutResponse(
        Long workSessionId,
        Long employeeId,
        OffsetDateTime clockInTime,
        OffsetDateTime clockOutTime,
        WorkSessionStatus status,
        Long workedMinutes,
        String message
) {
}
