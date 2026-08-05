package com.lmspilot.enrollment.domain; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LearningPathAssignmentRepository extends JpaRepository<LearningPathAssignmentEntity,UUID>{ List<LearningPathAssignmentEntity> findAllByPathIdOrderByAssignedAtDesc(UUID pathId); }
