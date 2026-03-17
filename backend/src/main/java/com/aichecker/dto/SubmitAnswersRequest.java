package com.aichecker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAnswersRequest(@NotEmpty(message = "At least one answer is required")
                                   List<@Valid AnswerInputDto> answers) {
}
