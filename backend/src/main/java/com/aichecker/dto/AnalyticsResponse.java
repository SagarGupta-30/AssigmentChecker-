package com.aichecker.dto;

import java.util.List;

public record AnalyticsResponse(
        Double averageScore,
        Double highestScore,
        String highestScorer,
        Integer totalSubmissions,
        List<WeakTopicDto> weakTopics
) {
}
