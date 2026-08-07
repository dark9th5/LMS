package com.lmspilot.assessment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
public interface CompetitionRepository extends JpaRepository<CompetitionEntity,UUID> {
}
