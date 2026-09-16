package com.careerpath.common.api;

import java.util.List;

/**
 * 统一分页响应外层结构，见 14-contracts-and-schemas.md 第 3 节。
 * page 从 1 开始，pageSize 取值 1~100。
 */
public record PaginatedResponse<T>(
        String schemaVersion,
        List<T> data,
        Pagination pagination,
        String traceId) {

    public static final int MAX_PAGE_SIZE = 100;

    public record Pagination(int page, int pageSize, long total) {
    }

    public static <T> PaginatedResponse<T> of(List<T> data, int page, int pageSize, long total, String traceId) {
        return new PaginatedResponse<>(
                ApiResponse.SCHEMA_VERSION,
                data,
                new Pagination(page, pageSize, total),
                traceId);
    }
}