package com.aichecker.repository;

import com.aichecker.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Assignment> findAllByOrderByCreatedAtDesc();
}
