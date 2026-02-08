package com.speakshire.homeworkservice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAssignmentDto(
        UUID studentId,
        List<UUID> studentIds,
        @NotBlank String title,
        String instructions,
        OffsetDateTime dueAt,
        UUID lessonId,
        String idempotencyKey,
        @NotEmpty List<CreateTaskDto> tasks
) {

}
