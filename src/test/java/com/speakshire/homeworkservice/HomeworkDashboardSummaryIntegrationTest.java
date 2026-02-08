package com.speakshire.homeworkservice;

import com.speakshire.homeworkservice.domain.HomeworkAssignment;
import com.speakshire.homeworkservice.domain.HomeworkTask;
import com.speakshire.homeworkservice.domain.HomeworkTaskStatus;
import com.speakshire.homeworkservice.domain.HomeworkTaskType;
import com.speakshire.homeworkservice.domain.SourceKind;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class HomeworkDashboardSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HomeworkAssignmentRepository assignmentRepository;

    private UUID tutorId;
    private UUID studentId;
    private UUID otherTutorId;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();

        tutorId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        otherTutorId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // due today (counts as due soon)
        saveAssignment(tutorId, studentId, "Due today", now.plusHours(1), HomeworkTaskStatus.NOT_STARTED);
        // overdue by 1 minute
        saveAssignment(tutorId, studentId, "Overdue by one minute", now.minusMinutes(1), HomeworkTaskStatus.IN_PROGRESS);
        // completed (excluded from due/overdue, used as tutor toReview equivalent)
        saveAssignment(tutorId, studentId, "Completed assignment", now.plusDays(1), HomeworkTaskStatus.COMPLETED);
        // another due-soon item for ordering and list limit checks
        saveAssignment(tutorId, studentId, "Due in two days", now.plusDays(2), HomeworkTaskStatus.IN_PROGRESS);

        // unrelated tutor data should not affect tutorId results
        saveAssignment(otherTutorId, studentId, "Other tutor due", now.plusHours(2), HomeworkTaskStatus.NOT_STARTED);
    }

    @Test
    void studentSummary_handlesDueTodayOverdueCompletedAndSorting() throws Exception {
        mockMvc.perform(get("/api/homework/dashboard/summary")
                        .param("userId", studentId.toString())
                        .param("role", "STUDENT")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueCount").value(2))
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.toReviewCount").value(0))
                .andExpect(jsonPath("$.nextDueItems.length()").value(2))
                .andExpect(jsonPath("$.nextDueItems[0].title").value("Due today"))
                .andExpect(jsonPath("$.nextDueItems[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.nextDueItems[1].title").value("Due in two days"))
                .andExpect(jsonPath("$.nextDueItems[1].status").value("IN_PROGRESS"));
    }

    @Test
    void tutorSummary_includesToReviewEquivalentAndRespectsLimit() throws Exception {
        mockMvc.perform(get("/api/homework/dashboard/summary")
                        .param("userId", tutorId.toString())
                        .param("role", "TUTOR")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueCount").value(2))
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.toReviewCount").value(1))
                .andExpect(jsonPath("$.nextDueItems.length()").value(1))
                .andExpect(jsonPath("$.nextDueItems[0].title").value("Due today"))
                .andExpect(jsonPath("$.nextDueItems[0].tutorId").value(tutorId.toString()));
    }

    private void saveAssignment(UUID teacherId,
                                UUID targetStudentId,
                                String title,
                                OffsetDateTime dueAt,
                                HomeworkTaskStatus taskStatus) {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setTeacherId(teacherId);
        assignment.setStudentId(targetStudentId);
        assignment.setTitle(title);
        assignment.setInstructions("instructions");
        assignment.setDueAt(dueAt);

        HomeworkTask task = new HomeworkTask();
        task.setOrdinal(1);
        task.setType(HomeworkTaskType.VOCAB);
        task.setTitle(title + " task");
        task.setInstructions("task instructions");
        task.setSourceKind(SourceKind.LESSON_CONTENT);
        task.setContentRef(Map.of());
        task.setStatus(taskStatus);
        task.setProgressPct(taskStatus == HomeworkTaskStatus.COMPLETED ? 100 : 20);

        assignment.addTask(task);
        assignmentRepository.save(assignment);
    }
}
