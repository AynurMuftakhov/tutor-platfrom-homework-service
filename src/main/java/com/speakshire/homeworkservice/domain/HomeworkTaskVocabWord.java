package com.speakshire.homeworkservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@Getter @Setter
@Entity
@Table(name = "homework_task_vocab_words")
@IdClass(HomeworkTaskVocabWord.PK.class)
public class HomeworkTaskVocabWord {

  @Id
  @Column(name = "task_id", nullable = false)
  private UUID taskId;

  @Id
  @Column(name = "word_id", nullable = false)
  private UUID wordId;

  @Column(nullable = false)
  private boolean learned = false;

  @EqualsAndHashCode
  public static class PK implements Serializable {
    private UUID taskId;
    private UUID wordId;
  }
}