package com.speakshire.homeworkservice.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

@Getter @Setter
@Entity
@Table(name = "homework_tasks",
        indexes = {
                @Index(name = "idx_hw_tasks_assignment", columnList = "assignment_id"),
                @Index(name = "idx_hw_tasks_status", columnList = "assignment_id,status")
        }
)
public class HomeworkTask {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assignment_id", nullable = false)
  @ToString.Exclude @EqualsAndHashCode.Exclude
  private HomeworkAssignment assignment;

  @Column(nullable = false)
  private Integer ordinal;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HomeworkTaskType type;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String instructions;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_kind", nullable = false)
  private SourceKind sourceKind;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content_ref", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> contentRef = Map.of();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HomeworkTaskStatus status = HomeworkTaskStatus.NOT_STARTED;

  @Column(name = "progress_pct", nullable = false)
  private Integer progressPct = 0;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "meta", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> meta = Map.of();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // NEW: cascade vocab rows from task
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude @EqualsAndHashCode.Exclude
  private List<HomeworkTaskVocabWord> vocabWords = new ArrayList<>();

  public void addVocabWord(UUID wordId) {
    var row = new HomeworkTaskVocabWord();
    row.setTask(this);
    row.setWordId(wordId);
    row.setLearned(false);
    vocabWords.add(row);
  }
}