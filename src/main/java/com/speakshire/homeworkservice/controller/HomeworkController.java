package com.speakshire.homeworkservice.controller;

import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.CreateAssignmentDto;
import com.speakshire.homeworkservice.service.HomeworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/homeworks")
@RequiredArgsConstructor
public class HomeworkController {

  private final HomeworkService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AssignmentDto create(@RequestParam UUID teacherId,
                              @Valid @RequestBody CreateAssignmentDto body) {
    return service.createAssignment(teacherId, body);
  }

  @GetMapping("/{studentId}")
  public Page<AssignmentDto> myAssignments(@PathVariable UUID studentId, Pageable pageable) {
    return service.listForStudent(studentId, pageable);
  }

  @DeleteMapping("/{assignmentId}")
  public ResponseEntity<?> deleteAssignment(@PathVariable UUID assignmentId) {
    service.deleteAssignment(assignmentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/tutor/{tutorId}")
  public Page<AssignmentDto> tutorAssignments(@PathVariable UUID tutorId,
                                                @RequestParam(required = false) UUID studentId,
                                                Pageable pageable) {
    return service.listForTeacher(tutorId, Optional.ofNullable(studentId), pageable);
  }
}