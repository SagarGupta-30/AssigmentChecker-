package com.aichecker.service.impl;

import com.aichecker.dto.*;
import com.aichecker.exception.ApiException;
import com.aichecker.model.Assignment;
import com.aichecker.model.Result;
import com.aichecker.model.Role;
import com.aichecker.model.Submission;
import com.aichecker.model.User;
import com.aichecker.repository.ResultRepository;
import com.aichecker.repository.SubmissionRepository;
import com.aichecker.service.*;
import com.aichecker.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Pattern NUMBERED_ANSWER_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*[).:-]?\\s*(.+?)\\s*$");

    private final SubmissionRepository submissionRepository;
    private final ResultRepository resultRepository;
    private final AssignmentService assignmentService;
    private final EvaluationService evaluationService;
    private final OcrService ocrService;
    private final JsonUtil jsonUtil;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository,
                                 ResultRepository resultRepository,
                                 AssignmentService assignmentService,
                                 EvaluationService evaluationService,
                                 OcrService ocrService,
                                 JsonUtil jsonUtil) {
        this.submissionRepository = submissionRepository;
        this.resultRepository = resultRepository;
        this.assignmentService = assignmentService;
        this.evaluationService = evaluationService;
        this.ocrService = ocrService;
        this.jsonUtil = jsonUtil;
    }

    @Override
    @Transactional
    public SubmitAndEvaluateResponse submitAnswers(Long assignmentId, SubmitAnswersRequest request, User student) {
        ensureStudent(student);
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);

        List<AnswerInputDto> answers = request.answers() == null ? List.of() : request.answers();
        return processSubmission(assignment, student, answers, null, null);
    }

    @Override
    public OcrExtractionResponse extractAnswersFromImage(MultipartFile imageFile) {
        String extractedText = ocrService.extractText(imageFile);
        List<AnswerInputDto> parsedAnswers = parseAnswersFromExtractedText(extractedText);
        return new OcrExtractionResponse(extractedText, parsedAnswers);
    }

    @Override
    @Transactional
    public SubmitAndEvaluateResponse submitAnswersWithImage(Long assignmentId,
                                                            MultipartFile imageFile,
                                                            List<AnswerInputDto> manualAnswers,
                                                            User student) {
        ensureStudent(student);
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);

        List<AnswerInputDto> safeManualAnswers = manualAnswers == null ? List.of() : manualAnswers;

        String extractedText = null;
        List<AnswerInputDto> parsedAnswers = List.of();
        try {
            extractedText = ocrService.extractText(imageFile);
            parsedAnswers = parseAnswersFromExtractedText(extractedText);
        } catch (ApiException ocrException) {
            if (safeManualAnswers.isEmpty()) {
                throw ocrException;
            }
        }

        List<AnswerInputDto> merged = mergeAnswers(parsedAnswers, safeManualAnswers);
        String savedPath = saveUploadedImage(imageFile, assignmentId, student.getId());

        return processSubmission(assignment, student, merged, extractedText, savedPath);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsForAssignment(Long assignmentId, User teacher) {
        if (teacher.getRole() != Role.TEACHER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only teachers can view these submissions");
        }

        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        if (!assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view your own assignment submissions");
        }

        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getMySubmissions(User student) {
        ensureStudent(student);
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResultResponse getSubmissionResult(Long submissionId, User requester) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (requester.getRole() == Role.STUDENT && !submission.getStudent().getId().equals(requester.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view your own result");
        }

        if (requester.getRole() == Role.TEACHER
                && !submission.getAssignment().getTeacher().getId().equals(requester.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view submissions for your assignments");
        }

        Result result = resultRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Result not found"));

        return toResultResponse(result);
    }

    private SubmitAndEvaluateResponse processSubmission(Assignment assignment,
                                                        User student,
                                                        List<AnswerInputDto> answers,
                                                        String extractedText,
                                                        String imagePath) {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setAnswers(jsonUtil.toJson(answers));
        submission.setExtractedText(extractedText);
        submission.setImagePath(imagePath);
        submission.setSubmittedAt(Instant.now());

        Submission savedSubmission = submissionRepository.save(submission);

        EvaluationAggregate aggregate = evaluationService.evaluate(assignment, answers);

        Result result = new Result();
        result.setSubmission(savedSubmission);
        result.setCorrectCount(aggregate.correct());
        result.setWrongCount(aggregate.wrong());
        result.setNotAttemptedCount(aggregate.notAttempted());
        result.setPercentage(aggregate.percentage());
        result.setDetailsJson(jsonUtil.toJson(aggregate.questionEvaluations()));

        Result savedResult = resultRepository.save(result);
        return new SubmitAndEvaluateResponse(savedSubmission.getId(), toResultResponse(savedResult));
    }

    private List<AnswerInputDto> parseAnswersFromExtractedText(String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return List.of();
        }

        Map<Integer, String> map = new HashMap<>();
        for (String line : extractedText.split("\\R")) {
            Matcher matcher = NUMBERED_ANSWER_PATTERN.matcher(line);
            if (matcher.matches()) {
                Integer question = Integer.parseInt(matcher.group(1));
                String answer = matcher.group(2).trim();
                if (!answer.isBlank()) {
                    map.put(question, answer);
                }
            }
        }

        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AnswerInputDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<AnswerInputDto> mergeAnswers(List<AnswerInputDto> parsed, List<AnswerInputDto> manual) {
        Map<Integer, String> merged = new HashMap<>();
        for (AnswerInputDto answer : parsed) {
            merged.put(answer.questionNumber(), answer.answer());
        }
        for (AnswerInputDto answer : manual) {
            merged.put(answer.questionNumber(), answer.answer());
        }

        return merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AnswerInputDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String saveUploadedImage(MultipartFile imageFile, Long assignmentId, Long studentId) {
        try {
            Path root = Path.of(uploadDir);
            Files.createDirectories(root);

            String fileName = "assignment-" + assignmentId + "-student-" + studentId + "-" +
                    System.currentTimeMillis() + extension(imageFile.getOriginalFilename());
            Path target = root.resolve(fileName);

            Files.copy(imageFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded image");
        }
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private SubmissionResponse toSubmissionResponse(Submission submission) {
        Double percentage = resultRepository.findBySubmissionId(submission.getId())
                .map(Result::getPercentage)
                .orElse(null);

        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getSubmittedAt(),
                percentage
        );
    }

    private ResultResponse toResultResponse(Result result) {
        List<QuestionEvaluationDto> details = jsonUtil.fromJson(
                result.getDetailsJson(),
                new TypeReference<List<QuestionEvaluationDto>>() {
                }
        );

        return new ResultResponse(
                result.getSubmission().getId(),
                result.getCorrectCount(),
                result.getWrongCount(),
                result.getNotAttemptedCount(),
                result.getPercentage(),
                details
        );
    }

    private void ensureStudent(User user) {
        if (user.getRole() != Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only students can submit assignments");
        }
    }
}
