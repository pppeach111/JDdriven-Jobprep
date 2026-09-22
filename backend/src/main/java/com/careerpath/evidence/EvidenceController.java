package com.careerpath.evidence;

import com.careerpath.common.api.ApiResponse;
import com.careerpath.common.security.CurrentUser;
import com.careerpath.common.web.TraceId;
import com.careerpath.evidence.dto.EvidenceView;
import com.careerpath.evidence.dto.RegisterEvidenceRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 能力证据登记，见 14-contracts-and-schemas.md 第 11 节（2026-09-22 新增）。
 */
@RestController
@RequestMapping("/api/v1/goals/{goalId}/evidences")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping
    public ApiResponse<EvidenceView> register(@PathVariable UUID goalId,
                                              @Valid @RequestBody RegisterEvidenceRequest request) {
        EvidenceView view = evidenceService.register(CurrentUser.id(), goalId, request);
        return ApiResponse.of(view, TraceId.current());
    }

    @GetMapping
    public ApiResponse<List<EvidenceView>> list(@PathVariable UUID goalId) {
        List<EvidenceView> views = evidenceService.list(CurrentUser.id(), goalId);
        return ApiResponse.of(views, TraceId.current());
    }
}