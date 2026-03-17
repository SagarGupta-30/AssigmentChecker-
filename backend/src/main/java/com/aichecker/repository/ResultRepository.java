package com.aichecker.repository;

import com.aichecker.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {
    Optional<Result> findBySubmissionId(Long submissionId);

    List<Result> findBySubmissionStudentIdOrderByEvaluatedAtDesc(Long studentId);

    List<Result> findBySubmissionAssignmentTeacherIdOrderByEvaluatedAtDesc(Long teacherId);
}
