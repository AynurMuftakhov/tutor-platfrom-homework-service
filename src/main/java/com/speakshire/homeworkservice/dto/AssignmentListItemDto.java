package com.speakshire.homeworkservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentListItemDto(
        UUID id,
        String title,
        UUID studentId,
        OffsetDateTime createdAt,
        OffsetDateTime dueAt,
        int totalTasks,
        int completedTasks,
        int inProgressTasks,
        int progressPct,
        boolean completed,
        boolean overdue,
        List<TaskBriefDto> tasks
) {}
