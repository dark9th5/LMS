package com.lmspilot.assessment.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentContextRepository extends JpaRepository<AssessmentContextEntity, UUID> {
    List<AssessmentContextEntity> findAllByAssessmentIdIn(Collection<UUID> assessmentIds);
}
