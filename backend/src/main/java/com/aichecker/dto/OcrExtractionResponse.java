package com.aichecker.dto;

import java.util.List;

public record OcrExtractionResponse(String extractedText, List<AnswerInputDto> parsedAnswers) {
}
