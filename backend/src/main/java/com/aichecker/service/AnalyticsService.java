package com.aichecker.service;

import com.aichecker.dto.AnalyticsResponse;

public interface AnalyticsService {
    AnalyticsResponse getTeacherAnalytics(Long teacherId);
}
