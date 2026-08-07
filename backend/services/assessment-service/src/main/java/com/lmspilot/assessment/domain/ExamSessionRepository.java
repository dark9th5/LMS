package com.lmspilot.assessment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
public interface ExamSessionRepository extends JpaRepository<ExamSessionEntity,UUID> {
    long countByExamIdAndUserId(UUID examId,UUID userId);
    Optional<ExamSessionEntity> findBySubmitIdempotencyKey(String key);
    List<ExamSessionEntity> findAllByExamIdAndUserIdOrderByAttemptNoAsc(UUID examId,UUID userId);
    List<ExamSessionEntity> findAllByExamIdAndEnrollmentIdOrderByAttemptNoAsc(UUID examId,UUID enrollmentId);
    List<ExamSessionEntity> findAllByExamIdAndEnrollmentIdAndUserIdOrderByAttemptNoAsc(UUID examId,UUID enrollmentId,UUID userId);
    List<ExamSessionEntity> findAllByExamIdAndUserIdAndEnrollmentIdIsNullOrderByAttemptNoAsc(UUID examId,UUID userId);
    boolean existsByExamId(UUID examId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExamSessionEntity s where s.id=:id") Optional<ExamSessionEntity> findForUpdateById(@Param("id") UUID id);
}
