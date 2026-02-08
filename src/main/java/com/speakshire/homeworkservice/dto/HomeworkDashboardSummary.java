package com.speakshire.homeworkservice.dto;

import java.util.List;

public record HomeworkDashboardSummary(
        long dueCount,
        long overdueCount,
        long toReviewCount,
        List<HomeworkDashboardItemDto> nextDueItems
) {}
