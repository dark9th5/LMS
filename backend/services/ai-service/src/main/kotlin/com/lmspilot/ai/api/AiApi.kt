package com.lmspilot.ai.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.LicenseGuard
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient

data class DraftQuestionsRequest(
    @field:NotBlank @field:Size(max = 120000) val sourceText: String,
    @field:Min(1) @field:Max(50) val count: Int = 10,
    val types: Set<String> = setOf("SINGLE_CHOICE", "TRUE_FALSE"),
    val language: String = "vi",
    val difficulty: String = "MIXED",
)
data class DraftQuestion(val type: String, val prompt: String, val options: List<String> = emptyList(), val correctAnswers: List<String> = emptyList(), val explanation: String? = null, val difficulty: Int = 1)
data class DraftQuestionsResponse(val model: String, val questions: List<DraftQuestion>, val requiresHumanReview: Boolean = true)
data class ChatMessage(val role: String, val content: String)
data class ChatRequest(val model: String, val messages: List<ChatMessage>, val temperature: Double = 0.2, val response_format: Map<String, String> = mapOf("type" to "json_object"))
@JsonIgnoreProperties(ignoreUnknown = true) data class ChatChoice(val message: ChatMessage)
@JsonIgnoreProperties(ignoreUnknown = true) data class ChatResponse(val choices: List<ChatChoice> = emptyList())
data class ModelResult(val questions: List<DraftQuestion> = emptyList())

@Service
class LocalAiService(
    private val mapper: ObjectMapper,
    @Value("\${ai.enabled:false}") private val enabled: Boolean,
    @Value("\${ai.base-url:http://host.docker.internal:11434/v1}") baseUrl: String,
    @Value("\${ai.model:qwen3:8b}") private val model: String,
    @Value("\${ai.api-key:local}") private val apiKey: String,
    private val license: LicenseGuard,
) {
    private val client = RestClient.builder().baseUrl(baseUrl).build()

    fun draft(input: DraftQuestionsRequest): DraftQuestionsResponse {
        license.requireFeature("AI")
        if (!enabled) throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_DISABLED", "AI local chưa được bật trong cấu hình hoặc license")
        val system = """Bạn là trợ lý tạo câu hỏi cho LMS doanh nghiệp. Chỉ trả JSON có cấu trúc {\"questions\":[{\"type\":\"SINGLE_CHOICE\",\"prompt\":\"...\",\"options\":[\"...\"],\"correctAnswers\":[\"...\"],\"explanation\":\"...\",\"difficulty\":1}]}. Không tự thêm kiến thức ngoài tài liệu. Mọi kết quả là bản nháp cần giảng viên duyệt."""
        val user = "Ngôn ngữ: ${input.language}; số lượng: ${input.count}; loại: ${input.types.joinToString()}; độ khó: ${input.difficulty}. Tài liệu:\n${input.sourceText}"
        val response = runCatching { client.post().uri("/chat/completions").header("Authorization", "Bearer $apiKey").body(ChatRequest(model, listOf(ChatMessage("system", system), ChatMessage("user", user)))).retrieve().body(ChatResponse::class.java) }.getOrElse { throw ApiException(HttpStatus.BAD_GATEWAY, "AI_UNAVAILABLE", "Không kết nối được mô hình AI local") }
        val content = response?.choices?.firstOrNull()?.message?.content ?: throw ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "Mô hình không trả kết quả")
        val parsed = runCatching { mapper.readValue(content, ModelResult::class.java) }.getOrElse { throw ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_INVALID_RESPONSE", "Kết quả AI không đúng cấu trúc JSON") }
        return DraftQuestionsResponse(model, parsed.questions.take(input.count))
    }

    fun status(): Map<String, Any> {
        val licensed = runCatching { license.requireFeature("AI", write = false); true }.getOrDefault(false)
        return mapOf("enabled" to (enabled && licensed), "configured" to enabled, "licensed" to licensed, "model" to model, "reviewRequired" to true)
    }
}

@RestController
@RequestMapping("/api/v1/ai")
class AiController(private val service: LocalAiService) {
    @GetMapping("/status") @PreAuthorize("hasAuthority('${Permissions.AI_USE}')") fun status() = service.status()
    @PostMapping("/question-drafts") @PreAuthorize("hasAuthority('${Permissions.AI_USE}')") fun draft(@Valid @RequestBody input: DraftQuestionsRequest) = service.draft(input)
}
