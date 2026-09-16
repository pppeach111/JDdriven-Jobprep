package com.careerpath.jd.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JD 导入请求契约与校验错误路径测试。
 *
 * <p>rawText 是不可信输入，必须非空且有长度上限；sourceUrl 可选但受限。
 */
@DisplayName("ImportJdRequest 契约与校验")
class ImportJdRequestTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> violatedProperties(ImportJdRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("合法请求无校验错误")
    void validRequestHasNoViolations() {
        ImportJdRequest request = new ImportJdRequest(
                "面向2027届本科生，熟悉 Redis 缓存设计。",
                "https://careers.example.com/jobs/123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("sourceUrl 可选，为 null 时仍合法")
    void sourceUrlIsOptional() {
        ImportJdRequest request = new ImportJdRequest("熟悉 Java 并发编程", null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("rawText 为 null 或空白时触发 NotBlank 校验")
    void blankRawTextIsRejected() {
        for (String text : new String[]{null, "", "   "}) {
            Set<String> violations = violatedProperties(new ImportJdRequest(text, null));
            assertTrue(violations.contains("rawText"), "空白 rawText 必须被拒绝: <" + text + ">");
        }
    }

    @Test
    @DisplayName("rawText 超过 200000 字符时触发 Size 校验")
    void oversizedRawTextIsRejected() {
        ImportJdRequest request = new ImportJdRequest("a".repeat(200_001), null);

        Set<String> violations = violatedProperties(request);
        assertTrue(violations.contains("rawText"));
    }

    @Test
    @DisplayName("rawText 恰好 200000 字符时通过（上边界不误伤）")
    void rawTextAtBoundaryIsAccepted() {
        ImportJdRequest request = new ImportJdRequest("a".repeat(200_000), null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("sourceUrl 超过 1000 字符时触发 Size 校验")
    void oversizedSourceUrlIsRejected() {
        ImportJdRequest request = new ImportJdRequest("有效 JD 文本", "u".repeat(1_001));

        Set<String> violations = violatedProperties(request);
        assertTrue(violations.contains("sourceUrl"));
    }

    @Test
    @DisplayName("校验消息面向用户且不泄露内部细节")
    void violationMessagesAreUserFacing() {
        Set<String> messages = validator.validate(new ImportJdRequest("", null)).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertFalse(messages.isEmpty());
        assertEquals(Set.of("JD 内容不能为空"), messages);
        for (String message : messages) {
            assertFalse(message.contains("Exception"), "校验消息不得包含异常细节: " + message);
        }
    }
}