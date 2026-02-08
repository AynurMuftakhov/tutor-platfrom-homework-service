package com.speakshire.homeworkservice.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReassignAssignmentDto(
        @NotEmpty List<UUID> studentIds,
        OffsetDateTime dueAt,
        Map<UUID, OffsetDateTime> dueAtByStudentId
) {
}
