package com.aichecker.dto;

import java.time.Instant;

public record SubmissionResponse(
        Long submissionId,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        Instant submittedAt,
        Double percentage
) {
}
