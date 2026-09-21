package com.careerpath.skill.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 个人能力图谱，结构严格对齐 14-contracts-and-schemas.md 第 5 节。
 * 前端必须同时展示等级与置信度，不能只展示单一分数。
 */
public record CapabilityMap(
        String schemaVersion,
        String goalId,
        List<SkillCard> skills
) {

    public static final String SCHEMA_VERSION = "1.0";

    public record SkillCard(
            String skillId,
            String name,
            Integer requiredLevel,
            BigDecimal estimatedLevel,
            BigDecimal confidence,
            String gapType,
            Map<String, Object> dimensions,
            List<String> evidenceIds,
            List<String> sourceQuotes,
            NextAction nextAction,
            Instant updatedAt
    ) {
    }

    /**
     * 下一动作，结构对齐 14-contracts-and-schemas.md 第 5 节。
     * taskId 在学习任务模块落地前为 null，表示"尚未生成可执行任务"，不得用占位 ID 冒充。
     */
    public record NextAction(String type, String taskId, String reason) {
    }
}