package com.lmspilot.competency.api;

import java.time.Instant;

import java.util.*;
public record CompetencyGapResponse(UUID userId,List<UUID> profileIds,double readinessPercent,List<GapRow> gaps,Instant assessedAt){
}
