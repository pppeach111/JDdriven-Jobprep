package com.careerpath.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 错误码枚举与业务异常契约测试，覆盖统一错误响应的输入侧。
 */
@DisplayName("ErrorCode 与 BusinessException 契约")
class BusinessExceptionTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("每个错误码都必须提供非空默认消息与 HTTP 状态")
    void everyErrorCodeIsWellFormed(ErrorCode code) {
        assertNotNull(code.defaultMessage(), code + " 缺少默认消息");
        assertFalse(code.defaultMessage().isBlank(), code + " 默认消息为空白");
        assertNotNull(code.httpStatus(), code + " 缺少 HTTP 状态");
    }

    @Test
    @DisplayName("错误码到 HTTP 状态与可重试性的映射稳定")
    void errorCodeMappingIsStable() {
        assertMapping(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, false);
        assertMapping(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, false);
        assertMapping(ErrorCode.CONFLICT, HttpStatus.CONFLICT, true);
        assertMapping(ErrorCode.UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE, false);
        assertMapping(ErrorCode.PAYLOAD_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE, false);
        assertMapping(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, true);
        assertMapping(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, false);
    }

    private static void assertMapping(ErrorCode code, HttpStatus status, boolean retryable) {
        assertEquals(status, code.httpStatus(), code + " HTTP 状态不符合契约");
        assertEquals(retryable, code.retryable(), code + " 可重试标志不符合契约");
    }

    @Test
    @DisplayName("单参数构造使用错误码默认消息与默认可重试性")
    void singleArgumentConstructorUsesDefaults() {
        BusinessException ex = new BusinessException(ErrorCode.CONFLICT);

        assertEquals(ErrorCode.CONFLICT.defaultMessage(), ex.getMessage());
        assertSame(ErrorCode.CONFLICT, ex.errorCode());
        assertTrue(ex.details().isEmpty());
        assertTrue(ex.retryable());
    }

    @Test
    @DisplayName("指定消息构造时保留自定义消息，details 为空 Map")
    void messageConstructorKeepsCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND, "求职目标不存在");

        assertEquals("求职目标不存在", ex.getMessage());
        assertSame(ErrorCode.NOT_FOUND, ex.errorCode());
        assertTrue(ex.details().isEmpty());
        assertFalse(ex.retryable());
    }

    @Test
    @DisplayName("details 为 null 时降级为空 Map，避免调用方 NPE")
    void nullDetailsDegradesToEmptyMap() {
        BusinessException ex = new BusinessException(ErrorCode.VALIDATION_ERROR, "x", null, true);

        assertNotNull(ex.details());
        assertTrue(ex.details().isEmpty());
        assertTrue(ex.retryable());
    }

    @Test
    @DisplayName("显式 retryable 覆盖错误码默认值")
    void retryableOverrideWins() {
        BusinessException forced = new BusinessException(ErrorCode.NOT_FOUND, "x", Map.of("k", "v"), true);

        assertTrue(forced.retryable());
        assertEquals(Map.of("k", "v"), forced.details());
    }

    @Test
    @DisplayName("BusinessException 是受检异常的运行时替代，可被统一处理器捕获")
    void isRuntimeException() {
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR);

        assertTrue(ex instanceof RuntimeException);
    }
}