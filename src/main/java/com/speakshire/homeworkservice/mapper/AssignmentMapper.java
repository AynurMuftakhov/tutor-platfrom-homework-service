package com.speakshire.homeworkservice.mapper;

import com.speakshire.homeworkservice.domain.HomeworkAssignment;
import com.speakshire.homeworkservice.domain.HomeworkTask;
import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.TaskDto;

import java.util.Comparator;
import java.util.List;

public class AssignmentMapper {

  public static AssignmentDto toDto(HomeworkAssignment a) {
    List<TaskDto> taskDtos = a.getTasks().stream()
            .sorted(Comparator.comparing(HomeworkTask::getOrdinal))
            .map(t -> new TaskDto(
                    t.getId(),
                    t.getOrdinal(),
                    t.getType(),
                    t.getSourceKind(),
                    t.getTitle(),
                    t.getInstructions(),
                    t.getContentRef(),
                    t.getStatus(),
                    t.getProgressPct(),
                    t.getStartedAt(),
                    t.getCompletedAt(),
                    t.getMeta()
            )).toList();

    return new AssignmentDto(
            a.getId(), a.getTeacherId(), a.getStudentId(),
            a.getTitle(), a.getInstructions(),
            a.getDueAt(), a.getCreatedAt(),
            taskDtos
    );
  }
}