package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.dto.HomeworkDashboardItemDto;
import com.speakshire.homeworkservice.dto.HomeworkDashboardSummary;
import com.speakshire.homeworkservice.exception.BadRequestException;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.repository.projection.HomeworkDashboardItemProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeworkDashboardService {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 20;

    private final HomeworkAssignmentRepository assignmentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public HomeworkDashboardSummary getSummary(UUID userId, String role, Integer limit) {
        DashboardRole dashboardRole = parseRole(role);
        int safeLimit = sanitizeLimit(limit);

        Instant nowInstant = Instant.now(clock);
        OffsetDateTime nowUtc = nowInstant.atOffset(ZoneOffset.UTC);
        OffsetDateTime dueSoonWindowEnd = nowUtc.plusDays(7);

        if (dashboardRole == DashboardRole.STUDENT) {
            long dueCount = assignmentRepository.countDashboardDueSoonForStudent(userId, nowUtc, dueSoonWindowEnd);
            long overdueCount = assignmentRepository.countDashboardOverdueForStudent(userId, nowUtc);
            List<HomeworkDashboardItemDto> nextDueItems = assignmentRepository
                    .findDashboardNextDueItemsForStudent(userId, nowUtc, dueSoonWindowEnd, PageRequest.of(0, safeLimit))
                    .stream()
                    .map(this::toDashboardItem)
                    .toList();

            return new HomeworkDashboardSummary(dueCount, overdueCount, 0, nextDueItems);
        }

        long dueCount = assignmentRepository.countDashboardDueSoonForTutor(userId, nowUtc, dueSoonWindowEnd);
        long overdueCount = assignmentRepository.countDashboardOverdueForTutor(userId, nowUtc);
        long toReviewCount = assignmentRepository.countDashboardToReviewForTutor(userId);
        List<HomeworkDashboardItemDto> nextDueItems = assignmentRepository
                .findDashboardNextDueItemsForTutor(userId, nowUtc, dueSoonWindowEnd, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toDashboardItem)
                .toList();

        return new HomeworkDashboardSummary(dueCount, overdueCount, toReviewCount, nextDueItems);
    }

    private HomeworkDashboardItemDto toDashboardItem(HomeworkDashboardItemProjection projection) {
        return new HomeworkDashboardItemDto(
                projection.getId(),
                projection.getTitle(),
                projection.getDueAt() == null ? null : projection.getDueAt().toInstant(),
                resolveAssignmentStatus(projection),
                projection.getLessonId(),
                projection.getStudentId(),
                projection.getTeacherId()
        );
    }

    private String resolveAssignmentStatus(HomeworkDashboardItemProjection projection) {
        long totalTasks = projection.getTotalTasks() == null ? 0 : projection.getTotalTasks();
        long completedTasks = projection.getCompletedTasks() == null ? 0 : projection.getCompletedTasks();
        long inProgressTasks = projection.getInProgressTasks() == null ? 0 : projection.getInProgressTasks();

        if (totalTasks > 0 && completedTasks >= totalTasks) {
            return "COMPLETED";
        }
        if (inProgressTasks > 0) {
            return "IN_PROGRESS";
        }
        return "NOT_STARTED";
    }

    private DashboardRole parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            throw new BadRequestException("role is required and must be STUDENT or TUTOR");
        }

        String normalized = roleValue.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "STUDENT" -> DashboardRole.STUDENT;
            case "TUTOR", "TEACHER" -> DashboardRole.TUTOR;
            default -> throw new BadRequestException("Unsupported role: " + roleValue);
        };
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private enum DashboardRole {
        STUDENT,
        TUTOR
    }
}
