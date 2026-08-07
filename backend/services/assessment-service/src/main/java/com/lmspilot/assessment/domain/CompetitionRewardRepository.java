package com.lmspilot.assessment.domain;

import java.util.*;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
public interface CompetitionRewardRepository extends JpaRepository<CompetitionRewardEntity,UUID> {
    List<CompetitionRewardEntity> findAllByCompetitionIdOrderByRankFromAsc(UUID id);
    void deleteAllByCompetitionId(UUID id);
}
