package com.lmspilot.competency.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;
public interface CompetencyRepository extends JpaRepository<CompetencyEntity,UUID>{
    boolean existsByCodeIgnoreCase(String code);
    List<CompetencyEntity> findAllByStatusOrderByCategoryAscNameAsc(CompetencyStatus status);
}
