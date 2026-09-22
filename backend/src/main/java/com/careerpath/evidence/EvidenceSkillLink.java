package com.careerpath.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 证据与技能的关联（方向 + 关联强度 + 声称等级）。
 * 表结构见 V1__init_schema.sql 与 V4__evidence_claimed_level.sql；
 * 同一证据对同一技能唯一（uq_evidence_skill）。
 */
@Entity
@Table(name = "evidence_skill_link")
@Getter
@Setter
public class EvidenceSkillLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    @Column(name = "skill_id", nullable = false, length = 100)
    private String skillId;

    /** 见 EvidenceDirection 枚举 */
    @Column(nullable = false, length = 20)
    private String direction;

    @Column(precision = 3, scale = 2)
    private BigDecimal strength;

    /** 用户声称该证据支撑的等级 0..5；SUPPORTS 必填，WEAKENS/NEUTRAL 可为 null。 */
    @Column(name = "claimed_level")
    private Integer claimedLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}