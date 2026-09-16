package com.careerpath.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一失败响应契约测试，对齐 14-contracts-and-schemas.md 第 3 节失败响应结构。
 */
@DisplayName("ErrorResponse 失败响应契约")
class ErrorResponseTest {

    @Test
    @DisplayName("外层字段名为 schemaVersion/error/traceId")
    void outerComponentNamesMatchContract() {
        List<String> names = Arrays.stream(ErrorResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("schemaVersion", "error", "traceId"), names);
    }

    @Test
    @DisplayName("error 对象字段名为 code/message/details/retryable")
    void errorBodyComponentNamesMatchContract() {
        List<String> names = Arrays.stream(ErrorResponse.ErrorBody.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("code", "message", "details", "retryable"), names);
    }

    @Test
    @DisplayName("of 补齐 schemaVersion 并保留错误码、消息与可重试标志")
    void ofBuildsContractShape() {
        ErrorResponse response = ErrorResponse.of("VALIDATION_ERROR", "请求内容不符合要求",
                Map.of("rawText", "JD 内容不能为空"), false, "trace-003");

        assertEquals("1.0", response.schemaVersion());
        assertEquals("trace-003", response.traceId());
        assertEquals("VALIDATION_ERROR", response.error().code());
        assertEquals("请求内容不符合要求", response.error().message());
        assertEquals(Map.of("rawText", "JD 内容不能为空"), response.error().details());
        assertFalse(response.error().retryable());
    }

    @Test
    @DisplayName("details 为 null 时降级为空 Map，保证响应体字段存在")
    void nullDetailsDegradeToEmptyMap() {
        ErrorResponse response = ErrorResponse.of("INTERNAL_ERROR", "服务器内部错误", null, false, "trace-004");

        assertNotNull(response.error().details());
        assertTrue(response.error().details().isEmpty());
    }

    @Test
    @DisplayName("retryable 原样透传，供前端决定是否重试")
    void retryableIsPassedThrough() {
        assertTrue(ErrorResponse.of("EXTERNAL_SERVICE_UNAVAILABLE", "外部服务暂时不可用", Map.of(), true, "t")
                .error().retryable());
        assertFalse(ErrorResponse.of("NOT_FOUND", "资源不存在", Map.of(), false, "t")
                .error().retryable());
    }
}