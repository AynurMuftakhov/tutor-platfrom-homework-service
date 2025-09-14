package com.speakshire.homeworkservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentDto(
        UUID id, UUID teacherId, UUID studentId,
        String title, String instructions,
        OffsetDateTime dueAt, OffsetDateTime createdAt,
        List<TaskDto> tasks
){}