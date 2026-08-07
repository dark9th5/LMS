package com.lmspilot.enrollment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface LearningPathRepository extends JpaRepository<LearningPathEntity,UUID>{
    boolean existsByCodeIgnoreCase(String code);
    List<LearningPathEntity> findAllByStatusNotOrderByUpdatedAtDesc(LearningPathStatus status);
}
