package com.lmspilot.ai.platform;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionGenerationReviewRepository extends JpaRepository<QuestionGenerationReviewEntity,UUID>{
    List<QuestionGenerationReviewEntity> findAllByJobIdOrderByCreatedAtAsc(UUID id);
}
