package com.careerpath.goal;

import com.careerpath.common.error.BusinessException;
import com.careerpath.common.error.ErrorCode;
import com.careerpath.common.security.CurrentUser;
import com.careerpath.goal.dto.CreateGoalRequest;
import com.careerpath.goal.dto.JobGoalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GoalService {

    private final JobGoalRepository goalRepository;

    public GoalService(JobGoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Transactional
    public JobGoalResponse create(CreateGoalRequest request) {
        JobGoal goal = new JobGoal();
        goal.setUserId(CurrentUser.id());
        goal.setName(request.name());
        if (request.jobFamily() != null && !request.jobFamily().isBlank()) {
            goal.setJobFamily(request.jobFamily());
        }
        goal.setCity(request.city());
        goal.setEmploymentType(request.employmentType());
        goal.setGraduationYear(request.graduationYear());
        goal.setAvailableFrom(request.availableFrom());
        goal.setWeeklyHours(request.weeklyHours());
        goal.setStatus("ACTIVE");
        return JobGoalResponse.from(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<JobGoalResponse> list() {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(CurrentUser.id())
                .stream()
                .map(JobGoalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobGoalResponse get(UUID goalId) {
        return goalRepository.findById(goalId)
                .map(JobGoalResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "求职目标不存在"));
    }
}