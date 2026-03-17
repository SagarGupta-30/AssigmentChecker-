package com.aichecker.controller;

import com.aichecker.dto.AnalyticsResponse;
import com.aichecker.security.SecurityUtil;
import com.aichecker.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityUtil securityUtil;

    public AnalyticsController(AnalyticsService analyticsService, SecurityUtil securityUtil) {
        this.analyticsService = analyticsService;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AnalyticsResponse> teacherAnalytics() {
        Long teacherId = securityUtil.getCurrentUserEntity().getId();
        return ResponseEntity.ok(analyticsService.getTeacherAnalytics(teacherId));
    }
}
