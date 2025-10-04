package com.speakshire.homeworkservice.controller;

import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.AssignmentListItemDto;
import com.speakshire.homeworkservice.dto.CreateAssignmentDto;
import com.speakshire.homeworkservice.service.HomeworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/homeworks")
@RequiredArgsConstructor
public class HomeworkController {

  private final HomeworkService homeworkService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AssignmentDto create(@RequestParam UUID teacherId,
                              @Valid @RequestBody CreateAssignmentDto body) {
    return homeworkService.createAssignment(teacherId, body);
  }

  // Extended student endpoint: preserves old behavior (full) when view param is absent
  @GetMapping("student/{studentId}")
  public Page<AssignmentListItemDto> myAssignments(@PathVariable UUID studentId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to,
                                                   @RequestParam(required = false) Boolean includeOverdue,
                                                   @RequestParam(required = false) Boolean hideCompleted,
                                                   @RequestParam(required = false) String sort,
                                                   Pageable pageable) {
    boolean includeOverdueVal = includeOverdue == null || includeOverdue;
    String statusVal = (status == null || status.isBlank()) ? "active" : status;
    Boolean hideCompletedVal = hideCompleted;
    if (hideCompletedVal == null) {
      hideCompletedVal = "active".equals(statusVal);
    }

    return homeworkService.listStudentAssignments(
            studentId,
            statusVal,
            from,
            to,
            includeOverdueVal,
            hideCompletedVal,
            sort,
            pageable
    );
  }

  @GetMapping("student/{studentId}/counts")
  public Map<String, Long> myAssignmentsCounts(@PathVariable UUID studentId,
                                               @RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) Boolean includeOverdue) {
    boolean includeOverdueVal = includeOverdue == null || includeOverdue;
    return homeworkService.countStudentAssignments(studentId, from, to, includeOverdueVal);
  }

  @GetMapping("/{id}")
  public AssignmentDto getAssignmentById(@PathVariable UUID id) {
    return homeworkService.getById(id);
  }

  @DeleteMapping("/{assignmentId}")
  public ResponseEntity<?> deleteAssignment(@PathVariable UUID assignmentId) {
    homeworkService.deleteAssignment(assignmentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/tutor/{tutorId}")
  public Page<AssignmentListItemDto> tutorAssignments(@PathVariable UUID tutorId,
                                                      @RequestParam(required = false) UUID studentId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to,
                                                      @RequestParam(required = false) Boolean includeOverdue,
                                                      @RequestParam(required = false) Boolean hideCompleted,
                                                      @RequestParam(required = false) String sort,
                                                      Pageable pageable) {
    boolean includeOverdueVal = includeOverdue == null || includeOverdue;
    String statusVal = (status == null || status.isBlank()) ? "active" : status;
    Boolean hideCompletedVal = hideCompleted;
    if (hideCompletedVal == null) {
      hideCompletedVal = "active".equals(statusVal);
    }

    return homeworkService.listTutorAssignments(
            tutorId,
            Optional.ofNullable(studentId),
            statusVal,
            from,
            to,
            includeOverdueVal,
            hideCompletedVal,
            sort,
            pageable
    );
  }
}