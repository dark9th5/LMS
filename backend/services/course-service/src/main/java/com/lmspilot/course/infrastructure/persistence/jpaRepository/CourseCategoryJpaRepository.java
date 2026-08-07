package com.lmspilot.course.infrastructure.persistence.jpaRepository;

import com.lmspilot.course.infrastructure.persistence.entity.CourseCategoryEntity;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseCategoryJpaRepository extends JpaRepository<CourseCategoryEntity,UUID> {
    boolean existsByCodeIgnoreCase(String code);
    List<CourseCategoryEntity> findAllByOrderBySortOrderAscNameAsc();
}
