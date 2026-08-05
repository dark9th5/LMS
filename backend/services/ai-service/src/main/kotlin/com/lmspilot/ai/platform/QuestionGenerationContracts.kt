package com.lmspilot.ai.platform

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

enum class AiProviderType { LOCAL_OPENAI_COMPATIBLE, REMOTE_OPENAI_COMPATIBLE, CUSTOM_ADAPTER }

data class GenerateQuestionsCommand(
    val courseId: UUID,
    val documentVersionIds: Set<UUID>,
    val language: String,
    val numberOfQuestions: Int,
    val difficultyDistribution: Map<String, Int> = emptyMap(),
    val questionTypes: Set<String> = setOf("SINGLE_CHOICE"),
)

data class SourceChunk(
    val documentVersionId: UUID,
    val page: Int?,
    val section: String?,
    val text: String,
)

interface QuestionGenerationProvider {
    val type: AiProviderType
    fun generate(command: GenerateQuestionsCommand, chunks: List<SourceChunk>): JsonNode
}

data class ValidationProblem(val path: String, val message: String)

data class QuestionSetValidationResult(val valid: Boolean, val problems: List<ValidationProblem>)

/** Runtime schema-equivalent structural checks plus LMS business validation. */
object QuestionSetBusinessValidator {
    private val allowedQuestionTypes = setOf("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE")
    private val allowedDifficulties = setOf("EASY", "MEDIUM", "HARD")
    private val optionIdPattern = Regex("^[A-Z][A-Z0-9_]{0,15}$")
    private val rootFields = setOf("schemaVersion", "source", "language", "title", "questions")
    private val sourceFields = setOf("courseId", "documentVersionIds", "provider", "model", "generatedAt")
    private val questionFields = setOf("externalId", "type", "stem", "difficulty", "topic", "learningObjective", "points", "options", "correctOptionIds", "explanation", "citations", "tags")
    private val optionFields = setOf("id", "text")
    private val citationFields = setOf("documentVersionId", "page", "section", "quote")

