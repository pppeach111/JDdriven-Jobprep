package com.careerpath.common.api;

import java.util.Map;

/**
 * 统一失败响应外层结构，见 14-contracts-and-schemas.md 第 3 节。
 * message 不得泄露密钥、堆栈、网页原始隐私或内部提示词。
 */
public record ErrorResponse(String schemaVersion, ErrorBody error, String traceId) {

    public record ErrorBody(String code, String message, Map<String, Object> details, boolean retryable) {
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> details,
                                   boolean retryable, String traceId) {
        return new ErrorResponse(
                ApiResponse.SCHEMA_VERSION,
                new ErrorBody(code, message, details == null ? Map.of() : details, retryable),
                traceId);
    }
}