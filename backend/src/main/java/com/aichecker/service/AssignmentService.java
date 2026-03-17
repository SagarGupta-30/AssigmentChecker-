package com.aichecker.service;

import com.aichecker.dto.AssignmentDetailResponse;
import com.aichecker.dto.AssignmentResponse;
import com.aichecker.dto.CreateAssignmentRequest;
import com.aichecker.model.Assignment;
import com.aichecker.model.User;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssignmentService {
    AssignmentResponse createAssignment(CreateAssignmentRequest request, User teacher);

    AssignmentResponse createAssignment(CreateAssignmentRequest request, MultipartFile questionImage, User teacher);

    List<AssignmentResponse> getTeacherAssignments(Long teacherId);

    List<AssignmentResponse> getAvailableAssignments();

    AssignmentDetailResponse getAssignmentDetail(Long assignmentId, User requester);

    AssignmentImageResponse loadQuestionImage(Long assignmentId, User requester);

    Assignment getAssignmentById(Long assignmentId);

    record AssignmentImageResponse(Resource resource, MediaType mediaType, String fileName) {
    }
}
