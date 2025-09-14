package com.speakshire.homeworkservice.repository;

import com.speakshire.homeworkservice.domain.HomeworkTaskVocabWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HomeworkTaskVocabWordRepository extends JpaRepository<HomeworkTaskVocabWord, UUID> {}