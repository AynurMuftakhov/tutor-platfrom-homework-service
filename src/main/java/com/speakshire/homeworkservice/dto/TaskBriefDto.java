package com.speakshire.homeworkservice.dto;

import com.speakshire.homeworkservice.domain.HomeworkTaskType;
import com.speakshire.homeworkservice.domain.SourceKind;

public record TaskBriefDto(
        HomeworkTaskType type,
        String title,
        SourceKind sourceKind
) {}
