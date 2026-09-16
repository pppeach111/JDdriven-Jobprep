package com.careerpath.jd;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JD 结构化要求。见 14-contracts-and-schemas.md 第 4 节 requirements。
 * 硬门槛（HARD_GATE）由程序判定，模型输出不得直接升级为硬门槛。
 */
@Entity
@Table(name = "job_requirement")
@Getter
@Setter
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "source_jd_id")
    private UUID sourceJdId;

    /** HARD_GATE | CORE | PREFERRED | BONUS */
    @Column(name = "requirement_type", nullable = false, length = 30)
    private String requirementType;

    @Column(length = 100)
    private String field;

    @Column(name = "skill_id", length = 100)
    private String skillId;

    @Column(name = "required_level")
    private Integer requiredLevel;

    @Column(precision = 3, scale = 2)
    private BigDecimal importance;

    @Column(nullable = false)
    private boolean explicit = true;

    @Column(name = "source_quote", columnDefinition = "text")
    private String sourceQuote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_locator", columnDefinition = "jsonb")
    private Map<String, Object> sourceLocator;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

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