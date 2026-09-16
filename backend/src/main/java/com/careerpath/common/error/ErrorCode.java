package com.careerpath.common.error;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码。新增错误码必须同步 14-contracts-and-schemas.md 和前端。
 */
public enum ErrorCode {

    VALIDATION_ERROR("请求内容不符合要求", HttpStatus.BAD_REQUEST, false),
    NOT_FOUND("资源不存在", HttpStatus.NOT_FOUND, false),
    CONFLICT("资源已被更新，请刷新后重试", HttpStatus.CONFLICT, true),
    UNSUPPORTED_MEDIA_TYPE("不支持的文件类型", HttpStatus.UNSUPPORTED_MEDIA_TYPE, false),
    PAYLOAD_TOO_LARGE("文件或内容超过允许大小", HttpStatus.PAYLOAD_TOO_LARGE, false),
    EXTERNAL_SERVICE_UNAVAILABLE("外部服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE, true),
    INTERNAL_ERROR("服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR, false);

    private final String defaultMessage;
    private final HttpStatus httpStatus;
    private final boolean retryable;

    ErrorCode(String defaultMessage, HttpStatus httpStatus, boolean retryable) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public boolean retryable() {
        return retryable;
    }
}