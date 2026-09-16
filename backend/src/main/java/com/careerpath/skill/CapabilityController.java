package com.careerpath.skill;

import com.careerpath.common.api.ApiResponse;
import com.careerpath.common.security.CurrentUser;
import com.careerpath.common.web.TraceId;
import com.careerpath.skill.dto.CapabilityMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals/{goalId}")
public class CapabilityController {

    private final CapabilityService capabilityService;

    public CapabilityController(CapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    @GetMapping("/capability-map")
    public ApiResponse<CapabilityMap> getCapabilityMap(@PathVariable UUID goalId) {
        CapabilityMap map = capabilityService.buildMap(CurrentUser.id(), goalId);
        return ApiResponse.of(map, TraceId.current());
    }
}