package com.careerpath.common.error;

import com.careerpath.common.api.ErrorResponse;
import com.careerpath.common.web.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一错误路径测试。
 *
 * <p>所有异常必须转换为 14-contracts-and-schemas.md 第 3 节规定的失败响应，
 * 且用户可见 message 不得泄露内部细节。
 */
@DisplayName("GlobalExceptionHandler 统一错误路径")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ---------------------------------------------------------------- 业务异常

    @Test
    @DisplayName("业务异常按 ErrorCode 映射状态码并保持自定义 message")
    void businessExceptionMapsToErrorCode() {
        ResponseEntity<ErrorResponse> resp =
                handler.handleBusiness(new BusinessException(ErrorCode.VALIDATION_ERROR, "目标 JD 内容不能为空"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("1.0", body.schemaVersion());
        assertEquals("VALIDATION_ERROR", body.error().code());
        assertEquals("目标 JD 内容不能为空", body.error().message());
        assertFalse(body.error().retryable());
        assertTrue(body.error().details().isEmpty());
    }

    @Test
    @DisplayName("NOT_FOUND 映射 404，且不可重试")
    void notFoundMapsTo404() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("NOT_FOUND", resp.getBody().error().code());
        assertEquals(ErrorCode.NOT_FOUND.defaultMessage(), resp.getBody().error().message());
        assertFalse(resp.getBody().error().retryable());
    }

    @Test
    @DisplayName("CONFLICT 映射 409，标记可重试")
    void conflictMapsTo409AndIsRetryable() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(new BusinessException(ErrorCode.CONFLICT));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertTrue(resp.getBody().error().retryable());
    }

    @Test
    @DisplayName("业务异常携带的 details 原样透传给前端")
    void businessExceptionDetailsArePropagated() {
        Map<String, Object> details = Map.of("field", "rawText", "reason", "too long");
        BusinessException ex = new BusinessException(ErrorCode.VALIDATION_ERROR, "内容过长", details, false);

        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(ex);

        assertEquals(details, resp.getBody().error().details());
    }

    @Test
    @DisplayName("未设置 traceId 时回退为 unknown，响应仍非空")
    void traceIdFallsBackToUnknown() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(new BusinessException(ErrorCode.INTERNAL_ERROR));

        assertEquals("unknown", resp.getBody().traceId());
    }

    @Test
    @DisplayName("已设置 MDC 时 traceId 回写到失败响应，保证可追溯")
    void traceIdIsPropagatedFromMdc() {
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-abc-123");

        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND));

        assertEquals("trace-abc-123", resp.getBody().traceId());
    }

    // ---------------------------------------------------------------- 校验异常

    @Test
    @DisplayName("参数校验失败映射 400，字段错误逐项进入 details")
    void validationFailureIsMappedWithFieldDetails() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "importJdRequest");
        binding.addError(new FieldError("importJdRequest", "rawText", "JD 内容不能为空"));
        binding.addError(new FieldError("importJdRequest", "sourceUrl", "来源地址过长"));
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        Mockito.when(ex.getBindingResult()).thenReturn(binding);

        ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertEquals("VALIDATION_ERROR", body.error().code());
        assertEquals(ErrorCode.VALIDATION_ERROR.defaultMessage(), body.error().message());
        assertFalse(body.error().retryable());
        assertEquals("JD 内容不能为空", body.error().details().get("rawText"));
        assertEquals("来源地址过长", body.error().details().get("sourceUrl"));
    }

    @Test
    @DisplayName("请求体不可读映射 400，且不暴露解析细节")
    void malformedBodyIsMappedTo400() {
        ResponseEntity<ErrorResponse> resp = handler.handleUnreadable(
                new HttpMessageNotReadableException("JSON parse error at line 3"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertEquals("VALIDATION_ERROR", body.error().code());
        assertEquals("请求体格式错误", body.error().message());
        assertFalse(body.error().message().contains("JSON parse error"), "不得泄露内部解析细节");
        assertTrue(body.error().details().isEmpty());
    }

    // ---------------------------------------------------------------- 兜底异常

    @Test
    @DisplayName("未知异常映射 500，message 使用通用文案，不泄露异常细节")
    void unexpectedExceptionIsMappedTo500WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> resp = handler.handleUnexpected(
                new IllegalStateException("jdbc connection refused to 10.0.0.9:5432"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertEquals("INTERNAL_ERROR", body.error().code());
        assertEquals(ErrorCode.INTERNAL_ERROR.defaultMessage(), body.error().message());
        assertFalse(body.error().message().contains("10.0.0.9"), "不得泄露内部地址");
        assertFalse(body.error().retryable());
        assertTrue(body.error().details().isEmpty());
    }
}