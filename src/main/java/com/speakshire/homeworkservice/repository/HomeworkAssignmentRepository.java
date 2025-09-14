package com.speakshire.homeworkservice.repository;

import com.speakshire.homeworkservice.domain.HomeworkAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, UUID> {

  Page<HomeworkAssignment> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

  Page<HomeworkAssignment> findAllByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

  Page<HomeworkAssignment> findAllByTeacherIdAndStudentIdOrderByCreatedAtDesc(
          UUID teacherId, UUID studentId, Pageable pageable);

  Optional<HomeworkAssignment> findByTeacherIdAndStudentIdAndIdempotencyKey(
          UUID teacherId, UUID studentId, String idempotencyKey);
}