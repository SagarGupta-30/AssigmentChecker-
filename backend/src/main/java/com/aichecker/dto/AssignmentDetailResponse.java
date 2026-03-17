package com.aichecker.dto;

import java.time.Instant;
import java.util.List;

public record AssignmentDetailResponse(
        Long id,
        String title,
        Integer numberOfQuestions,
        Long teacherId,
        String teacherName,
        Instant createdAt,
        boolean questionImageAvailable,
        List<QuestionKeyDto> answerKey
) {
}
