package com.speakshire.homeworkservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentListItemDto(
        UUID id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime dueAt,
        int totalTasks,
        int completedTasks,
        int inProgressTasks,
        int progressPct,
        boolean completed,
        boolean overdue
) {}
