package com.lmspilot.ai.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.lmspilot.ai.cls.*
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.LicenseGuard
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class AiProviderRequest(
    @field:NotBlank @field:Size(max = 80) val code: String,
    val providerType: AiProviderType,
    @field:NotBlank @field:Size(max = 1000) val baseUrl: String,
    @field:NotBlank @field:Size(max = 240) val model: String,
    val enabled: Boolean = false,
    /** Null keeps the current key, empty removes it. */
    val apiKey: String? = null,
    @field:Min(5) @field:Max(3600) val requestTimeoutSeconds: Int = 120,
    @field:Min(1) val maxOutputTokens: Int? = null,
    val config: Map<String, Any?> = emptyMap(),
)

data class AiProviderResponse(
    val id: UUID,
    val code: String,
    val providerType: AiProviderType,
    val baseUrl: String,
    val model: String,
    val enabled: Boolean,
    val apiKeyConfigured: Boolean,
    val requestTimeoutSeconds: Int,
    val maxOutputTokens: Int?,
    val config: Map<String, Any?>,
    val updatedAt: Instant,
)

data class GenerateQuestionSetRequest(
    val courseId: UUID,
    val providerConfigId: UUID,
    @field:Size(max = 50) val documentFileIds: Set<UUID> = emptySet(),
    @field:Size(max = 120000) val sourceText: String? = null,
    val language: String = "vi",
    @field:Min(1) @field:Max(100) val numberOfQuestions: Int = 10,
    val questionTypes: Set<String> = setOf("SINGLE_CHOICE", "TRUE_FALSE"),
    val difficultyDistribution: Map<String, Int> = emptyMap(),
) {
    @AssertTrue(message = "Cần ít nhất một tài liệu hoặc sourceText")
    fun hasSource() = documentFileIds.isNotEmpty() || !sourceText.isNullOrBlank()
}

data class ReviewQuestionSetRequest(val decision: ReviewDecision, @field:Size(max = 5000) val comments: String? = null)

data class QuestionGenerationJobResponse(
    val id: UUID,
    val courseId: UUID,
    val requestedBy: UUID,
    val providerConfigId: UUID,
    val documentFileIds: Set<UUID>,
    val options: Map<String, Any?>,
    val status: QuestionGenerationStatus,
    val questionSet: JsonNode?,
    val validationProblems: List<ValidationProblem>,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant?,
)

@Service
class AiSecretCipher(@Value("\${ai.secret-key}") secret: String) {
    private val random = SecureRandom()
    private val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray()), "AES")
    fun encrypt(value: String): ByteArray {
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        return nonce + cipher.doFinal(value.toByteArray())
    }
    fun decrypt(value: ByteArray): String {
        val nonce = value.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        return cipher.doFinal(value.copyOfRange(12, value.size)).toString(Charsets.UTF_8)
    }
}

@Service
class AiProviderConfigurationService(
    private val providers: AiProviderConfigRepository,
    private val mapper: ObjectMapper,
    private val cipher: AiSecretCipher,
    private val license: LicenseGuard,
) {
    @Transactional(readOnly = true)
    fun list() = providers.findAllByOrderByCodeAsc().map { it.response(mapper) }

    @Transactional
    fun save(id: UUID?, input: AiProviderRequest): AiProviderResponse {
        license.requireFeature("AI")
        val entity = when {
            id != null -> providers.findById(id).orElseThrow { notFound() }
            else -> providers.findByCodeIgnoreCase(input.code) ?: AiProviderConfigEntity()
        }
        entity.code = input.code.trim().uppercase()
        entity.providerType = input.providerType
        entity.baseUrl = input.baseUrl.trim().trimEnd('/')
        entity.model = input.model.trim()
        entity.enabled = input.enabled
        entity.requestTimeoutSeconds = input.requestTimeoutSeconds
        entity.maxOutputTokens = input.maxOutputTokens
        entity.configJson = mapper.writeValueAsString(input.config)
        if (input.apiKey != null) {
            entity.encryptedApiKey = input.apiKey.takeIf { it.isNotEmpty() }?.let(cipher::encrypt)
            entity.secretKeyVersion = if (entity.encryptedApiKey == null) null else 1
        }
        entity.updatedBy = CurrentUser.id()
        entity.updatedAt = Instant.now()
        return providers.save(entity).response(mapper)
    }

    fun apiKey(entity: AiProviderConfigEntity): String? = entity.encryptedApiKey?.let(cipher::decrypt)
    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "AI_PROVIDER_NOT_FOUND", "Không tìm thấy cấu hình AI")
}

