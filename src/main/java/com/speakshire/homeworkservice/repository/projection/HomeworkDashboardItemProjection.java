package com.speakshire.homeworkservice.repository.projection;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface HomeworkDashboardItemProjection {
    UUID getId();
    String getTitle();
    OffsetDateTime getDueAt();
    UUID getLessonId();
    UUID getStudentId();
    UUID getTeacherId();
    Long getTotalTasks();
    Long getCompletedTasks();
    Long getInProgressTasks();
}
