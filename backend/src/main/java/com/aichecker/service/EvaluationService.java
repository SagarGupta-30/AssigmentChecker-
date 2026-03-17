package com.aichecker.service;

import com.aichecker.dto.AnswerInputDto;
import com.aichecker.model.Assignment;

import java.util.List;

public interface EvaluationService {
    EvaluationAggregate evaluate(Assignment assignment, List<AnswerInputDto> answers);
}
