package com.speakshire.homeworkservice.repository;

import com.speakshire.homeworkservice.domain.HomeworkAssignment;
import com.speakshire.homeworkservice.repository.projection.AssignmentListItemProjection;
import com.speakshire.homeworkservice.repository.projection.HomeworkDashboardItemProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
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
          "where a.studentId = :studentId group by a.id having sum(case when t.type = :type then 1 else 0 end) > 0")
  Page<AssignmentListItemProjection> listItemsBaseByType(@Param("studentId") UUID studentId,
                                                         @Param("type") com.speakshire.homeworkservice.domain.HomeworkTaskType type,
                                                         Pageable pageable);

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

  @Query("select a.id as id, a.title as title, a.studentId as studentId, a.createdAt as createdAt, a.dueAt as dueAt, " +
          "count(t.id) as totalTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks, " +
          "sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks, " +
          "coalesce(avg(t.progressPct),0) as progressPct " +
          "from HomeworkAssignment a left join a.tasks t " +
          "where a.studentId = :studentId and a.createdAt between :from and :to group by a.id having sum(case when t.type = :type then 1 else 0 end) > 0")
  Page<AssignmentListItemProjection> listItemsWithinByType(@Param("studentId") UUID studentId,
                                                   @Param("from") OffsetDateTime from,
                                                   @Param("to") OffsetDateTime to,
                                                   @Param("type") com.speakshire.homeworkservice.domain.HomeworkTaskType type,
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

  @Query("""
          select count(a)
          from HomeworkAssignment a
          where a.studentId = :studentId
            and a.dueAt is not null
            and a.dueAt >= :fromInclusive
            and a.dueAt <= :toInclusive
            and exists (
              select 1 from HomeworkTask t
              where t.assignment = a
                and t.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED
            )
          """)
  long countDashboardDueSoonForStudent(@Param("studentId") UUID studentId,
                                       @Param("fromInclusive") OffsetDateTime fromInclusive,
                                       @Param("toInclusive") OffsetDateTime toInclusive);

  @Query("""
          select count(a)
          from HomeworkAssignment a
          where a.studentId = :studentId
            and a.dueAt is not null
            and a.dueAt < :nowUtc
            and exists (
              select 1 from HomeworkTask t
              where t.assignment = a
                and t.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED
            )
          """)
  long countDashboardOverdueForStudent(@Param("studentId") UUID studentId,
                                       @Param("nowUtc") OffsetDateTime nowUtc);

  @Query("""
          select count(a)
          from HomeworkAssignment a
          where a.teacherId = :teacherId
            and a.dueAt is not null
            and a.dueAt >= :fromInclusive
            and a.dueAt <= :toInclusive
            and exists (
              select 1 from HomeworkTask t
              where t.assignment = a
                and t.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED
            )
          """)
  long countDashboardDueSoonForTutor(@Param("teacherId") UUID teacherId,
                                     @Param("fromInclusive") OffsetDateTime fromInclusive,
                                     @Param("toInclusive") OffsetDateTime toInclusive);

  @Query("""
          select count(a)
          from HomeworkAssignment a
          where a.teacherId = :teacherId
            and a.dueAt is not null
            and a.dueAt < :nowUtc
            and exists (
              select 1 from HomeworkTask t
              where t.assignment = a
                and t.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED
            )
          """)
  long countDashboardOverdueForTutor(@Param("teacherId") UUID teacherId,
                                     @Param("nowUtc") OffsetDateTime nowUtc);

  @Query("""
          select count(a)
          from HomeworkAssignment a
          where a.teacherId = :teacherId
            and exists (
              select 1 from HomeworkTask tAny
              where tAny.assignment = a
            )
            and not exists (
              select 1 from HomeworkTask tNotDone
              where tNotDone.assignment = a
                and tNotDone.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED
            )
          """)
  long countDashboardToReviewForTutor(@Param("teacherId") UUID teacherId);

  @Query("""
          select a.id as id,
                 a.title as title,
                 a.dueAt as dueAt,
                 a.lessonId as lessonId,
                 a.studentId as studentId,
                 a.teacherId as teacherId,
                 count(t.id) as totalTasks,
                 sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks,
                 sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks
          from HomeworkAssignment a
          join a.tasks t
          where a.studentId = :studentId
            and a.dueAt is not null
            and a.dueAt >= :fromInclusive
            and a.dueAt <= :toInclusive
          group by a.id
          having sum(case when t.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) > 0
          order by a.dueAt asc
          """)
  List<HomeworkDashboardItemProjection> findDashboardNextDueItemsForStudent(@Param("studentId") UUID studentId,
                                                                             @Param("fromInclusive") OffsetDateTime fromInclusive,
                                                                             @Param("toInclusive") OffsetDateTime toInclusive,
                                                                             Pageable pageable);

  @Query("""
          select a.id as id,
                 a.title as title,
                 a.dueAt as dueAt,
                 a.lessonId as lessonId,
                 a.studentId as studentId,
                 a.teacherId as teacherId,
                 count(t.id) as totalTasks,
                 sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) as completedTasks,
                 sum(case when t.status = com.speakshire.homeworkservice.domain.HomeworkTaskStatus.IN_PROGRESS then 1 else 0 end) as inProgressTasks
          from HomeworkAssignment a
          join a.tasks t
          where a.teacherId = :teacherId
            and a.dueAt is not null
            and a.dueAt >= :fromInclusive
            and a.dueAt <= :toInclusive
          group by a.id
          having sum(case when t.status <> com.speakshire.homeworkservice.domain.HomeworkTaskStatus.COMPLETED then 1 else 0 end) > 0
          order by a.dueAt asc
          """)
  List<HomeworkDashboardItemProjection> findDashboardNextDueItemsForTutor(@Param("teacherId") UUID teacherId,
                                                                           @Param("fromInclusive") OffsetDateTime fromInclusive,
                                                                           @Param("toInclusive") OffsetDateTime toInclusive,
                                                                           Pageable pageable);
}
