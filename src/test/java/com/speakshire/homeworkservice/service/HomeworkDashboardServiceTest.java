package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.dto.HomeworkDashboardSummary;
import com.speakshire.homeworkservice.exception.BadRequestException;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.repository.projection.HomeworkDashboardItemProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkDashboardServiceTest {

    @Mock
    private HomeworkAssignmentRepository assignmentRepository;
    @Mock
    private Clock clock;

    private HomeworkDashboardService homeworkDashboardService;

    @BeforeEach
    void setUp() {
        homeworkDashboardService = new HomeworkDashboardService(assignmentRepository, clock);
        when(clock.instant()).thenReturn(Instant.parse("2026-02-07T12:00:00Z"));
    }

    @Test
    void studentSummary_usesStudentQueriesAndReturnsZeroToReview() {
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        when(assignmentRepository.countDashboardDueSoonForStudent(eq(studentId), any(), any())).thenReturn(2L);
        when(assignmentRepository.countDashboardOverdueForStudent(eq(studentId), any())).thenReturn(1L);
        when(assignmentRepository.findDashboardNextDueItemsForStudent(eq(studentId), any(), any(), any()))
                .thenReturn(List.of(item(
                        UUID.fromString("00000000-0000-0000-0000-000000000201"),
                        "Due soon",
                        OffsetDateTime.parse("2026-02-07T13:00:00Z"),
                        1L,
                        0L,
                        1L
                )));

        HomeworkDashboardSummary summary = homeworkDashboardService.getSummary(studentId, "student", 3);

        assertThat(summary.dueCount()).isEqualTo(2);
        assertThat(summary.overdueCount()).isEqualTo(1);
        assertThat(summary.toReviewCount()).isZero();
        assertThat(summary.nextDueItems()).hasSize(1);
        assertThat(summary.nextDueItems().get(0).status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void tutorSummary_acceptsTeacherAliasAndUsesDefaultLimit() {
        UUID tutorId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        when(assignmentRepository.countDashboardDueSoonForTutor(eq(tutorId), any(), any())).thenReturn(3L);
        when(assignmentRepository.countDashboardOverdueForTutor(eq(tutorId), any())).thenReturn(2L);
        when(assignmentRepository.countDashboardToReviewForTutor(eq(tutorId))).thenReturn(4L);
        when(assignmentRepository.findDashboardNextDueItemsForTutor(eq(tutorId), any(), any(), any()))
                .thenReturn(List.of(item(
                        UUID.fromString("00000000-0000-0000-0000-000000000202"),
                        "Tutor due item",
                        OffsetDateTime.parse("2026-02-08T13:00:00Z"),
                        2L,
                        0L,
                        0L
                )));

        HomeworkDashboardSummary summary = homeworkDashboardService.getSummary(tutorId, "TEACHER", null);

        assertThat(summary.dueCount()).isEqualTo(3);
        assertThat(summary.overdueCount()).isEqualTo(2);
        assertThat(summary.toReviewCount()).isEqualTo(4);
        assertThat(summary.nextDueItems()).hasSize(1);
        assertThat(summary.nextDueItems().get(0).status()).isEqualTo("NOT_STARTED");

        verify(assignmentRepository).findDashboardNextDueItemsForTutor(
                eq(tutorId),
                any(),
                any(),
                ArgumentMatchers.argThat((Pageable pageable) -> pageable.getPageSize() == 3)
        );
    }

    @Test
    void unsupportedRole_throwsBadRequest() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000103");

        assertThatThrownBy(() -> homeworkDashboardService.getSummary(userId, "ADMIN", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported role");
    }

    private HomeworkDashboardItemProjection item(UUID id,
                                                 String title,
                                                 OffsetDateTime dueAt,
                                                 Long totalTasks,
                                                 Long completedTasks,
                                                 Long inProgressTasks) {
        return new HomeworkDashboardItemProjection() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public OffsetDateTime getDueAt() {
                return dueAt;
            }

            @Override
            public UUID getLessonId() {
                return null;
            }

            @Override
            public UUID getStudentId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000104");
            }

            @Override
            public UUID getTeacherId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000105");
            }

            @Override
            public Long getTotalTasks() {
                return totalTasks;
            }

            @Override
            public Long getCompletedTasks() {
                return completedTasks;
            }

            @Override
            public Long getInProgressTasks() {
                return inProgressTasks;
            }
        };
    }
}
