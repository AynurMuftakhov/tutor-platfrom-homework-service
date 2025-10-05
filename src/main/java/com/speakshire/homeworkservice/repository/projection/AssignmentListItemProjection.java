package com.speakshire.homeworkservice.repository.projection;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AssignmentListItemProjection {
    UUID getId();
    String getTitle();
    UUID getStudentId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getDueAt();
    Integer getTotalTasks();
    Integer getCompletedTasks();
    Integer getInProgressTasks();
    Integer getProgressPct();
}
