package com.careerpath.common.api;

/**
 * 统一成功响应外层结构，见 14-contracts-and-schemas.md 第 3 节。
 */
public record ApiResponse<T>(String schemaVersion, T data, String traceId) {

    public static final String SCHEMA_VERSION = "1.0";

    public static <T> ApiResponse<T> of(T data, String traceId) {
        return new ApiResponse<>(SCHEMA_VERSION, data, traceId);
    }
}