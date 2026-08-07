package com.lmspilot.course.domain;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseCategoryRepository extends JpaRepository<CourseCategoryEntity,UUID> {
    boolean existsByCodeIgnoreCase(String code);
    List<CourseCategoryEntity> findAllByOrderBySortOrderAscNameAsc();
}
