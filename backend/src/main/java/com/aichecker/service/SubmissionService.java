package com.aichecker.service;

import com.aichecker.dto.*;
import com.aichecker.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubmissionService {
    SubmitAndEvaluateResponse submitAnswers(Long assignmentId, SubmitAnswersRequest request, User student);

    OcrExtractionResponse extractAnswersFromImage(MultipartFile imageFile);

    SubmitAndEvaluateResponse submitAnswersWithImage(Long assignmentId, MultipartFile imageFile, List<AnswerInputDto> manualAnswers, User student);

    List<SubmissionResponse> getSubmissionsForAssignment(Long assignmentId, User teacher);

    List<SubmissionResponse> getMySubmissions(User student);

    ResultResponse getSubmissionResult(Long submissionId, User requester);
}
