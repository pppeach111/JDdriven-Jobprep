package com.careerpath.jd.dto;

import java.util.List;
import java.util.Map;

/**
 * JD 解析结果，结构严格对齐 14-contracts-and-schemas.md 第 4 节。
 * 推断字段必须 explicit=false，无法确定的字段返回 UNKNOWN（放入 unknowns）。
 */
public record JdParseResult(
        String schemaVersion,
        String sourceId,
        JobFamilyView jobFamily,
        List<RequirementView> requirements,
        List<String> unknowns,
        List<String> warnings
) {

    public static final String SCHEMA_VERSION = "1.0";

    public record JobFamilyView(String value, Double confidence) {
    }

    public record RequirementView(
            String id,
            String type,
            String field,
            Map<String, Object> value,
            String skillId,
            Integer requiredLevel,
            Double importance,
            boolean explicit,
            String sourceQuote,
            Map<String, Object> sourceLocator,
            Double confidence
    ) {
    }
}