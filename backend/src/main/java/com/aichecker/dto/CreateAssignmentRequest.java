package com.aichecker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAssignmentRequest(
        @NotBlank(message = "Title is required") String title,
        @NotNull @Min(value = 1, message = "Number of questions must be >= 1") Integer numberOfQuestions,
        @NotEmpty(message = "Answer key is required") List<@Valid QuestionKeyDto> answerKey
) {
}
