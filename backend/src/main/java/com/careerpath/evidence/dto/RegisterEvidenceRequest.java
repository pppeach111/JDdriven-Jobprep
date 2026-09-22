package com.careerpath.evidence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 登记能力证据请求，结构严格对齐 14-contracts-and-schemas.md 第 11.2 节。
 * type / direction 的枚举合法性在 Service 层校验，保证错误消息可定位。
 */
public record RegisterEvidenceRequest(

        @NotBlank(message = "证据类型不能为空")
        @Size(max = 30, message = "证据类型非法")
        String type,

        @NotBlank(message = "证据标题不能为空")
        @Size(max = 200, message = "证据标题过长")
        String title,

        @Size(max = 2000, message = "证据说明过长")
        String contentSummary,

        @DecimalMin(value = "0.0", message = "credibility 不能小于 0")
        @DecimalMax(value = "1.0", message = "credibility 不能大于 1")
        BigDecimal credibility,

        @NotEmpty(message = "至少关联一项技能")
        @Size(max = 20, message = "一条证据最多关联 20 项技能")
        @Valid
        List<Link> links
) {

    public record Link(

            @NotBlank(message = "skillId 不能为空")
            @Size(max = 100, message = "skillId 过长")
            String skillId,

            @NotBlank(message = "direction 不能为空")
            @Size(max = 20, message = "direction 非法")
            String direction,

            @Min(value = 0, message = "claimedLevel 不能小于 0")
            @Max(value = 5, message = "claimedLevel 不能大于 5")
            Integer claimedLevel,

            @DecimalMin(value = "0.0", message = "strength 不能小于 0")
            @DecimalMax(value = "1.0", message = "strength 不能大于 1")
            BigDecimal strength
    ) {
    }
}