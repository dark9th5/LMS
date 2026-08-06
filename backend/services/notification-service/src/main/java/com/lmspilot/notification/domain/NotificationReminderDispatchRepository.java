package com.lmspilot.notification.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationReminderDispatchRepository extends JpaRepository<NotificationReminderDispatchEntity, UUID> {
    boolean existsByRuleIdAndEnrollmentIdAndDueAt(UUID ruleId, UUID enrollmentId, Instant dueAt);
}
