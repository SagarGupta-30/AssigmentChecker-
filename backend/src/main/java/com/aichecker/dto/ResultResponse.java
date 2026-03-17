package com.aichecker.dto;

import java.util.List;

public record ResultResponse(
        Long submissionId,
        Integer correct,
        Integer wrong,
        Integer notAttempted,
        Double percentage,
        List<QuestionEvaluationDto> questions
) {
}
