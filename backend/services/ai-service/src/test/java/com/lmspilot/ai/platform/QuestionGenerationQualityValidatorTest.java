package com.lmspilot.ai.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lmspilot.ai.platform.QuestionGenerationContracts.GenerateQuestionsCommand;
import com.lmspilot.ai.platform.QuestionGenerationContracts.QuestionSetValidationResult;
import com.lmspilot.ai.platform.QuestionGenerationContracts.SourceChunk;

import java.io.InputStream;

import java.util.List;

import java.util.Map;

import java.util.Set;

import java.util.UUID;

import org.apache.tika.Tika;

import org.junit.jupiter.api.Test;
class QuestionGenerationQualityValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID courseId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final String source = "Chuyển đổi số thành công cần kết hợp con người, quy trình và công nghệ. Dữ liệu phải được bảo vệ bằng phân quyền và mã hóa.";
    @Test
    void largestRemainderAllocationKeepsExactQuestionCount() {
        assertEquals(Map.of("EASY", 3, "MEDIUM", 5, "HARD", 2), DifficultyDistributionPolicy.expectedCounts(10, Map.of("EASY", 30, "MEDIUM", 50, "HARD", 20)));
        assertEquals(7, DifficultyDistributionPolicy.expectedCounts(7, Map.of("EASY", 30, "MEDIUM", 50, "HARD", 20)).values().stream().mapToInt(Integer::intValue).sum());
    }
    @Test
    void tikaExtractsVietnameseTextFromPdfAndDocxFixtures() throws Exception {
        Tika tika = new Tika();
        for (String resource : List.of("fixtures/an-toan-thong-tin.pdf", "fixtures/an-toan-thong-tin.docx")) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                if (stream == null) throw new AssertionError("Missing " + resource);
                String extracted = tika.parseToString(stream);
                assertTrue(extracted.contains("Xác thực đa yếu tố"), "Không trích xuất được nội dung từ " + resource);
                assertTrue(extracted.contains("đặc quyền tối thiểu"), "Thiếu đoạn quan trọng trong " + resource);
            }

        }

    }
    @Test
    void validGeneratedSetMatchesSourceAndDifficulty() {
        JsonNode root = root(List.of(
        question("q1", "EASY", "Yếu tố nào cần kết hợp trong chuyển đổi số?", "con người, quy trình và công nghệ", source.substring(0, source.indexOf(". "))),
        question("q2", "MEDIUM", "Biện pháp nào bảo vệ dữ liệu?", "phân quyền và mã hóa", "Dữ liệu phải được bảo vệ bằng phân quyền và mã hóa."),
        question("q3", "MEDIUM", "Nhận định nào đúng về bảo vệ dữ liệu?", "Cần phân quyền và mã hóa", "Dữ liệu phải được bảo vệ bằng phân quyền và mã hóa.")
        ));
        QuestionSetValidationResult result = GeneratedQuestionQualityValidator.validate(
        root,
        new GenerateQuestionsCommand(courseId, Set.of(documentId), "vi", 3, Map.of("EASY", 34, "MEDIUM", 66, "HARD", 0), Set.of("SINGLE_CHOICE")),
        List.of(new SourceChunk(documentId, 1, "Trang 1", source))
        );
        assertTrue(result.valid(), result.problems().toString());
    }
    @Test
    void invalidCitationAndDifficultyAreRejected() {
        JsonNode root = root(List.of(question("q1", "HARD", "Yếu tố nào cần kết hợp trong chuyển đổi số?", "con người", "Đoạn này không có trong tài liệu")));
        QuestionSetValidationResult result = GeneratedQuestionQualityValidator.validate(
        root,
        new GenerateQuestionsCommand(courseId, Set.of(documentId), "vi", 1, Map.of("EASY", 100, "MEDIUM", 0, "HARD", 0), Set.of("SINGLE_CHOICE")),
        List.of(new SourceChunk(documentId, 1, "Trang 1", source))
        );
        assertFalse(result.valid());
        assertTrue(result.problems().stream().anyMatch(problem -> problem.message().contains("Phân bố EASY")));
        assertTrue(result.problems().stream().anyMatch(problem -> problem.message().contains("không khớp nguyên văn")));
    }
    private Map<String, Object> question(String id, String difficulty, String stem, String correctText, String quote) {
        return Map.of(
        "externalId", id,
        "type", "SINGLE_CHOICE",
        "stem", stem,
        "difficulty", difficulty,
        "points", 1,
        "options", List.of(Map.of("id", "A", "text", correctText), Map.of("id", "B", "text", "Phương án nhiễu khác")),
        "correctOptionIds", List.of("A"),
        "explanation", "Đáp án A được nêu trực tiếp trong tài liệu nguồn.",
        "tags", List.of(),
        "citations", List.of(Map.of("documentVersionId", documentId.toString(), "page", 1, "section", "Trang 1", "quote", quote))
        );
    }
    private JsonNode root(List<Map<String, Object>> questions) {
        return mapper.valueToTree(Map.of(
        "schemaVersion", "1.0",
        "source", Map.of(
        "courseId", courseId.toString(),
        "documentVersionIds", List.of(documentId.toString()),
        "provider", "LOCAL_OPENAI_COMPATIBLE",
        "model", "test-model",
        "generatedAt", "2026-08-05T00:00:00Z"
        ),
        "language", "vi",
        "questions", questions
        ));
    }

}
