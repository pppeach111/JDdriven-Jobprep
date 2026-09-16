package com.careerpath.common.error;

import java.util.Map;

/**
 * 业务异常。由 GlobalExceptionHandler 转换为统一失败响应。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;
    private final boolean retryableOverride;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), Map.of(), errorCode.retryable());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of(), errorCode.retryable());
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : details;
        this.retryableOverride = retryable;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }

    public boolean retryable() {
        return retryableOverride;
    }
}