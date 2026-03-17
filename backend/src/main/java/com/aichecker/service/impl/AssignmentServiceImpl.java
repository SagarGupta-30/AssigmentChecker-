package com.aichecker.service.impl;

import com.aichecker.dto.*;
import com.aichecker.exception.ApiException;
import com.aichecker.model.Assignment;
import com.aichecker.model.Role;
import com.aichecker.model.User;
import com.aichecker.repository.AssignmentRepository;
import com.aichecker.service.AssignmentService;
import com.aichecker.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final JsonUtil jsonUtil;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository, JsonUtil jsonUtil) {
        this.assignmentRepository = assignmentRepository;
        this.jsonUtil = jsonUtil;
    }

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, User teacher) {
        return createAssignment(request, null, teacher);
    }

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, MultipartFile questionImage, User teacher) {
        if (teacher.getRole() != Role.TEACHER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only teachers can create assignments");
        }

        List<QuestionKeyDto> sortedKeys = request.answerKey().stream()
                .sorted(Comparator.comparingInt(QuestionKeyDto::questionNumber))
                .toList();

        validateAnswerKey(request.numberOfQuestions(), sortedKeys);

        Assignment assignment = new Assignment();
        assignment.setTitle(request.title().trim());
        assignment.setNumberOfQuestions(request.numberOfQuestions());
        assignment.setTeacher(teacher);
        assignment.setAnswerKey(jsonUtil.toJson(sortedKeys));
        assignment.setQuestionImagePath(null);

        Assignment saved = assignmentRepository.save(assignment);
        if (questionImage != null && !questionImage.isEmpty()) {
            String questionImagePath = saveQuestionImage(questionImage, saved.getId(), teacher.getId());
            saved.setQuestionImagePath(questionImagePath);
            saved = assignmentRepository.save(saved);
        }

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
                hasQuestionImage(assignment),
                answerKey
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentImageResponse loadQuestionImage(Long assignmentId, User requester) {
        Assignment assignment = getAssignmentById(assignmentId);
        authorizeQuestionImageAccess(assignment, requester);

        if (!hasQuestionImage(assignment)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Question image not found for this assignment");
        }

        Path imagePath = Path.of(assignment.getQuestionImagePath());
        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Question image file not found");
        }

        String contentType;
        try {
            contentType = Files.probeContentType(imagePath);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read question image");
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        Resource resource = new FileSystemResource(imagePath.toFile());
        return new AssignmentImageResponse(resource, mediaType, imagePath.getFileName().toString());
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
                hasQuestionImage(assignment),
                assignment.getCreatedAt()
        );
    }

    private void validateAnswerKey(Integer numberOfQuestions, List<QuestionKeyDto> sortedKeys) {
        if (sortedKeys.size() != numberOfQuestions) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "numberOfQuestions must match answerKey size");
        }

        Set<Integer> unique = sortedKeys.stream()
                .map(QuestionKeyDto::questionNumber)
                .collect(Collectors.toSet());

        if (unique.size() != sortedKeys.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate question numbers in answer key");
        }

        boolean outOfRange = unique.stream().anyMatch(q -> q < 1 || q > numberOfQuestions);
        if (outOfRange) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Question numbers must be between 1 and numberOfQuestions");
        }
    }

    private String saveQuestionImage(MultipartFile questionImage, Long assignmentId, Long teacherId) {
        validateImageFile(questionImage);

        try {
            Path root = Path.of(uploadDir, "question-images");
            Files.createDirectories(root);

            String fileName = "assignment-" + assignmentId + "-teacher-" + teacherId + "-"
                    + System.currentTimeMillis() + extension(questionImage.getOriginalFilename());
            Path target = root.resolve(fileName).normalize();

            Files.copy(questionImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save question image");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded question image is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
    }

    private void authorizeQuestionImageAccess(Assignment assignment, User requester) {
        if (requester.getRole() == Role.STUDENT) {
            return;
        }

        if (requester.getRole() == Role.TEACHER && assignment.getTeacher().getId().equals(requester.getId())) {
            return;
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this question image");
    }

    private boolean hasQuestionImage(Assignment assignment) {
        return assignment.getQuestionImagePath() != null && !assignment.getQuestionImagePath().isBlank();
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }

        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return extension.length() > 10 ? ".png" : extension;
    }
}
