package com.lmspilot.ai.platform;

import java.util.*;

public record GenerateQuestionsCommand(
    UUID courseId,
    Set<UUID> documentVersionIds,
    String language,
    int numberOfQuestions,
    Map<String, Integer> difficultyDistribution,
    Set<String> questionTypes
) {
    public GenerateQuestionsCommand {
        documentVersionIds = documentVersionIds == null ? Set.of() : Set.copyOf(documentVersionIds);
        difficultyDistribution = difficultyDistribution == null ? Map.of() : Map.copyOf(difficultyDistribution);
        questionTypes = questionTypes == null ? Set.of("SINGLE_CHOICE") : Set.copyOf(questionTypes);
    }
}
