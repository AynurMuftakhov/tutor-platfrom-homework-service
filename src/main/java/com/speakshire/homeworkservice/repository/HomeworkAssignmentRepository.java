package com.speakshire.homeworkservice.repository;

import com.speakshire.homeworkservice.domain.HomeworkAssignment;
import com.speakshire.homeworkservice.repository.projection.AssignmentListItemProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, UUID> {

  Optional<HomeworkAssignment> findByTeacherIdAndStudentIdAndIdempotencyKey(
          UUID teacherId, UUID studentId, String idempotencyKey);

  // Student-focused projections
  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.studentId = :studentId group by a.id")
  Page<AssignmentListItemProjection> listItemsBase(@Param("studentId") UUID studentId, Pageable pageable);

  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.studentId = :studentId and a.createdAt between :from and :to group by a.id")
  Page<AssignmentListItemProjection> listItemsWithin(@Param("studentId") UUID studentId,
                                                   @Param("from") OffsetDateTime from,
                                                   @Param("to") OffsetDateTime to,
                                                   Pageable pageable);

  // Teacher-focused projections (optionally filtered by student)
  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.teacherId = :teacherId group by a.id")
  Page<AssignmentListItemProjection> listItemsBaseForTeacher(@Param("teacherId") UUID teacherId, Pageable pageable);

  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.teacherId = :teacherId and a.createdAt between :from and :to group by a.id")
  Page<AssignmentListItemProjection> listItemsWithinForTeacher(@Param("teacherId") UUID teacherId,
                                                              @Param("from") OffsetDateTime from,
                                                              @Param("to") OffsetDateTime to,
                                                              Pageable pageable);

  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.teacherId = :teacherId and a.studentId = :studentId group by a.id")
  Page<AssignmentListItemProjection> listItemsBaseForTeacherAndStudent(@Param("teacherId") UUID teacherId,
                                                                      @Param("studentId") UUID studentId,
                                                                      Pageable pageable);

  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.teacherId = :teacherId and a.studentId = :studentId and a.createdAt between :from and :to group by a.id")
  Page<AssignmentListItemProjection> listItemsWithinForTeacherAndStudent(@Param("teacherId") UUID teacherId,
                                                                        @Param("studentId") UUID studentId,
                                                                        @Param("from") OffsetDateTime from,
                                                                        @Param("to") OffsetDateTime to,
                                                                        Pageable pageable);
}