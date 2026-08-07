package com.lmspilot.notification.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<NotificationEntity,UUID>{
    List<NotificationEntity> findAllByUserIdAndChannelOrderByCreatedAtDesc(UUID userId,NotificationChannel channel);
    long countByUserIdAndChannelAndReadFalse(UUID userId,NotificationChannel channel);
}
