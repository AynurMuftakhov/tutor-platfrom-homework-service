package com.speakshire.homeworkservice.repository.projection;

import com.speakshire.homeworkservice.domain.HomeworkTaskType;
import com.speakshire.homeworkservice.domain.SourceKind;

import java.util.UUID;

public interface TaskBriefProjection {
    UUID getAssignmentId();
    HomeworkTaskType getType();
    String getTitle();
    SourceKind getSourceKind();
}
