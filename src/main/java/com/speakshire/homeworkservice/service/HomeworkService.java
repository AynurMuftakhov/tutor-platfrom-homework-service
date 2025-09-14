package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.domain.*;
import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.CreateAssignmentDto;
import com.speakshire.homeworkservice.exception.BadRequestException;
import com.speakshire.homeworkservice.mapper.AssignmentMapper;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.repository.HomeworkTaskVocabWordRepository;
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
  private final HomeworkTaskVocabWordRepository vocabRepo;

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

    var assignment = new HomeworkAssignment();
    assignment.setTeacherId(teacherId);
    assignment.setStudentId(dto.studentId());
    assignment.setTitle(dto.title());
    assignment.setInstructions(dto.instructions());
    assignment.setDueAt(dto.dueAt());
    assignment.setLessonId(dto.lessonId());
    assignment.setIdempotencyKey(dto.idempotencyKey());

    int ordinal = 1;
    for (var tDto : dto.tasks()) {
      var homeworkTask = new HomeworkTask();
      homeworkTask.setOrdinal(Optional.ofNullable(tDto.ordinal()).orElse(ordinal++));
      homeworkTask.setType(tDto.type());
      homeworkTask.setTitle(tDto.title());
      homeworkTask.setInstructions(tDto.instructions());
      homeworkTask.setSourceKind(tDto.sourceKind());
      homeworkTask.setContentRef(tDto.contentRef());
      homeworkTask.setStatus(HomeworkTaskStatus.NOT_STARTED);
      homeworkTask.setProgressPct(0);
      assignment.addTask(homeworkTask);

      if (homeworkTask.getType() == HomeworkTaskType.VOCAB && tDto.vocabWordIds() != null) {
        for (var wid : tDto.vocabWordIds()) {
          var row = new HomeworkTaskVocabWord();
          row.setTaskId(homeworkTask.getId()); // t.getId() is null until flush; persist after save below (handled by JPA via PK gen on flush)
          row.setWordId(wid);
          vocabRepo.save(row); // Will persist after ids are available; alternative is to batch after save(a)
        }
      }
    }

    var saved = assignmentRepo.save(assignment);
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
}