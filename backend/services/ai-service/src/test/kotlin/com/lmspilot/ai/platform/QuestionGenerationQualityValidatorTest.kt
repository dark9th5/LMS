package com.lmspilot.ai.platform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.apache.tika.Tika
import java.util.UUID

class QuestionGenerationQualityValidatorTest {
    private val mapper = ObjectMapper()
    private val courseId = UUID.randomUUID()
    private val documentId = UUID.randomUUID()
    private val source = "Chuyển đổi số thành công cần kết hợp con người, quy trình và công nghệ. Dữ liệu phải được bảo vệ bằng phân quyền và mã hóa."

    @Test
    fun `largest remainder allocation keeps exact question count`() {
        assertEquals(mapOf("EASY" to 3, "MEDIUM" to 5, "HARD" to 2), DifficultyDistributionPolicy.expectedCounts(10, mapOf("EASY" to 30, "MEDIUM" to 50, "HARD" to 20)))
        assertEquals(7, DifficultyDistributionPolicy.expectedCounts(7, mapOf("EASY" to 30, "MEDIUM" to 50, "HARD" to 20)).values.sum())
    }


    @Test
    fun `tika extracts Vietnamese text from pdf and docx fixtures`() {
        val tika = Tika()
        listOf("fixtures/an-toan-thong-tin.pdf", "fixtures/an-toan-thong-tin.docx").forEach { resource ->
            val stream = javaClass.classLoader.getResourceAsStream(resource) ?: error("Missing $resource")
            val extracted = stream.use { tika.parseToString(it) }
            assertTrue(extracted.contains("Xác thực đa yếu tố"), "Không trích xuất được nội dung từ $resource")
            assertTrue(extracted.contains("đặc quyền tối thiểu"), "Thiếu đoạn quan trọng trong $resource")
        }
    }
    @Test
    fun `valid generated set matches source and difficulty`() {
        val questions = listOf(
            question("q1", "EASY", "Yếu tố nào cần kết hợp trong chuyển đổi số?", "con người, quy trình và công nghệ", source.substringBefore(". ")),
            question("q2", "MEDIUM", "Biện pháp nào bảo vệ dữ liệu?", "phân quyền và mã hóa", "Dữ liệu phải được bảo vệ bằng phân quyền và mã hóa."),
            question("q3", "MEDIUM", "Nhận định nào đúng về bảo vệ dữ liệu?", "Cần phân quyền và mã hóa", "Dữ liệu phải được bảo vệ bằng phân quyền và mã hóa."),
        )
        val root = root(questions)
        val result = GeneratedQuestionQualityValidator.validate(
            root,
            GenerateQuestionsCommand(courseId, setOf(documentId), "vi", 3, mapOf("EASY" to 34, "MEDIUM" to 66, "HARD" to 0), setOf("SINGLE_CHOICE")),
            listOf(SourceChunk(documentId, 1, "Trang 1", source)),
        )
        assertTrue(result.valid, result.problems.joinToString { it.message })
    }

    @Test
    fun `invalid citation and difficulty are rejected`() {
        val root = root(listOf(question("q1", "HARD", "Yếu tố nào cần kết hợp trong chuyển đổi số?", "con người", "Đoạn này không có trong tài liệu")))
        val result = GeneratedQuestionQualityValidator.validate(
            root,
            GenerateQuestionsCommand(courseId, setOf(documentId), "vi", 1, mapOf("EASY" to 100, "MEDIUM" to 0, "HARD" to 0), setOf("SINGLE_CHOICE")),
            listOf(SourceChunk(documentId, 1, "Trang 1", source)),
        )
        assertFalse(result.valid)
        assertTrue(result.problems.any { it.message.contains("Phân bố EASY") })
        assertTrue(result.problems.any { it.message.contains("không khớp nguyên văn") })
    }

    private fun question(id: String, difficulty: String, stem: String, correctText: String, quote: String) = mapOf(
        "externalId" to id,
        "type" to "SINGLE_CHOICE",
        "stem" to stem,
        "difficulty" to difficulty,
        "points" to 1,
        "options" to listOf(mapOf("id" to "A", "text" to correctText), mapOf("id" to "B", "text" to "Phương án nhiễu khác")),
        "correctOptionIds" to listOf("A"),
        "explanation" to "Đáp án A được nêu trực tiếp trong tài liệu nguồn.",
        "tags" to emptyList<String>(),
        "citations" to listOf(mapOf("documentVersionId" to documentId.toString(), "page" to 1, "section" to "Trang 1", "quote" to quote)),
    )

    private fun root(questions: List<Map<String, Any>>) = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(mapOf(
        "schemaVersion" to "1.0",
        "source" to mapOf(
            "courseId" to courseId.toString(),
            "documentVersionIds" to listOf(documentId.toString()),
            "provider" to "LOCAL_OPENAI_COMPATIBLE",
            "model" to "test-model",
            "generatedAt" to "2026-08-05T00:00:00Z",
        ),
        "language" to "vi",
        "questions" to questions,
    ))
}
