package com.aichecker.service;

public interface AiSemanticService {
    SemanticEvaluation evaluate(String expectedAnswer, String studentAnswer);
}
