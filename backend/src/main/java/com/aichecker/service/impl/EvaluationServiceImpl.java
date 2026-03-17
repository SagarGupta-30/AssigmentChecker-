package com.aichecker.service.impl;

import com.aichecker.dto.AnswerInputDto;
import com.aichecker.dto.QuestionEvaluationDto;
import com.aichecker.dto.QuestionKeyDto;
import com.aichecker.model.Assignment;
import com.aichecker.model.QuestionResultStatus;
import com.aichecker.model.QuestionType;
import com.aichecker.service.AiSemanticService;
import com.aichecker.service.EvaluationAggregate;
import com.aichecker.service.EvaluationService;
import com.aichecker.service.SemanticEvaluation;
import com.aichecker.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final JsonUtil jsonUtil;
    private final AiSemanticService aiSemanticService;

    public EvaluationServiceImpl(JsonUtil jsonUtil, AiSemanticService aiSemanticService) {
        this.jsonUtil = jsonUtil;
        this.aiSemanticService = aiSemanticService;
    }

    @Override
    public EvaluationAggregate evaluate(Assignment assignment, List<AnswerInputDto> answers) {
        List<QuestionKeyDto> answerKey = jsonUtil.fromJson(
                assignment.getAnswerKey(),
                new TypeReference<List<QuestionKeyDto>>() {
                }
        );

        Map<Integer, String> answerMap = new HashMap<>();
        for (AnswerInputDto answer : answers) {
            answerMap.put(answer.questionNumber(), answer.answer());
        }

        List<QuestionEvaluationDto> details = new ArrayList<>();
        int correct = 0;
        int wrong = 0;
        int notAttempted = 0;

        for (QuestionKeyDto key : answerKey.stream().sorted(Comparator.comparingInt(QuestionKeyDto::questionNumber)).toList()) {
            String expected = safeTrim(key.expectedAnswer());
            String student = safeTrim(answerMap.get(key.questionNumber()));

            if (student.isBlank()) {
                notAttempted++;
                details.add(new QuestionEvaluationDto(
                        key.questionNumber(),
                        key.topic(),
                        expected,
                        "",
                        QuestionResultStatus.UNATTEMPTED,
                        "Question not attempted",
                        0.0
                ));
                continue;
            }

            if (key.questionType() == QuestionType.MCQ) {
                boolean isCorrect = expected.equalsIgnoreCase(student);
                if (isCorrect) {
                    correct++;
                    details.add(new QuestionEvaluationDto(
                            key.questionNumber(),
                            key.topic(),
                            expected,
                            student,
                            QuestionResultStatus.CORRECT,
                            "Exact match",
                            1.0
                    ));
                } else {
                    wrong++;
                    details.add(new QuestionEvaluationDto(
                            key.questionNumber(),
                            key.topic(),
                            expected,
                            student,
                            QuestionResultStatus.WRONG,
                            "Answer does not match expected option",
                            1.0
                    ));
                }
                continue;
            }

            SemanticEvaluation semanticEvaluation = aiSemanticService.evaluate(expected, student);
            QuestionResultStatus status = semanticEvaluation.correct()
                    ? QuestionResultStatus.CORRECT
                    : QuestionResultStatus.WRONG;

            if (status == QuestionResultStatus.CORRECT) {
                correct++;
            } else {
                wrong++;
            }

            details.add(new QuestionEvaluationDto(
                    key.questionNumber(),
                    key.topic(),
                    expected,
                    student,
                    status,
                    semanticEvaluation.explanation(),
                    semanticEvaluation.confidence()
            ));
        }

        double percentage = answerKey.isEmpty() ? 0.0 : (correct * 100.0) / answerKey.size();
        percentage = Math.round(percentage * 100.0) / 100.0;

        return new EvaluationAggregate(correct, wrong, notAttempted, percentage, details);
    }

    private String safeTrim(String input) {
        return input == null ? "" : input.trim();
    }
}