private fun AiProviderConfigEntity.response(mapper: ObjectMapper) = AiProviderResponse(
    id, code, providerType, baseUrl, model, enabled, encryptedApiKey != null, requestTimeoutSeconds,
    maxOutputTokens, mapper.readValue(configJson, object : TypeReference<Map<String, Any?>>() {}), updatedAt,
)

@Service
class AiFileDocumentClient(
    builder: RestClient.Builder,
    @Value("\${file-storage-service.url:http://localhost:8089}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val token: String,
) {
    private val client = builder.baseUrl(baseUrl).build()
    fun content(fileId: UUID): ByteArray = client.get().uri("/internal/v1/files/{id}/content", fileId)
        .header("X-Service-Token", token).retrieve().body(ByteArray::class.java)
        ?: throw ApiException(HttpStatus.BAD_GATEWAY, "DOCUMENT_EMPTY", "Tài liệu không có nội dung")
}

@Service
class AssessmentQuestionImportClient(
    builder: RestClient.Builder,
    @Value("\${assessment-service.url:http://localhost:8086}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val token: String,
) {
    private val client = builder.baseUrl(baseUrl).build()
    fun import(ownerId: UUID, courseId: UUID, questionSet: JsonNode): List<UUID> {
        val response = client.post().uri("/internal/v1/questions/import-generated")
            .header("X-Service-Token", token)
            .body(mapOf("ownerId" to ownerId, "courseId" to courseId, "questionSet" to questionSet))
            .retrieve().body(Array<String>::class.java) ?: emptyArray()
        return response.map(UUID::fromString)
    }
}

@Service
class QuestionGenerationService(
    private val jobs: QuestionGenerationJobRepository,
    private val reviews: QuestionGenerationReviewRepository,
    private val providers: AiProviderConfigRepository,
    private val providerService: AiProviderConfigurationService,
    private val files: AiFileDocumentClient,
    private val importer: AssessmentQuestionImportClient,
    private val mapper: ObjectMapper,
    private val scopedAuthorization: ScopedAuthorizationClient,
    private val license: LicenseGuard,
) {
    private val tika = Tika()

    @Transactional(readOnly = true)
    fun list(): List<QuestionGenerationJobResponse> {
        val canReview = Permissions.QUESTIONS_APPROVE_AI in CurrentUser.authorities()
        val source = if (CurrentUser.isSystemAdmin() || canReview) {
            jobs.findAllByOrderByCreatedAtDesc().filter { job ->
                job.requestedBy == CurrentUser.id() || canAccessCourse(job.courseId, if (canReview) Permissions.QUESTIONS_APPROVE_AI else Permissions.QUESTIONS_GENERATE_AI)
            }
        } else jobs.findAllByRequestedByOrderByCreatedAtDesc(CurrentUser.id())
        return source.map { it.response(mapper) }
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): QuestionGenerationJobResponse = requireAccessible(id).response(mapper)

    fun generate(input: GenerateQuestionSetRequest): QuestionGenerationJobResponse {
        license.requireFeature("AI")
        requireCoursePermission(input.courseId, Permissions.QUESTIONS_GENERATE_AI, "Khóa học ngoài phạm vi sinh câu hỏi")
        val provider = providers.findById(input.providerConfigId).orElseThrow {
            ApiException(HttpStatus.BAD_REQUEST, "AI_PROVIDER_NOT_FOUND", "Không tìm thấy cấu hình AI")
        }
        if (!provider.enabled) throw ApiException(HttpStatus.CONFLICT, "AI_PROVIDER_DISABLED", "Cấu hình AI chưa được bật")
        val options = mapOf(
            "language" to input.language,
            "numberOfQuestions" to input.numberOfQuestions,
            "questionTypes" to input.questionTypes,
            "difficultyDistribution" to input.difficultyDistribution,
        )
        val job = jobs.save(
            QuestionGenerationJobEntity(
                courseId = input.courseId,
                requestedBy = CurrentUser.id(),
                providerConfigId = provider.id,
                documentVersionIdsJson = mapper.writeValueAsString(input.documentFileIds),
                generationOptionsJson = mapper.writeValueAsString(options),
            )
        )
        return runJob(job, provider, input)
    }

    @Transactional
    fun review(id: UUID, input: ReviewQuestionSetRequest): QuestionGenerationJobResponse {
        license.requireFeature("AI")
        val job = requireAccessible(id, reviewer = true)
        if (job.status !in setOf(QuestionGenerationStatus.REVIEW_REQUIRED, QuestionGenerationStatus.APPROVED)) {
            throw ApiException(HttpStatus.CONFLICT, "JOB_NOT_REVIEWABLE", "Bộ câu hỏi chưa ở trạng thái chờ duyệt")
        }
        reviews.save(QuestionGenerationReviewEntity(jobId = id, reviewerId = CurrentUser.id(), decision = input.decision, comments = input.comments))
        job.status = when (input.decision) {
            ReviewDecision.APPROVE -> QuestionGenerationStatus.APPROVED
            ReviewDecision.REJECT, ReviewDecision.REQUEST_CHANGES -> QuestionGenerationStatus.REVIEW_REQUIRED
        }
        job.updatedAt = Instant.now()
        return job.response(mapper)
    }

    @Transactional
    fun import(id: UUID): Map<String, Any> {
        val job = requireAccessible(id, reviewer = true)
        if (job.status != QuestionGenerationStatus.APPROVED) throw ApiException(HttpStatus.CONFLICT, "JOB_NOT_APPROVED", "Bộ câu hỏi phải được duyệt trước khi nhập")
        val questionSet = job.questionSetJson?.let { mapper.readTree(it) }
            ?: throw ApiException(HttpStatus.CONFLICT, "QUESTION_SET_EMPTY", "Bộ câu hỏi không có dữ liệu")
        val importedIds = importer.import(job.requestedBy, job.courseId, questionSet)
        job.status = QuestionGenerationStatus.IMPORTED
        job.updatedAt = Instant.now()
        job.completedAt = Instant.now()
        return mapOf("jobId" to id, "importedQuestionIds" to importedIds)
    }

    private fun runJob(job: QuestionGenerationJobEntity, provider: AiProviderConfigEntity, input: GenerateQuestionSetRequest): QuestionGenerationJobResponse {
        return try {
            job.status = QuestionGenerationStatus.EXTRACTING
            job.updatedAt = Instant.now()
            jobs.save(job)
            val chunks = mutableListOf<SourceChunk>()
            input.sourceText?.takeIf { it.isNotBlank() }?.let {
                chunks += SourceChunk(UUID(0, 0), null, "Nội dung nhập trực tiếp", it.trim())
            }
            input.documentFileIds.forEach { fileId ->
                val text = runCatching { tika.parseToString(files.content(fileId).inputStream()) }
                    .getOrElse { throw ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_EXTRACTION_FAILED", "Không đọc được tài liệu $fileId") }
                    .take(250_000)
                chunks += SourceChunk(fileId, null, "Tài liệu $fileId", text)
            }
            if (chunks.none { it.text.isNotBlank() }) throw ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_TEXT_EMPTY", "Không trích xuất được nội dung từ tài liệu")

            job.status = QuestionGenerationStatus.GENERATING
            job.updatedAt = Instant.now()
            jobs.save(job)
            val generated = callProvider(provider, input, chunks)
            val result = canonicalize(generated, provider, input, chunks)

            job.status = QuestionGenerationStatus.VALIDATING
            val validation = QuestionSetBusinessValidator.validate(result)
            job.questionSetJson = mapper.writeValueAsString(result)
            job.validationErrorsJson = mapper.writeValueAsString(validation.problems)
            job.status = if (validation.valid) QuestionGenerationStatus.REVIEW_REQUIRED else QuestionGenerationStatus.FAILED
            job.errorMessage = if (validation.valid) null else "Kết quả AI không đạt cấu trúc nghiệp vụ chung"
            job.updatedAt = Instant.now()
            job.completedAt = Instant.now()
            jobs.save(job).response(mapper)
        } catch (ex: Exception) {
            job.status = QuestionGenerationStatus.FAILED
            job.errorMessage = (ex as? ApiException)?.message ?: ex.message ?: "Không thể sinh bộ câu hỏi"
            job.updatedAt = Instant.now()
            job.completedAt = Instant.now()
            jobs.save(job).response(mapper)
        }
    }

    private fun callProvider(provider: AiProviderConfigEntity, input: GenerateQuestionSetRequest, chunks: List<SourceChunk>): JsonNode {
        val sourceIds = chunks.map(SourceChunk::documentVersionId).distinct().map(UUID::toString)
        val generatedAt = Instant.now()
        val system = """
            Bạn tạo câu hỏi cho LMS doanh nghiệp. Chỉ trả một JSON object, không markdown, không giải thích ngoài JSON.
            Không dùng kiến thức ngoài tài liệu. Mỗi câu phải có trích dẫn nguyên văn từ đúng DOCUMENT_VERSION_ID được cung cấp.
            Cấu trúc JSON bắt buộc theo schemaVersion 1.0:
            {"schemaVersion":"1.0","source":{"courseId":"${input.courseId}","documentVersionIds":${mapper.writeValueAsString(sourceIds)},"provider":"${provider.providerType}","model":"${provider.model}","generatedAt":"$generatedAt"},"language":"${input.language}","questions":[{"externalId":"q-1","type":"SINGLE_CHOICE","stem":"...","difficulty":"EASY","points":1,"options":[{"id":"A","text":"..."},{"id":"B","text":"..."}],"correctOptionIds":["A"],"explanation":"...","tags":[],"citations":[{"documentVersionId":"${sourceIds.firstOrNull() ?: UUID(0, 0)}","section":"...","quote":"..."}]}]}
            difficulty chỉ nhận EASY, MEDIUM hoặc HARD. type chỉ nhận SINGLE_CHOICE, MULTIPLE_CHOICE hoặc TRUE_FALSE.
        """.trimIndent()
        val source = chunks.joinToString("\n\n---\n\n") { chunk ->
            "DOCUMENT_VERSION_ID=${chunk.documentVersionId}\nSECTION=${chunk.section ?: ""}\n${chunk.text}"
        }
        val user = "Số câu: ${input.numberOfQuestions}; loại: ${input.questionTypes.joinToString()}; phân bố độ khó: ${input.difficultyDistribution}; ngôn ngữ: ${input.language}.\nTÀI LIỆU:\n${source.take(700_000)}"
        val request = mutableMapOf<String, Any?>(
            "model" to provider.model,
            "messages" to listOf(mapOf("role" to "system", "content" to system), mapOf("role" to "user", "content" to user)),
            "temperature" to 0.2,
            "response_format" to mapOf("type" to "json_object"),
        )
        provider.maxOutputTokens?.let { request["max_tokens"] = it }
        val apiKey = providerService.apiKey(provider)
        val response = RestClient.builder().baseUrl(provider.baseUrl).build().post().uri("/chat/completions")
            .headers { headers -> if (!apiKey.isNullOrBlank()) headers.setBearerAuth(apiKey) }
            .body(request).retrieve().body(JsonNode::class.java)
            ?: throw ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "Mô hình không trả kết quả")
        val content = response.path("choices").firstOrNull()?.path("message")?.path("content")?.asText()
            ?: throw ApiException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", "Không tìm thấy nội dung trả về")
        val normalized = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching { mapper.readTree(normalized) }.getOrElse {
            throw ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_INVALID_JSON", "Kết quả AI không phải JSON hợp lệ")
        }
    }

    private fun canonicalize(
        generated: JsonNode,
        provider: AiProviderConfigEntity,
        input: GenerateQuestionSetRequest,
        chunks: List<SourceChunk>,
    ): JsonNode {
        if (!generated.isObject) return generated
        val root = generated.deepCopy<ObjectNode>()
        root.put("schemaVersion", "1.0")
        root.put("language", input.language.trim().ifBlank { "vi" })
        val source = mapper.createObjectNode().apply {
            put("courseId", input.courseId.toString())
            putArray("documentVersionIds").also { array ->
                chunks.map(SourceChunk::documentVersionId).distinct().forEach { array.add(it.toString()) }
            }
            put("provider", provider.providerType.name)
            put("model", provider.model)
            put("generatedAt", Instant.now().toString())
        }
        root.set<ObjectNode>("source", source)
        root.remove(listOf("courseId", "generatedAt", "sourceDocumentVersions"))
        root.path("questions").takeIf(JsonNode::isArray)?.forEach { item ->
            if (item is ObjectNode) {
                if (!item.hasNonNull("stem") && item.hasNonNull("prompt")) item.set<JsonNode>("stem", item.path("prompt"))
                item.remove("prompt")
                val difficulty = item.path("difficulty")
                if (difficulty.isNumber) {
                    item.put("difficulty", when (difficulty.asInt()) {
                        1, 2 -> "EASY"
                        3 -> "MEDIUM"
                        else -> "HARD"
                    })
                } else if (difficulty.isTextual) {
                    item.put("difficulty", difficulty.asText().uppercase())
                }
            }
        }
        return root
    }

    private fun canAccessCourse(courseId: UUID, permission: String): Boolean =
        CurrentUser.isSystemAdmin() || scopedAuthorization.allowed(permission, "COURSE", courseId)

    private fun requireCoursePermission(courseId: UUID, permission: String, message: String) {
        if (!canAccessCourse(courseId, permission)) {
            throw ApiException(HttpStatus.FORBIDDEN, "AI_COURSE_OUT_OF_SCOPE", message)
        }
    }

    private fun requireAccessible(id: UUID, reviewer: Boolean = false): QuestionGenerationJobEntity {
        val job = jobs.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "GENERATION_JOB_NOT_FOUND", "Không tìm thấy tác vụ sinh câu hỏi") }
        if (CurrentUser.isSystemAdmin() || job.requestedBy == CurrentUser.id()) return job
        val permission = if (reviewer) Permissions.QUESTIONS_APPROVE_AI else Permissions.QUESTIONS_GENERATE_AI
        if (permission !in CurrentUser.authorities() || !canAccessCourse(job.courseId, permission)) {
            throw ApiException(HttpStatus.FORBIDDEN, "GENERATION_JOB_OUT_OF_SCOPE", "Tác vụ ngoài phạm vi")
        }
        return job
    }
}

