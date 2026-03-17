package com.aichecker.service;

import com.aichecker.dto.QuestionEvaluationDto;

import java.util.List;

public record EvaluationAggregate(
        int correct,
        int wrong,
        int notAttempted,
        double percentage,
        List<QuestionEvaluationDto> questionEvaluations
) {
}
