package com.lmspilot.assessment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
public interface QuestionRepository extends JpaRepository<QuestionEntity,UUID> {
    List<QuestionEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
