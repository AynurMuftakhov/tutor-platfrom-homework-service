package com.speakshire.homeworkservice.dto;

import com.speakshire.homeworkservice.domain.HomeworkTaskType;
import com.speakshire.homeworkservice.domain.SourceKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateTaskDto(
        @NotNull HomeworkTaskType type,
        @NotNull SourceKind sourceKind,
        @NotBlank String title,
        String instructions,
        Integer ordinal,
        @NotNull Map<String, Object> contentRef,
        List<UUID> vocabWordIds
) {}