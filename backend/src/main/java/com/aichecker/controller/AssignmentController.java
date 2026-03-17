package com.aichecker.controller;

import com.aichecker.dto.AssignmentDetailResponse;
import com.aichecker.dto.AssignmentResponse;
import com.aichecker.dto.CreateAssignmentRequest;
import com.aichecker.security.SecurityUtil;
import com.aichecker.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AssignmentResponse> createAssignmentWithImage(
            @Valid @RequestPart("request") CreateAssignmentRequest request,
            @RequestPart(value = "questionImage", required = false) MultipartFile questionImage
    ) {
        return ResponseEntity.ok(
                assignmentService.createAssignment(request, questionImage, securityUtil.getCurrentUserEntity())
        );
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

    @GetMapping("/{assignmentId}/question-image")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Resource> getQuestionImage(@PathVariable Long assignmentId) {
        AssignmentService.AssignmentImageResponse image = assignmentService.loadQuestionImage(
                assignmentId,
                securityUtil.getCurrentUserEntity()
        );
        String safeFileName = image.fileName().replace("\"", "");

        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"")
                .body(image.resource());
    }
}
