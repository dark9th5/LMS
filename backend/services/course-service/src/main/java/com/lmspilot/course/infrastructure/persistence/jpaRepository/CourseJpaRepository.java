package com.lmspilot.course.infrastructure.persistence.jpaRepository;

import com.lmspilot.course.domain.enums.CourseStatus;
import com.lmspilot.course.infrastructure.persistence.entity.CourseEntity;
import java.util.*;

import org.springframework.data.domain.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;
public interface CourseJpaRepository extends JpaRepository<CourseEntity,UUID> {
    boolean existsByCodeIgnoreCase(String code);
    @Query("select c from CourseEntity c where (:query is null or lower(c.code) like lower(concat('%',:query,'%')) or lower(c.name) like lower(concat('%',:query,'%'))) and (:status is null or c.status=:status) and (:categoryId is null or c.categoryId=:categoryId)") Page<CourseEntity> search(@Param("query") String query,@Param("status") CourseStatus status,@Param("categoryId") UUID categoryId,Pageable pageable);
}
