package com.aichecker.controller;

import com.aichecker.dto.AssignmentDetailResponse;
import com.aichecker.dto.AssignmentResponse;
import com.aichecker.dto.CreateAssignmentRequest;
import com.aichecker.security.SecurityUtil;
import com.aichecker.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SecurityUtil securityUtil;

    public AssignmentController(AssignmentService assignmentService, SecurityUtil securityUtil) {
        this.assignmentService = assignmentService;
        this.securityUtil = securityUtil;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AssignmentResponse> createAssignment(@Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.createAssignment(request, securityUtil.getCurrentUserEntity()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AssignmentResponse>> myAssignments() {
        Long teacherId = securityUtil.getCurrentUserEntity().getId();
        return ResponseEntity.ok(assignmentService.getTeacherAssignments(teacherId));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<List<AssignmentResponse>> availableAssignments() {
        return ResponseEntity.ok(assignmentService.getAvailableAssignments());
    }

    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<AssignmentDetailResponse> getAssignment(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentDetail(assignmentId, securityUtil.getCurrentUserEntity())
        );
    }
}
