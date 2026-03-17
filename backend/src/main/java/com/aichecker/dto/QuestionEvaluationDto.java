package com.aichecker.dto;

import com.aichecker.model.QuestionResultStatus;

public record QuestionEvaluationDto(
        Integer questionNumber,
        String topic,
        String expectedAnswer,
        String studentAnswer,
        QuestionResultStatus status,
        String explanation,
        Double aiConfidence
) {
}
