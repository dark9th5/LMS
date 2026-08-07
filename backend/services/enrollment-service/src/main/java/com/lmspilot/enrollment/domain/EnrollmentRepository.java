package com.lmspilot.enrollment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity,UUID>{
    Optional<EnrollmentEntity> findByClassIdAndUserId(UUID classId,UUID userId);
    Optional<EnrollmentEntity> findByIdempotencyKey(String key);
    List<EnrollmentEntity> findAllByUserIdOrderByEnrolledAtDesc(UUID userId);
    List<EnrollmentEntity> findAllByCourseIdOrderByEnrolledAtDesc(UUID courseId);
}
