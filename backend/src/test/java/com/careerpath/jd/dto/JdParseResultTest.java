package com.careerpath.jd.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JD 解析结果契约测试。
 *
 * <p>字段名与结构必须严格对齐 14-contracts-and-schemas.md 第 4 节，
 * 任何改名都会破坏前后端契约，因此用反射对组件名做回归断言。
 */
@DisplayName("JdParseResult 契约 Schema")
class JdParseResultTest {

    @Test
    @DisplayName("schemaVersion 固定为 1.0")
    void schemaVersionIsPinned() {
        assertEquals("1.0", JdParseResult.SCHEMA_VERSION);
    }

    @Test
    @DisplayName("外层字段名与顺序对齐契约文档第 4 节")
    void outerComponentNamesMatchContract() {
        List<String> names = Arrays.stream(JdParseResult.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of(
                "schemaVersion", "sourceId", "jobFamily",
                "requirements", "unknowns", "warnings"), names);
    }

    @Test
    @DisplayName("RequirementView 字段名与顺序对齐契约文档第 4 节 requirements")
    void requirementComponentNamesMatchContract() {
        List<String> names = Arrays.stream(JdParseResult.RequirementView.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of(
                "id", "type", "field", "value", "skillId", "requiredLevel",
                "importance", "explicit", "sourceQuote", "sourceLocator", "confidence"), names);
    }

    @Test
    @DisplayName("JobFamilyView 只有 value 与 confidence 两个字段")
    void jobFamilyComponentNamesMatchContract() {
        List<String> names = Arrays.stream(JdParseResult.JobFamilyView.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("value", "confidence"), names);
    }

    @Test
    @DisplayName("访问器原样返回构造传入的值")
    void accessorsReturnConstructedValues() {
        JdParseResult.RequirementView requirement = new JdParseResult.RequirementView(
                "req_001", "HARD_GATE", "graduationYear",
                Map.of("min", 2027, "max", 2028),
                null, null, 1.0, true,
                "面向2027-2028届本科生",
                Map.of("charStart", 2, "charEnd", 12), 0.95);
        JdParseResult result = new JdParseResult(
                JdParseResult.SCHEMA_VERSION, "jd_001",
                new JdParseResult.JobFamilyView("JAVA_BACKEND", 0.94),
                List.of(requirement), List.of("internshipDuration"), List.of());

        assertEquals("1.0", result.schemaVersion());
        assertEquals("jd_001", result.sourceId());
        assertEquals("JAVA_BACKEND", result.jobFamily().value());
        assertEquals(0.94, result.jobFamily().confidence(), 0.0);
        assertEquals(List.of(requirement), result.requirements());
        assertEquals(List.of("internshipDuration"), result.unknowns());
        assertTrue(result.warnings().isEmpty());

        JdParseResult.RequirementView view = result.requirements().get(0);
        assertEquals("req_001", view.id());
        assertEquals("HARD_GATE", view.type());
        assertEquals("graduationYear", view.field());
        assertEquals(Map.of("min", 2027, "max", 2028), view.value());
        assertNull(view.skillId());
        assertNull(view.requiredLevel());
        assertEquals(1.0, view.importance(), 0.0);
        assertTrue(view.explicit());
        assertEquals("面向2027-2028届本科生", view.sourceQuote());
        assertEquals(2, view.sourceLocator().get("charStart"));
        assertEquals(0.95, view.confidence(), 0.0);
    }

    @Test
    @DisplayName("契约允许 explicit=false 表达推断内容，且不写入 HARD_GATE")
    void inferredRequirementCanBeMarkedNonExplicit() {
        JdParseResult.RequirementView inferred = new JdParseResult.RequirementView(
                "req_009", "CORE", "skill", null, "redis-cache-design", 3,
                0.6, false, "推测可能涉及缓存", Map.of("charStart", 0, "charEnd", 3), 0.55);

        assertFalse(inferred.explicit());
        assertEquals("CORE", inferred.type());
        assertNotEquals("HARD_GATE", inferred.type(), "推断内容不得升级为硬门槛");
    }

    @Test
    @DisplayName("未知字段用 null 或 UNKNOWN 表达，而非空字符串")
    void unknownFieldsUseNullInsteadOfEmptyString() {
        JdParseResult.RequirementView unknown = new JdParseResult.RequirementView(
                "req_010", "CORE", "skill", null, null, null,
                null, true, null, Map.of(), null);

        assertNull(unknown.skillId());
        assertNull(unknown.value());
        assertNull(unknown.requiredLevel());
    }

    @Test
    @DisplayName("record 值语义：相同内容相等，不同内容不等")
    void recordValueSemantics() {
        JdParseResult.JobFamilyView a = new JdParseResult.JobFamilyView("JAVA_BACKEND", 0.5);
        JdParseResult.JobFamilyView b = new JdParseResult.JobFamilyView("JAVA_BACKEND", 0.5);
        JdParseResult.JobFamilyView c = new JdParseResult.JobFamilyView("FRONTEND", 0.5);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("unknowns 与 warnings 允许为空列表，但不应为 null")
    void emptyListsAreDistinctFromNull() {
        JdParseResult result = new JdParseResult(
                "1.0", "jd_002", new JdParseResult.JobFamilyView("JAVA_BACKEND", 0.5),
                List.of(), List.of(), List.of());

        assertTrue(result.unknowns().isEmpty());
        assertTrue(result.warnings().isEmpty());
        assertTrue(result.requirements().isEmpty());
    }
}