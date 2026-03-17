package com.aichecker.service.impl;

import com.aichecker.dto.*;
import com.aichecker.exception.ApiException;
import com.aichecker.model.Assignment;
import com.aichecker.model.Role;
import com.aichecker.model.User;
import com.aichecker.repository.AssignmentRepository;
import com.aichecker.service.AssignmentService;
import com.aichecker.util.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final JsonUtil jsonUtil;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository, JsonUtil jsonUtil) {
        this.assignmentRepository = assignmentRepository;
        this.jsonUtil = jsonUtil;
    }

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, User teacher) {
        if (teacher.getRole() != Role.TEACHER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only teachers can create assignments");
        }

        List<QuestionKeyDto> sortedKeys = request.answerKey().stream()
                .sorted(Comparator.comparingInt(QuestionKeyDto::questionNumber))
                .toList();

        if (sortedKeys.size() != request.numberOfQuestions()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "numberOfQuestions must match answerKey size");
        }

        Set<Integer> unique = sortedKeys.stream()
                .map(QuestionKeyDto::questionNumber)
                .collect(Collectors.toSet());

        if (unique.size() != sortedKeys.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate question numbers in answer key");
        }

        boolean outOfRange = unique.stream().anyMatch(q -> q < 1 || q > request.numberOfQuestions());
        if (outOfRange) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Question numbers must be between 1 and numberOfQuestions");
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(request.title().trim());
        assignment.setNumberOfQuestions(request.numberOfQuestions());
        assignment.setTeacher(teacher);
        assignment.setAnswerKey(jsonUtil.toJson(sortedKeys));

        Assignment saved = assignmentRepository.save(assignment);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getTeacherAssignments(Long teacherId) {
        return assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAvailableAssignments() {
        return assignmentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDetailResponse getAssignmentDetail(Long assignmentId, User requester) {
        Assignment assignment = getAssignmentById(assignmentId);

        boolean isOwnerTeacher = requester.getRole() == Role.TEACHER
                && assignment.getTeacher().getId().equals(requester.getId());

        List<QuestionKeyDto> answerKey = null;
        if (isOwnerTeacher) {
            answerKey = jsonUtil.fromJson(
                    assignment.getAnswerKey(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<QuestionKeyDto>>() {
                    }
            );
        }

        return new AssignmentDetailResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getNumberOfQuestions(),
                assignment.getTeacher().getId(),
                assignment.getTeacher().getName(),
                assignment.getCreatedAt(),
                answerKey
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getNumberOfQuestions(),
                assignment.getTeacher().getId(),
                assignment.getTeacher().getName(),
                assignment.getCreatedAt()
        );
    }
}
