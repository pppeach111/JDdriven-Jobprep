package com.careerpath.skill;

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
 * 用户在某目标下的技能估计。
 * 等级与置信度由程序计算写入，模型不直接写入本表。
 * 见 14-contracts-and-schemas.md 第 5 节。
 */
@Entity
@Table(name = "user_skill_estimate")
@Getter
@Setter
public class UserSkillEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "skill_id", nullable = false, length = 100)
    private String skillId;

    @Column(name = "estimated_level", precision = 3, scale = 1)
    private BigDecimal estimatedLevel;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    /** 见 GapType 枚举 */
    @Column(name = "gap_type", length = 30)
    private String gapType;

    /** 维度分：concept/codeReading/diagnosis/design/expression/practice */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> dimensions;

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