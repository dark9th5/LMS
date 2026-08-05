package com.lmspilot.ai.platform

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ai_provider_configs")
class AiProviderConfigEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 80) var code: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    var providerType: AiProviderType = AiProviderType.LOCAL_OPENAI_COMPATIBLE,
    @Column(name = "base_url", nullable = false, length = 1000) var baseUrl: String = "",
    @Column(nullable = false, length = 240) var model: String = "",
    @Column(nullable = false) var enabled: Boolean = false,
    @Column(name = "encrypted_api_key") var encryptedApiKey: ByteArray? = null,
    @Column(name = "secret_key_version") var secretKeyVersion: Int? = null,
    @Column(name = "request_timeout_seconds", nullable = false) var requestTimeoutSeconds: Int = 120,
    @Column(name = "max_output_tokens") var maxOutputTokens: Int? = null,
    @Column(name = "config_json", nullable = false, columnDefinition = "text") var configJson: String = "{}",
    @Column(name = "updated_by", nullable = false) var updatedBy: UUID = UUID.randomUUID(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
)

interface AiProviderConfigRepository : org.springframework.data.jpa.repository.JpaRepository<AiProviderConfigEntity, UUID> {
    fun findByCodeIgnoreCase(code: String): AiProviderConfigEntity?
    fun findAllByOrderByCodeAsc(): List<AiProviderConfigEntity>
}

enum class QuestionGenerationStatus { QUEUED, EXTRACTING, GENERATING, VALIDATING, REVIEW_REQUIRED, APPROVED, IMPORTED, FAILED }

@Entity
@Table(name = "question_generation_jobs")
class QuestionGenerationJobEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "course_id", nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(name = "requested_by", nullable = false) var requestedBy: UUID = UUID.randomUUID(),
    @Column(name = "provider_config_id", nullable = false) var providerConfigId: UUID = UUID.randomUUID(),
    @Column(name = "document_version_ids_json", nullable = false, columnDefinition = "text") var documentVersionIdsJson: String = "[]",
    @Column(name = "generation_options_json", nullable = false, columnDefinition = "text") var generationOptionsJson: String = "{}",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30) var status: QuestionGenerationStatus = QuestionGenerationStatus.QUEUED,
    @Column(name = "question_set_json", columnDefinition = "text") var questionSetJson: String? = null,
    @Column(name = "validation_errors_json", columnDefinition = "text") var validationErrorsJson: String? = null,
    @Column(name = "error_message", length = 2000) var errorMessage: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Column(name = "completed_at") var completedAt: Instant? = null,
)

interface QuestionGenerationJobRepository : org.springframework.data.jpa.repository.JpaRepository<QuestionGenerationJobEntity, UUID> {
    fun findAllByRequestedByOrderByCreatedAtDesc(requestedBy: UUID): List<QuestionGenerationJobEntity>
    fun findAllByOrderByCreatedAtDesc(): List<QuestionGenerationJobEntity>
}

enum class ReviewDecision { APPROVE, REJECT, REQUEST_CHANGES }

@Entity
@Table(name = "question_generation_reviews")
class QuestionGenerationReviewEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "job_id", nullable = false) var jobId: UUID = UUID.randomUUID(),
    @Column(name = "reviewer_id", nullable = false) var reviewerId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) var decision: ReviewDecision = ReviewDecision.APPROVE,
    @Column(columnDefinition = "text") var comments: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

interface QuestionGenerationReviewRepository : org.springframework.data.jpa.repository.JpaRepository<QuestionGenerationReviewEntity, UUID> {
    fun findAllByJobIdOrderByCreatedAtAsc(jobId: UUID): List<QuestionGenerationReviewEntity>
}
