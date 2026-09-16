package com.careerpath.jd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 导入目标 JD 请求。rawText 为不可信输入，只作为待分析内容。
 */
public record ImportJdRequest(

        @NotBlank(message = "JD 内容不能为空")
        @Size(max = 200000, message = "JD 内容过长")
        String rawText,

        @Size(max = 1000, message = "来源地址过长")
        String sourceUrl
) {
}