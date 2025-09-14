package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.domain.*;
import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.CreateAssignmentDto;
import com.speakshire.homeworkservice.exception.BadRequestException;
import com.speakshire.homeworkservice.mapper.AssignmentMapper;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeworkService {

  private final HomeworkAssignmentRepository assignmentRepo;

  @Transactional
  public AssignmentDto createAssignment(UUID teacherId, CreateAssignmentDto dto) {
    if (dto.tasks() == null || dto.tasks().isEmpty()) {
      throw new BadRequestException("At least one task is required");
    }

    // Idempotency
    if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
      var existing = assignmentRepo.findByTeacherIdAndStudentIdAndIdempotencyKey(
              teacherId, dto.studentId(), dto.idempotencyKey());
      if (existing.isPresent()) return AssignmentMapper.toDto(existing.get());
    }

    var assignment = buildHomeworkAssignment(teacherId, dto);

    int ordinal = 1;
    for (var tDto : dto.tasks()) {
      var task = new HomeworkTask();
      // Let Hibernate generate id
      task.setOrdinal(Optional.ofNullable(tDto.ordinal()).orElse(ordinal++));
      task.setType(tDto.type());
      task.setTitle(tDto.title());
      task.setInstructions(tDto.instructions());
      task.setSourceKind(tDto.sourceKind());
      task.setContentRef(Optional.ofNullable(tDto.contentRef()).orElse(Map.of()));
      task.setStatus(HomeworkTaskStatus.NOT_STARTED);
      task.setProgressPct(0);

      assignment.addTask(task);

      // If VOCAB task, attach vocab rows to the task (cascade persists them)
      if (task.getType() == HomeworkTaskType.VOCAB && tDto.vocabWordIds() != null) {
        for (UUID wid : tDto.vocabWordIds()) {
          task.addVocabWord(wid);
        }
      }
    }

    var saved = assignmentRepo.save(assignment); // cascades tasks & vocab words
    return AssignmentMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public Page<AssignmentDto> listForStudent(UUID studentId, Pageable pageable) {
    return assignmentRepo.findAllByStudentIdOrderByCreatedAtDesc(studentId, pageable)
            .map(AssignmentMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<AssignmentDto> listForTeacher(UUID teacherId, Optional<UUID> studentId, Pageable pageable) {
    var page = studentId
            .map(sid -> assignmentRepo.findAllByTeacherIdAndStudentIdOrderByCreatedAtDesc(teacherId, sid, pageable))
            .orElseGet(() -> assignmentRepo.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, pageable));
    return page.map(AssignmentMapper::toDto);
  }

  public void deleteAssignment(UUID assignmentId) {
    assignmentRepo.deleteById(assignmentId);
  }

  private HomeworkAssignment buildHomeworkAssignment(UUID teacherId, CreateAssignmentDto dto) {
    var assignment = new HomeworkAssignment();
    assignment.setTeacherId(teacherId);
    assignment.setStudentId(dto.studentId());
    assignment.setTitle(dto.title());
    assignment.setInstructions(dto.instructions());
    assignment.setDueAt(dto.dueAt());
    assignment.setLessonId(dto.lessonId());
    assignment.setIdempotencyKey(dto.idempotencyKey());
    return assignment;
  }
}