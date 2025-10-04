package com.speakshire.homeworkservice;

import com.speakshire.homeworkservice.domain.*;
import com.speakshire.homeworkservice.dto.AssignmentListItemDto;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.service.HomeworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class StudentAssignmentsTests {

    @Autowired
    HomeworkService homeworkService;

    @Autowired
    HomeworkAssignmentRepository assignmentRepository;

    private UUID studentId;

    @BeforeEach
    void setup() {
        studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // A1: not finished, not overdue
        HomeworkAssignment a1 = new HomeworkAssignment();
        a1.setTeacherId(teacherId);
        a1.setStudentId(studentId);
        a1.setTitle("A1");
        a1.setInstructions("i1");
        a1.setDueAt(now.plusDays(2));
        addTask(a1, 1, HomeworkTaskStatus.NOT_STARTED, 0);
        addTask(a1, 2, HomeworkTaskStatus.IN_PROGRESS, 50);
        assignmentRepository.save(a1);

        // A2: not finished, overdue
        HomeworkAssignment a2 = new HomeworkAssignment();
        a2.setTeacherId(teacherId);
        a2.setStudentId(studentId);
        a2.setTitle("A2");
        a2.setInstructions("i2");
        a2.setDueAt(now.minusDays(1));
        addTask(a2, 1, HomeworkTaskStatus.NOT_STARTED, 0);
        assignmentRepository.save(a2);

        // A3: completed
        HomeworkAssignment a3 = new HomeworkAssignment();
        a3.setTeacherId(teacherId);
        a3.setStudentId(studentId);
        a3.setTitle("A3");
        a3.setInstructions("i3");
        a3.setDueAt(now.plusDays(1));
        addTask(a3, 1, HomeworkTaskStatus.COMPLETED, 100);
        addTask(a3, 2, HomeworkTaskStatus.COMPLETED, 100);
        assignmentRepository.save(a3);
    }

    private void addTask(HomeworkAssignment a, int ordinal, HomeworkTaskStatus status, int progress) {
        HomeworkTask t = new HomeworkTask();
        t.setOrdinal(ordinal);
        t.setType(HomeworkTaskType.VOCAB);
        t.setTitle("T" + ordinal);
        t.setInstructions("TI");
        t.setSourceKind(SourceKind.LESSON_CONTENT);
        t.setContentRef(Map.of());
        t.setStatus(status);
        t.setProgressPct(progress);
        a.addTask(t);
    }

    @Test
    void counts_endpoint_logic_via_service() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Long> counts = homeworkService.countStudentAssignments(studentId, now.minusDays(7).toString(), now.toString(), true);
        assertThat(counts.get("notFinished")).isEqualTo(2L);
        assertThat(counts.get("completed")).isEqualTo(1L);
        assertThat(counts.get("overdue")).isEqualTo(1L);
        assertThat(counts.get("active")).isEqualTo(2L);
        assertThat(counts.get("all")).isEqualTo(3L);
    }

    @Test
    void summary_listing_active_includes_overdue_out_of_range() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // Narrow range to exclude createdAt (which is ~now) so only overdue can be included
        OffsetDateTime from = now.minusHours(2);
        OffsetDateTime to = now.minusHours(1);
        Page<AssignmentListItemDto> page = homeworkService.listStudentAssignments(
                studentId,
                "active",
                from,
                to,
                true,
                true,
                "assigned_desc",
                PageRequest.of(0, 10)
        );
        assertThat(page.getContent()).extracting("title").containsExactly("A2");
        assertThat(page.getContent().get(0).overdue()).isTrue();
    }
}
