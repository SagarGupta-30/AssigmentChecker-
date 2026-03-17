package com.aichecker.dto;

import com.aichecker.model.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionKeyDto(
        @NotNull @Min(value = 1, message = "Question number must be >= 1") Integer questionNumber,
        @NotNull QuestionType questionType,
        @NotBlank(message = "Expected answer is required") String expectedAnswer,
        @NotBlank(message = "Topic is required") String topic
) {
}
