package com.lmspilot.ai.platform;

import java.util.List;

public record QuestionSetValidationResult(boolean valid, List<ValidationProblem> problems) {
    public QuestionSetValidationResult {
        problems = problems == null ? List.of() : List.copyOf(problems);
    }
}
