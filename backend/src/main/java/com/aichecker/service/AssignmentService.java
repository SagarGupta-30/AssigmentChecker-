package com.aichecker.service;

import com.aichecker.dto.AssignmentDetailResponse;
import com.aichecker.dto.AssignmentResponse;
import com.aichecker.dto.CreateAssignmentRequest;
import com.aichecker.model.Assignment;
import com.aichecker.model.User;

import java.util.List;

public interface AssignmentService {
    AssignmentResponse createAssignment(CreateAssignmentRequest request, User teacher);

    List<AssignmentResponse> getTeacherAssignments(Long teacherId);

    List<AssignmentResponse> getAvailableAssignments();

    AssignmentDetailResponse getAssignmentDetail(Long assignmentId, User requester);

    Assignment getAssignmentById(Long assignmentId);
}
