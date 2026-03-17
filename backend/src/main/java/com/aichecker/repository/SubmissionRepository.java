package com.aichecker.repository;

import com.aichecker.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    List<Submission> findByAssignmentTeacherIdOrderBySubmittedAtDesc(Long teacherId);
}
