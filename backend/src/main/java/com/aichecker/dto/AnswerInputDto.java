package com.aichecker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AnswerInputDto(
        @NotNull @Min(value = 1, message = "Question number must be >= 1") Integer questionNumber,
        String answer
) {
}
