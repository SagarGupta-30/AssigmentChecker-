package com.aichecker.controller;

import com.aichecker.dto.*;
import com.aichecker.security.SecurityUtil;
import com.aichecker.service.SubmissionService;
import com.aichecker.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SecurityUtil securityUtil;
    private final JsonUtil jsonUtil;

    public SubmissionController(SubmissionService submissionService, SecurityUtil securityUtil, JsonUtil jsonUtil) {
        this.submissionService = submissionService;
        this.securityUtil = securityUtil;
        this.jsonUtil = jsonUtil;
    }

    @PostMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmitAndEvaluateResponse> submitAnswers(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmitAnswersRequest request
    ) {
        return ResponseEntity.ok(submissionService.submitAnswers(
                assignmentId,
                request,
                securityUtil.getCurrentUserEntity()
        ));
    }

    @PostMapping(value = "/assignment/{assignmentId}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmitAndEvaluateResponse> submitWithImage(
            @PathVariable Long assignmentId,
            @RequestPart("file") MultipartFile imageFile,
            @RequestPart(value = "answersJson", required = false) String answersJson
    ) {
        List<AnswerInputDto> manualAnswers = answersJson == null || answersJson.isBlank()
                ? List.of()
                : jsonUtil.fromJson(answersJson, new TypeReference<List<AnswerInputDto>>() {
                });

        return ResponseEntity.ok(submissionService.submitAnswersWithImage(
                assignmentId,
                imageFile,
                manualAnswers,
                securityUtil.getCurrentUserEntity()
        ));
    }

    @PostMapping(value = "/ocr/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<OcrExtractionResponse> extractText(@RequestPart("file") MultipartFile imageFile) {
        return ResponseEntity.ok(submissionService.extractAnswersFromImage(imageFile));
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SubmissionResponse>> assignmentSubmissions(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(
                submissionService.getSubmissionsForAssignment(assignmentId, securityUtil.getCurrentUserEntity())
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SubmissionResponse>> mySubmissions() {
        return ResponseEntity.ok(submissionService.getMySubmissions(securityUtil.getCurrentUserEntity()));
    }

    @GetMapping("/{submissionId}/result")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<ResultResponse> result(@PathVariable Long submissionId) {
        return ResponseEntity.ok(
                submissionService.getSubmissionResult(submissionId, securityUtil.getCurrentUserEntity())
        );
    }
}
