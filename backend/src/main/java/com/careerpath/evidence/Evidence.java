package com.careerpath.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 一条能力证据。表结构见 V1__init_schema.sql（无 updated_at 列）。
 * 证据归属用户（user_id），不归属目标；在目标流程中登记，但可跨目标复用。
 * 等级与置信度由程序聚合写入 user_skill_estimate，本表只保存证据本体。
 * 见 14-contracts-and-schemas.md 第 11 节。
 */
@Entity
@Table(name = "evidence")
@Getter
@Setter
public class Evidence {

    /** 表单登记来源标记，用于与后续测评/简历解析来源区分。 */
    public static final String SOURCE_USER_FORM = "USER_FORM";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 见 EvidenceType 枚举 */
    @Column(name = "evidence_type", nullable = false, length = 30)
    private String evidenceType;

    /** 人类可读标题（V4 迁移新增；表单登记必填）。 */
    @Column(length = 200)
    private String title;

    @Column(length = 100)
    private String source;

    @Column(name = "content_summary", columnDefinition = "text")
    private String contentSummary;

    @Column(precision = 3, scale = 2)
    private BigDecimal credibility;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}