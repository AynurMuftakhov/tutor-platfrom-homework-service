package com.speakshire.homeworkservice.repository;

import com.speakshire.homeworkservice.domain.HomeworkTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HomeworkTaskRepository extends JpaRepository<HomeworkTask, UUID> {
}