    fun validate(root: JsonNode): QuestionSetValidationResult {
        val problems = mutableListOf<ValidationProblem>()
        if (!root.isObject) return QuestionSetValidationResult(false, listOf(ValidationProblem("/", "Bộ câu hỏi phải là JSON object")))
        rejectUnknownFields(root, rootFields, "/", problems)
        if (root.path("schemaVersion").asText() != "1.0") problems += ValidationProblem("/schemaVersion", "schemaVersion phải là 1.0")

        val source = root.path("source")
        if (!source.isObject) problems += ValidationProblem("/source", "Thiếu thông tin nguồn")
        else rejectUnknownFields(source, sourceFields, "/source", problems)
        val courseId = source.path("courseId").asText()
        if (runCatching { UUID.fromString(courseId) }.isFailure) problems += ValidationProblem("/source/courseId", "courseId không phải UUID hợp lệ")
        val documentIds = source.path("documentVersionIds")
        val allowedDocuments = mutableSetOf<String>()
        if (!documentIds.isArray || documentIds.isEmpty) {
            problems += ValidationProblem("/source/documentVersionIds", "Phải có ít nhất một phiên bản tài liệu nguồn")
        } else {
            documentIds.forEachIndexed { index, node ->
                val value = node.asText()
                if (runCatching { UUID.fromString(value) }.isFailure) problems += ValidationProblem("/source/documentVersionIds/$index", "documentVersionId không hợp lệ")
                if (!allowedDocuments.add(value)) problems += ValidationProblem("/source/documentVersionIds/$index", "documentVersionId bị trùng")
            }
        }
        if (root.path("language").asText().length !in 2..16) problems += ValidationProblem("/language", "Mã ngôn ngữ phải dài từ 2 đến 16 ký tự")

        val questions = root.path("questions")
        if (!questions.isArray || questions.isEmpty) {
            problems += ValidationProblem("/questions", "Bộ câu hỏi phải có ít nhất một câu")
            return QuestionSetValidationResult(false, problems)
        }
        if (questions.size() > 500) problems += ValidationProblem("/questions", "Bộ câu hỏi không được vượt quá 500 câu")

        val externalIds = mutableSetOf<String>()
        questions.forEachIndexed { index, question ->
            val base = "/questions/$index"
            if (!question.isObject) {
                problems += ValidationProblem(base, "Câu hỏi phải là JSON object")
                return@forEachIndexed
            }
            rejectUnknownFields(question, questionFields, base, problems)
            val externalId = question.path("externalId").asText().trim()
            if (externalId.isEmpty() || externalId.length > 100) problems += ValidationProblem("$base/externalId", "externalId không hợp lệ")
            else if (!externalIds.add(externalId)) problems += ValidationProblem("$base/externalId", "externalId bị trùng")

            val type = question.path("type").asText()
            if (type !in allowedQuestionTypes) problems += ValidationProblem("$base/type", "Loại câu hỏi không được hỗ trợ")
            val stem = question.path("stem").asText().trim()
            if (stem.length !in 5..4000) problems += ValidationProblem("$base/stem", "Nội dung câu hỏi phải dài từ 5 đến 4000 ký tự")
            if (question.path("difficulty").asText() !in allowedDifficulties) problems += ValidationProblem("$base/difficulty", "Độ khó phải là EASY, MEDIUM hoặc HARD")
            if (!question.path("points").isNumber || question.path("points").asDouble() <= 0 || question.path("points").asDouble() > 1000) {
                problems += ValidationProblem("$base/points", "Điểm phải lớn hơn 0 và không vượt quá 1000")
            }

            val optionsNode = question.path("options")
            val optionIds = mutableSetOf<String>()
            if (!optionsNode.isArray || optionsNode.size() !in 2..10) {
                problems += ValidationProblem("$base/options", "Câu hỏi phải có từ 2 đến 10 phương án")
            } else {
                optionsNode.forEachIndexed { optionIndex, option ->
                    rejectUnknownFields(option, optionFields, "$base/options/$optionIndex", problems)
                    val id = option.path("id").asText()
                    if (!optionIdPattern.matches(id)) problems += ValidationProblem("$base/options/$optionIndex/id", "Mã phương án không hợp lệ")
                    else if (!optionIds.add(id)) problems += ValidationProblem("$base/options/$optionIndex/id", "Mã phương án bị trùng")
                    if (option.path("text").asText().trim().isEmpty()) problems += ValidationProblem("$base/options/$optionIndex/text", "Nội dung phương án không được trống")
                }
            }

            val correctNode = question.path("correctOptionIds")
            val correct = if (correctNode.isArray) correctNode.map { it.asText() }.toSet() else emptySet()
            if (correct.isEmpty()) problems += ValidationProblem("$base/correctOptionIds", "Phải có ít nhất một đáp án đúng")
            if (!optionIds.containsAll(correct)) problems += ValidationProblem("$base/correctOptionIds", "Đáp án đúng không tồn tại trong options")
            if (type in setOf("SINGLE_CHOICE", "TRUE_FALSE") && correct.size != 1) {
                problems += ValidationProblem("$base/correctOptionIds", "Loại câu hỏi này phải có đúng một đáp án")
            }
            if (type == "TRUE_FALSE" && optionsNode.isArray && optionsNode.size() != 2) {
                problems += ValidationProblem("$base/options", "Câu đúng/sai phải có đúng hai phương án")
            }
            if (question.path("explanation").asText().trim().isEmpty()) problems += ValidationProblem("$base/explanation", "Phải có giải thích đáp án")

            val citations = question.path("citations")
            if (!citations.isArray || citations.isEmpty) {
                problems += ValidationProblem("$base/citations", "Phải có trích dẫn nguồn")
            } else {
                citations.forEachIndexed { citationIndex, citation ->
                    val citationBase = "$base/citations/$citationIndex"
                    rejectUnknownFields(citation, citationFields, citationBase, problems)
                    val documentId = citation.path("documentVersionId").asText()
                    if (documentId !in allowedDocuments) problems += ValidationProblem("$citationBase/documentVersionId", "Trích dẫn không thuộc tài liệu nguồn")
                    if (citation.path("quote").asText().trim().isEmpty()) problems += ValidationProblem("$citationBase/quote", "Trích dẫn phải có nội dung nguyên văn")
                    if (citation.has("page") && !citation.path("page").isNull && citation.path("page").asInt() < 1) problems += ValidationProblem("$citationBase/page", "Số trang phải từ 1")
                }
            }
        }
        return QuestionSetValidationResult(problems.isEmpty(), problems)
    }

    private fun rejectUnknownFields(node: JsonNode, allowed: Set<String>, path: String, problems: MutableList<ValidationProblem>) {
        if (!node.isObject) return
        node.fieldNames().forEachRemaining { field ->
            if (field !in allowed) problems += ValidationProblem(if (path == "/") "/$field" else "$path/$field", "Trường không thuộc JSON Schema chung")
        }
    }
}


object DifficultyDistributionPolicy {
    val difficulties = listOf("EASY", "MEDIUM", "HARD")
    private val defaultDistribution = linkedMapOf("EASY" to 30, "MEDIUM" to 50, "HARD" to 20)

