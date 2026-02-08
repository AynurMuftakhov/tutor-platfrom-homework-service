package com.speakshire.homeworkservice.dto;

import java.time.Instant;
import java.util.UUID;

public record HomeworkDashboardItemDto(
        UUID id,
        String title,
        Instant dueAtUtc,
        String status,
        UUID lessonId,
        UUID studentId,
        UUID tutorId
) {}
