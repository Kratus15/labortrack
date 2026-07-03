package com.labortrack.labortrack_backend.company.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name="companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, nullable = false, columnDefinition = "citext")
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name="created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private OffsetDateTime  updatedAt;


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
