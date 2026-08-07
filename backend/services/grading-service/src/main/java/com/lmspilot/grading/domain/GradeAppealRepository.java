package com.lmspilot.grading.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;
public interface GradeAppealRepository extends JpaRepository<GradeAppealEntity,UUID>{
    List<GradeAppealEntity>findAllByUserIdOrderByCreatedAtDesc(UUID id);
    List<GradeAppealEntity>findAllByStatusInOrderByCreatedAtAsc(Collection<GradeAppealStatus>s);
    GradeAppealEntity findByGradeIdAndUserIdAndActiveKey(UUID g,UUID u,String k);
}
