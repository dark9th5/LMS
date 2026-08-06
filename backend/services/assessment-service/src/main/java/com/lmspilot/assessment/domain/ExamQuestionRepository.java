package com.lmspilot.assessment.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestionEntity, UUID> {
    List<ExamQuestionEntity> findAllByExamIdOrderBySortOrderAsc(UUID examId);
    List<ExamQuestionEntity> findAllByExamIdInOrderByExamIdAscSortOrderAsc(Collection<UUID> examIds);
    long countByExamId(UUID examId);
    void deleteAllByExamId(UUID examId);
}
