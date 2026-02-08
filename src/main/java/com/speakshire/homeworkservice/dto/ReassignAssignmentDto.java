package com.speakshire.homeworkservice.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReassignAssignmentDto(
        @NotEmpty List<UUID> studentIds
) {
}
