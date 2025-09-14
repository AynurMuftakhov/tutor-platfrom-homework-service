package com.speakshire.homeworkservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "homework_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_assignment_idem",
                columnNames = {"teacher_id","student_id","idempotency_key"}),
        indexes = {
                @Index(name = "idx_hw_assign_student", columnList = "student_id,due_at"),
                @Index(name = "idx_hw_assign_teacher", columnList = "teacher_id,created_at")
        }
)
public class HomeworkAssignment {

  @Id
  @UuidGenerator
  private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String instructions;

  @Column(name = "due_at")
  private OffsetDateTime dueAt;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "idempotency_key")
  private String idempotencyKey;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordinal ASC")
  @ToString.Exclude @EqualsAndHashCode.Exclude
  private List<HomeworkTask> tasks = new ArrayList<>();

  public void addTask(HomeworkTask t) {
    t.setAssignment(this);
    tasks.add(t);
  }
}
