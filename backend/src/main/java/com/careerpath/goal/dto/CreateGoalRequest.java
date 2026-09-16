package com.careerpath.goal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 创建求职目标请求。字段范围依据 14-contracts-and-schemas.md 第 2.2 节。
 */
public record CreateGoalRequest(

        @NotBlank(message = "目标名称不能为空")
        @Size(max = 200, message = "目标名称过长")
        String name,

        @Size(max = 60)
        String jobFamily,

        @Size(max = 100)
        String city,

        @Size(max = 30)
        String employmentType,

        @Min(value = 2000, message = "毕业年份不合法")
        @Max(value = 2100, message = "毕业年份不合法")
        Integer graduationYear,

        LocalDate availableFrom,

        @Min(value = 1, message = "每周可用时间不合法")
        @Max(value = 168, message = "每周可用时间不合法")
        Integer weeklyHours
) {
}