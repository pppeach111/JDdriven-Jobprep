package com.careerpath.goal;

import com.careerpath.common.api.ApiResponse;
import com.careerpath.common.web.TraceId;
import com.careerpath.goal.dto.CreateGoalRequest;
import com.careerpath.goal.dto.JobGoalResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ApiResponse<JobGoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        return ApiResponse.of(goalService.create(request), TraceId.current());
    }

    @GetMapping
    public ApiResponse<List<JobGoalResponse>> list() {
        return ApiResponse.of(goalService.list(), TraceId.current());
    }

    @GetMapping("/{goalId}")
    public ApiResponse<JobGoalResponse> get(@PathVariable UUID goalId) {
        return ApiResponse.of(goalService.get(goalId), TraceId.current());
    }
}