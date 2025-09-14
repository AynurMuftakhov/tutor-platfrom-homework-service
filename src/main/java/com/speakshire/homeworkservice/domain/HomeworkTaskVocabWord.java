package com.speakshire.homeworkservice.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter @Setter
@Entity
@Table(
        name = "homework_task_vocab_words",
        uniqueConstraints = @UniqueConstraint(name = "uk_hw_task_word", columnNames = {"task_id","word_id"}),
        indexes = {
                @Index(name = "idx_hw_vocab_task", columnList = "task_id"),
                @Index(name = "idx_hw_vocab_word", columnList = "word_id")
        }
)
public class HomeworkTaskVocabWord {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  @EqualsAndHashCode.Exclude
  private HomeworkTask task;

  @Column(name = "word_id", nullable = false)
  private UUID wordId;

  @Column(nullable = false)
  private boolean learned = false;
}