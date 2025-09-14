package com.speakshire.homeworkservice.controller;

import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.ProgressDto;
import com.speakshire.homeworkservice.service.TaskProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/homeworks/tasks")
@RequiredArgsConstructor
public class TaskProgressController {

  private final TaskProgressService progressService;

  @PostMapping("/{taskId}/start")
  public AssignmentDto start(@PathVariable UUID taskId, @RequestParam UUID studentId) {
    return progressService.start(taskId, studentId);
  }

  @PostMapping("/{taskId}/progress")
  public AssignmentDto progress(@PathVariable UUID taskId,
                                @RequestParam UUID studentId,
                                @Valid @RequestBody ProgressDto body) {
    return progressService.progress(taskId, studentId, body);
  }

  @PostMapping("/{taskId}/complete")
  public AssignmentDto complete(@PathVariable UUID taskId,
                                @RequestParam UUID studentId,
                                @RequestBody(required = false) Map<String, Object> meta) {
    return progressService.complete(taskId, studentId, meta == null ? Map.of() : meta);
  }
}