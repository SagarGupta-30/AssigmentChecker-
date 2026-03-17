package com.aichecker.dto;

import java.time.Instant;

public record AssignmentResponse(
        Long id,
        String title,
        Integer numberOfQuestions,
        Long teacherId,
        String teacherName,
        boolean questionImageAvailable,
        Instant createdAt
) {
}
