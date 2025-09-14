package com.speakshire.homeworkservice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAssignmentDto(
        @NotNull UUID studentId,
        @NotBlank String title,
        String instructions,
        OffsetDateTime dueAt,
        UUID lessonId,
        String idempotencyKey,
        @NotEmpty List<CreateTaskDto> tasks
) {

}
