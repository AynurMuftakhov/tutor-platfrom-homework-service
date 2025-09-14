package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.domain.HomeworkTask;
import com.speakshire.homeworkservice.domain.HomeworkTaskStatus;
import com.speakshire.homeworkservice.dto.ProgressDto;
import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.exception.ForbiddenException;
import com.speakshire.homeworkservice.exception.NotFoundException;
import com.speakshire.homeworkservice.mapper.AssignmentMapper;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.repository.HomeworkTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskProgressService {

  private final HomeworkTaskRepository taskRepo;
  private final HomeworkAssignmentRepository assignmentRepo;

  private HomeworkTask loadTaskEnsureStudent(UUID taskId, UUID studentId) {
    var task = taskRepo.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Task not found"));
    var owner = task.getAssignment().getStudentId();
    if (!owner.equals(studentId)) {
      throw new ForbiddenException("You are not the owner of this task");
    }
    return task;
  }

  @Transactional
  public AssignmentDto start(UUID taskId, UUID studentId) {
    var task = loadTaskEnsureStudent(taskId, studentId);
    if (task.getStatus() == HomeworkTaskStatus.NOT_STARTED) {
      task.setStatus(HomeworkTaskStatus.IN_PROGRESS);
      task.setStartedAt(OffsetDateTime.now());
      taskRepo.save(task);
    }
    // return whole assignment to simplify FE updates
    return AssignmentMapper.toDto(task.getAssignment());
  }

  @Transactional
  public AssignmentDto progress(UUID taskId, UUID studentId, ProgressDto body) {
    var task = loadTaskEnsureStudent(taskId, studentId);
    if (task.getStatus() == HomeworkTaskStatus.NOT_STARTED) {
      task.setStatus(HomeworkTaskStatus.IN_PROGRESS);
      task.setStartedAt(OffsetDateTime.now());
    }
    if (body.progressPct() != null) {
      int next = Math.max(task.getProgressPct(), Math.min(100, Math.max(0, body.progressPct())));
      task.setProgressPct(next);
    }
    if (body.meta() != null && !body.meta().isEmpty()) {
      // merge meta
      var merged = new java.util.HashMap<String, Object>();
      if (task.getMeta() != null) merged.putAll(task.getMeta());
      merged.putAll(body.meta());
      task.setMeta(merged);
    }
    taskRepo.save(task);
    return AssignmentMapper.toDto(task.getAssignment());
  }

  @Transactional
  public AssignmentDto complete(UUID taskId, UUID studentId, Map<String,Object> meta) {
    var task = loadTaskEnsureStudent(taskId, studentId);
    task.setStatus(HomeworkTaskStatus.COMPLETED);
    task.setProgressPct(100);
    if (task.getStartedAt() == null) task.setStartedAt(OffsetDateTime.now());
    task.setCompletedAt(OffsetDateTime.now());

    if (meta != null && !meta.isEmpty()) {
      var merged = new java.util.HashMap<String, Object>();
      if (task.getMeta() != null) merged.putAll(task.getMeta());
      merged.putAll(meta);
      task.setMeta(merged);
    }

    taskRepo.save(task);
    return AssignmentMapper.toDto(task.getAssignment());
  }
}