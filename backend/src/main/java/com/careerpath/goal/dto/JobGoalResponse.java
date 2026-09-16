package com.careerpath.goal.dto;

import com.careerpath.goal.JobGoal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobGoalResponse(
        UUID id,
        String name,
        String jobFamily,
        String city,
        String employmentType,
        Integer graduationYear,
        LocalDate availableFrom,
        Integer weeklyHours,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static JobGoalResponse from(JobGoal goal) {
        return new JobGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getJobFamily(),
                goal.getCity(),
                goal.getEmploymentType(),
                goal.getGraduationYear(),
                goal.getAvailableFrom(),
                goal.getWeeklyHours(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}