    fun normalize(input: Map<String, Int>): Map<String, Int> {
        if (input.isEmpty()) return defaultDistribution
        val normalized = input.entries.associate { it.key.trim().uppercase() to it.value }
        require(normalized.keys.all { it in difficulties }) { "Độ khó chỉ nhận EASY, MEDIUM hoặc HARD" }
        require(normalized.values.all { it in 0..100 }) { "Tỷ lệ độ khó phải nằm trong khoảng 0 đến 100" }
        require(difficulties.sumOf { normalized[it] ?: 0 } == 100) { "Tổng tỷ lệ độ khó phải bằng 100%" }
        require(normalized.values.any { it > 0 }) { "Cần chọn ít nhất một mức độ khó" }
        return difficulties.associateWith { normalized[it] ?: 0 }
    }

    fun expectedCounts(total: Int, distribution: Map<String, Int>): Map<String, Int> {
        val normalized = normalize(distribution)
        val exact = difficulties.associateWith { total.coerceAtLeast(0) * (normalized[it] ?: 0) / 100.0 }
        val result = difficulties.associateWith { kotlin.math.floor(exact[it] ?: 0.0).toInt() }.toMutableMap()
        var remaining = total.coerceAtLeast(0) - result.values.sum()
        difficulties
            .sortedWith(compareByDescending<String> { (exact[it] ?: 0.0) - kotlin.math.floor(exact[it] ?: 0.0) }.thenBy { difficulties.indexOf(it) })
            .forEach { key ->
                if (remaining > 0) {
                    result[key] = (result[key] ?: 0) + 1
                    remaining -= 1
                }
            }
        return difficulties.associateWith { result[it] ?: 0 }
    }
}

/** Validates the generated set against the exact user request and its extracted source text. */
object GeneratedQuestionQualityValidator {
    fun validate(root: JsonNode, command: GenerateQuestionsCommand, chunks: List<SourceChunk>): QuestionSetValidationResult {
        val structural = QuestionSetBusinessValidator.validate(root)
        val problems = structural.problems.toMutableList()
        val questions = root.path("questions")
        if (!questions.isArray) return QuestionSetValidationResult(false, problems)

        if (questions.size() != command.numberOfQuestions) {
            problems += ValidationProblem("/questions", "AI phải tạo đúng ${command.numberOfQuestions} câu, hiện nhận ${questions.size()} câu")
        }
        val expected = DifficultyDistributionPolicy.expectedCounts(command.numberOfQuestions, command.difficultyDistribution)
        val actual = DifficultyDistributionPolicy.difficulties.associateWith { difficulty ->
            questions.count { it.path("difficulty").asText() == difficulty }
        }
        DifficultyDistributionPolicy.difficulties.forEach { difficulty ->
            if (actual[difficulty] != expected[difficulty]) {
                problems += ValidationProblem(
                    "/questions/difficulty",
                    "Phân bố $difficulty phải có ${expected[difficulty]} câu, hiện có ${actual[difficulty]} câu",
                )
            }
        }

        val allowedTypes = command.questionTypes.map(String::uppercase).toSet()
        val stems = mutableSetOf<String>()
        val sourceByDocument = chunks.groupBy(SourceChunk::documentVersionId)
        questions.forEachIndexed { index, question ->
            val base = "/questions/$index"
            if (question.path("type").asText() !in allowedTypes) {
                problems += ValidationProblem("$base/type", "Loại câu hỏi không nằm trong lựa chọn của giảng viên")
            }
            val normalizedStem = normalize(question.path("stem").asText())
            if (normalizedStem.isNotEmpty() && !stems.add(normalizedStem)) {
                problems += ValidationProblem("$base/stem", "Câu hỏi bị trùng nội dung")
            }
            val optionTexts = question.path("options").map { normalize(it.path("text").asText()) }
            if (optionTexts.filter(String::isNotEmpty).distinct().size != optionTexts.filter(String::isNotEmpty).size) {
                problems += ValidationProblem("$base/options", "Các phương án trả lời bị trùng")
            }
            if (question.path("explanation").asText().trim().length < 12) {
                problems += ValidationProblem("$base/explanation", "Giải thích đáp án quá ngắn")
            }
            question.path("citations").takeIf(JsonNode::isArray)?.forEachIndexed { citationIndex, citation ->
                val documentId = runCatching { UUID.fromString(citation.path("documentVersionId").asText()) }.getOrNull()
                val quote = normalize(citation.path("quote").asText())
                val page = citation.path("page").takeIf { it.isInt }?.asInt()
                val candidates = documentId?.let(sourceByDocument::get).orEmpty().filter { page == null || it.page == null || it.page == page }
                if (quote.isNotEmpty() && candidates.none { normalize(it.text).contains(quote) }) {
                    problems += ValidationProblem("$base/citations/$citationIndex/quote", "Trích dẫn không khớp nguyên văn tài liệu nguồn")
                }
            }
        }
        return QuestionSetValidationResult(problems.isEmpty(), problems.distinct())
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(Regex("[\\s\\u00A0]+"), " ")
        .replace(Regex("[“”‘’]"), "\"")
        .trim()
}
