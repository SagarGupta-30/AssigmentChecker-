package com.aichecker.service;

public record SemanticEvaluation(boolean correct, double confidence, String explanation, boolean aiUsed) {
}
