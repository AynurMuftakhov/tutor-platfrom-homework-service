package com.speakshire.homeworkservice.repository;

import com.speakshire.homeworkservice.domain.HomeworkTask;
import com.speakshire.homeworkservice.repository.projection.TaskBriefProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface HomeworkTaskRepository extends JpaRepository<HomeworkTask, UUID> {
    @Query("select t.assignment.id as assignmentId, t.type as type, t.title as title, t.sourceKind as sourceKind " +
            "from HomeworkTask t where t.assignment.id in :assignmentIds order by t.assignment.id, t.ordinal asc")
    java.util.List<TaskBriefProjection> findTaskBriefsByAssignmentIds(@Param("assignmentIds") Collection<UUID> assignmentIds);
}