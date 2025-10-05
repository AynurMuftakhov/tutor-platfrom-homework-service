package com.speakshire.homeworkservice.mapper;

import com.speakshire.homeworkservice.dto.AssignmentListItemDto;
import com.speakshire.homeworkservice.repository.projection.AssignmentListItemProjection;

import java.time.OffsetDateTime;

public class AssignmentListItemMapper {

    public static AssignmentListItemDto fromProjection(AssignmentListItemProjection p, OffsetDateTime now) {
        int total = p.getTotalTasks() == null ? 0 : p.getTotalTasks();
        int completedTasks = p.getCompletedTasks() == null ? 0 : p.getCompletedTasks();
        int inProgressTasks = p.getInProgressTasks() == null ? 0 : p.getInProgressTasks();
        int progress = total == 0 ? 0 : Math.min(100, Math.max(0, (int) Math.round(100.0 * completedTasks / total)));
        boolean completed = total > 0 && completedTasks == total;
        boolean overdue = !completed && p.getDueAt() != null && p.getDueAt().isBefore(now);
        return new AssignmentListItemDto(
                p.getId(),
                p.getTitle(),
                p.getStudentId(),
                p.getCreatedAt(),
                p.getDueAt(),
                total,
                completedTasks,
                inProgressTasks,
                progress,
                completed,
                overdue
        );
    }
}
