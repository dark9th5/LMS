package com.lmspilot.learning.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface XapiStatementRepository extends JpaRepository<XapiStatementEntity,UUID>{
    List<XapiStatementEntity> findTop200ByActorUserIdOrderByOccurredAtDesc(UUID userId);
    List<XapiStatementEntity> findTop500ByCourseIdOrderByOccurredAtDesc(UUID courseId);
}
