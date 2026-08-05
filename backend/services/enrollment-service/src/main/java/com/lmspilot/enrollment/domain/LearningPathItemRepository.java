package com.lmspilot.enrollment.domain; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LearningPathItemRepository extends JpaRepository<LearningPathItemEntity,UUID>{ List<LearningPathItemEntity> findAllByPathIdOrderBySortOrderAsc(UUID pathId); void deleteAllByPathId(UUID pathId); }
