package com.lmspilot.notification.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity,UUID>{
    List<NewsArticleEntity> findAllByStatusOrderByPinnedDescPriorityDescPublishFromDesc(NewsStatus status);
}
