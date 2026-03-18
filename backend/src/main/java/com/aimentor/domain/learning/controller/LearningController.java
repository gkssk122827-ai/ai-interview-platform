package com.aimentor.domain.learning.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.domain.learning.dto.request.LearningGenerateRequest;
import com.aimentor.domain.learning.dto.request.LearningGradeRequest;
import com.aimentor.domain.learning.dto.response.LearningGenerateResponse;
import com.aimentor.domain.learning.dto.response.LearningGradeResponse;
import com.aimentor.domain.learning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/learning", "/api/v1/learning"})
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping("/generate")
    public ApiResponse<LearningGenerateResponse> generate(@Valid @RequestBody LearningGenerateRequest request) {
        return ApiResponse.success(learningService.generateProblems(request));
    }

    @PostMapping("/grade")
    public ApiResponse<LearningGradeResponse> grade(@Valid @RequestBody LearningGradeRequest request) {
        return ApiResponse.success(learningService.gradeAnswer(request));
    }
}
