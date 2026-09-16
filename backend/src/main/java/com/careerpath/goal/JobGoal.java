package com.careerpath.goal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 求职目标。见 02-system-architecture.md 核心领域对象 JobGoal。
 */
@Entity
@Table(name = "job_goal")
@Getter
@Setter
public class JobGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "job_family", nullable = false, length = 60)
    private String jobFamily = "JAVA_BACKEND";

    @Column(length = 100)
    private String city;

    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "weekly_hours")
    private Integer weeklyHours;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}