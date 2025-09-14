package com.speakshire.homeworkservice.dto;

import com.speakshire.homeworkservice.domain.HomeworkTaskStatus;
import com.speakshire.homeworkservice.domain.HomeworkTaskType;
import com.speakshire.homeworkservice.domain.SourceKind;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TaskDto(
        UUID id, Integer ordinal,
        HomeworkTaskType type, SourceKind sourceKind,
        String title, String instructions,
        Map<String,Object> contentRef,
        HomeworkTaskStatus status, Integer progressPct,
        OffsetDateTime startedAt, OffsetDateTime completedAt,
        Map<String,Object> meta
) {}