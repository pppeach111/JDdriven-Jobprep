package com.careerpath.jd;

import com.careerpath.common.api.ApiResponse;
import com.careerpath.common.web.TraceId;
import com.careerpath.jd.dto.ImportJdRequest;
import com.careerpath.jd.dto.JdImportResponse;
import com.careerpath.jd.dto.JdParseResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals/{goalId}")
public class JdController {

    private final JdService jdService;

    public JdController(JdService jdService) {
        this.jdService = jdService;
    }

    @PostMapping("/jd")
    public ApiResponse<JdImportResponse> importJd(@PathVariable UUID goalId,
                                                  @Valid @RequestBody ImportJdRequest request) {
        SourceJd jd = jdService.importJd(goalId, request);
        return ApiResponse.of(new JdImportResponse(jd.getId(), jd.getParseStatus()), TraceId.current());
    }

    @PostMapping("/analyze")
    public ApiResponse<JdParseResult> analyze(@PathVariable UUID goalId) {
        return ApiResponse.of(jdService.analyze(goalId), TraceId.current());
    }
}