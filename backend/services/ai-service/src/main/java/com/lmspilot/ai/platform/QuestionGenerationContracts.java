package com.lmspilot.ai.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

public final class QuestionGenerationContracts {
    private QuestionGenerationContracts() {}

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

    public record SourceChunk(UUID documentVersionId, Integer page, String section, String text) {}
    public record ValidationProblem(String path, String message) {}
    public record QuestionSetValidationResult(boolean valid, List<ValidationProblem> problems) {
        public QuestionSetValidationResult {
            problems = problems == null ? List.of() : List.copyOf(problems);
        }
    }
}
