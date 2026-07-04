package com.labortrack.labortrack_backend.company.entity;

import com.labortrack.labortrack_backend.employee.entity.Employee;
import com.labortrack.labortrack_backend.user.entity.User;
import com.labortrack.labortrack_backend.worksession.entity.WorkSession;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
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

    // One company can have many users. A user belongs to one company
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    // One company can have many employees. Employee(s) belongs to one company.
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    // One company can have many workSessions. A workSession belongs to one company.
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<WorkSession> workSessions = new ArrayList<>();

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
