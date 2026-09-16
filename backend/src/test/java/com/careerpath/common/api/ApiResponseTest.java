package com.careerpath.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一成功响应外层契约测试，对齐 14-contracts-and-schemas.md 第 3 节。
 */
@DisplayName("ApiResponse 成功响应契约")
class ApiResponseTest {

    @Test
    @DisplayName("schemaVersion 固定为 1.0 且与失败响应共用同一版本")
    void schemaVersionIsPinned() {
        assertEquals("1.0", ApiResponse.SCHEMA_VERSION);
        assertEquals(ApiResponse.SCHEMA_VERSION,
                ErrorResponse.of("X", "m", null, false, "t").schemaVersion());
    }

    @Test
    @DisplayName("字段名与顺序对齐契约：schemaVersion/data/traceId")
    void componentNamesMatchContract() {
        List<String> names = Arrays.stream(ApiResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("schemaVersion", "data", "traceId"), names);
    }

    @Test
    @DisplayName("of 包装业务数据并带上 schemaVersion 与 traceId")
    void ofWrapsDataWithMetadata() {
        ApiResponse<String> response = ApiResponse.of("payload", "trace-001");

        assertEquals("1.0", response.schemaVersion());
        assertEquals("payload", response.data());
        assertEquals("trace-001", response.traceId());
    }

    @Test
    @DisplayName("data 允许为 null（例如空结果），外层结构仍然完整")
    void ofToleratesNullData() {
        ApiResponse<String> response = ApiResponse.<String>of(null, "trace-002");

        assertNull(response.data());
        assertEquals("1.0", response.schemaVersion());
        assertEquals("trace-002", response.traceId());
    }
}