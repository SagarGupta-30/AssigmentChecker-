package com.aichecker.service.impl;

import com.aichecker.dto.AnalyticsResponse;
import com.aichecker.dto.QuestionEvaluationDto;
import com.aichecker.dto.WeakTopicDto;
import com.aichecker.model.QuestionResultStatus;
import com.aichecker.model.Result;
import com.aichecker.repository.ResultRepository;
import com.aichecker.service.AnalyticsService;
import com.aichecker.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ResultRepository resultRepository;
    private final JsonUtil jsonUtil;

    public AnalyticsServiceImpl(ResultRepository resultRepository, JsonUtil jsonUtil) {
        this.resultRepository = resultRepository;
        this.jsonUtil = jsonUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getTeacherAnalytics(Long teacherId) {
        List<Result> results = resultRepository.findBySubmissionAssignmentTeacherIdOrderByEvaluatedAtDesc(teacherId);

        if (results.isEmpty()) {
            return new AnalyticsResponse(0.0, 0.0, null, 0, List.of());
        }

        double avg = results.stream()
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);

        Result highest = results.stream()
                .max(Comparator.comparing(Result::getPercentage))
                .orElse(results.get(0));

        Map<String, Integer> weakTopicCounts = new HashMap<>();
        for (Result result : results) {
            List<QuestionEvaluationDto> details = jsonUtil.fromJson(
                    result.getDetailsJson(),
                    new TypeReference<List<QuestionEvaluationDto>>() {
                    }
            );
            for (QuestionEvaluationDto detail : details) {
                if (detail.status() == QuestionResultStatus.WRONG
                        || detail.status() == QuestionResultStatus.UNATTEMPTED) {
                    weakTopicCounts.merge(detail.topic(), 1, Integer::sum);
                }
            }
        }

        List<WeakTopicDto> weakTopics = weakTopicCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> new WeakTopicDto(entry.getKey(), entry.getValue()))
                .limit(8)
                .toList();

        avg = Math.round(avg * 100.0) / 100.0;
        double highestScore = Math.round(highest.getPercentage() * 100.0) / 100.0;
        String highestScorer = highest.getSubmission().getStudent().getName();

        return new AnalyticsResponse(avg, highestScore, highestScorer, results.size(), weakTopics);
    }
}