private fun QuestionGenerationJobEntity.response(mapper: ObjectMapper) = QuestionGenerationJobResponse(
    id = id,
    courseId = courseId,
    requestedBy = requestedBy,
    providerConfigId = providerConfigId,
    documentFileIds = mapper.readValue(documentVersionIdsJson, object : TypeReference<Set<UUID>>() {}),
    options = mapper.readValue(generationOptionsJson, object : TypeReference<Map<String, Any?>>() {}),
    status = status,
    questionSet = questionSetJson?.let { mapper.readTree(it) },
    validationProblems = validationErrorsJson?.let { mapper.readValue(it, object : TypeReference<List<ValidationProblem>>() {}) }.orEmpty(),
    errorMessage = errorMessage,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)

@RestController
@RequestMapping("/api/v1/ai")
class QuestionGenerationController(
    private val providers: AiProviderConfigurationService,
    private val generation: QuestionGenerationService,
) {
    @GetMapping("/providers")
    @PreAuthorize("hasAnyAuthority('${Permissions.CONFIGURATION_MANAGE}','${Permissions.QUESTIONS_GENERATE_AI}')")
    fun providers() = providers.list()

    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.CONFIGURATION_MANAGE}')")
    fun createProvider(@Valid @RequestBody input: AiProviderRequest) = providers.save(null, input)

    @PutMapping("/providers/{id}")
    @PreAuthorize("hasAuthority('${Permissions.CONFIGURATION_MANAGE}')")
    fun updateProvider(@PathVariable id: UUID, @Valid @RequestBody input: AiProviderRequest) = providers.save(id, input)

    @GetMapping("/question-generation-jobs")
    @PreAuthorize("hasAnyAuthority('${Permissions.QUESTIONS_GENERATE_AI}','${Permissions.QUESTIONS_APPROVE_AI}')")
    fun jobs() = generation.list()

    @GetMapping("/question-generation-jobs/{id}")
    @PreAuthorize("hasAnyAuthority('${Permissions.QUESTIONS_GENERATE_AI}','${Permissions.QUESTIONS_APPROVE_AI}')")
    fun job(@PathVariable id: UUID) = generation.get(id)

    @PostMapping("/question-generation-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.QUESTIONS_GENERATE_AI}')")
    fun generate(@Valid @RequestBody input: GenerateQuestionSetRequest) = generation.generate(input)

    @PostMapping("/question-generation-jobs/{id}/review")
    @PreAuthorize("hasAuthority('${Permissions.QUESTIONS_APPROVE_AI}')")
    fun review(@PathVariable id: UUID, @Valid @RequestBody input: ReviewQuestionSetRequest) = generation.review(id, input)

    @PostMapping("/question-generation-jobs/{id}/import")
    @PreAuthorize("hasAuthority('${Permissions.QUESTIONS_APPROVE_AI}')")
    fun import(@PathVariable id: UUID) = generation.import(id)
}
