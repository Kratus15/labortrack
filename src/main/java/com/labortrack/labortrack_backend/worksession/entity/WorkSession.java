package com.labortrack.labortrack_backend.worksession.entity;

import com.labortrack.labortrack_backend.worksession.enums.WorkSessionStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "work_sessions")
public class WorkSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "clock_in_time", nullable = false)
    private OffsetDateTime clockInTime;

    @Column(name = "clock_out_time")
    private OffsetDateTime clockOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkSessionStatus status = WorkSessionStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // HELPER METHODS
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
