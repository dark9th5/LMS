package com.lmspilot.learning.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.EventTypes
import com.lmspilot.contracts.Permissions
import com.lmspilot.learning.domain.XapiObjectType
import com.lmspilot.learning.domain.XapiStatementEntity
import com.lmspilot.learning.domain.XapiStatementRepository
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class XapiStatementRequest(
    val id: UUID? = null,
    @field:NotBlank @field:Size(max = 180) val verb: String,
    @field:NotBlank @field:Size(max = 500) val objectId: String,
    val objectType: XapiObjectType = XapiObjectType.OTHER,
    val courseId: UUID? = null,
    val lessonId: UUID? = null,
    val enrollmentId: UUID? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0") val score: Double? = null,
    val success: Boolean? = null,
    val completion: Boolean? = null,
    @field:Min(0) @field:Max(86400) val durationSeconds: Long? = null,
    val context: JsonNode? = null,
    val timestamp: Instant? = null,
    @field:Size(max = 80) val source: String = "WEB",
)

data class XapiStatementResponse(
    val id: UUID,
    val actorUserId: UUID,
    val verb: String,
    val objectId: String,
    val objectType: XapiObjectType,
    val courseId: UUID?,
    val lessonId: UUID?,
    val enrollmentId: UUID?,
    val score: Double?,
    val success: Boolean?,
    val completion: Boolean?,
    val durationSeconds: Long?,
    val context: JsonNode,
    val timestamp: Instant,
    val storedAt: Instant,
    val source: String,
)

@Service
class XapiService(
    private val repository: XapiStatementRepository,
    private val mapper: ObjectMapper,
    private val events: DomainEventPublisher,
) {
    @Transactional
    fun record(input: XapiStatementRequest): XapiStatementResponse {
        val userId = CurrentUser.id()
        val statementId = input.id ?: UUID.randomUUID()
        repository.findById(statementId).orElse(null)?.let { existing ->
            if (existing.actorUserId != userId) {
                throw ApiException(HttpStatus.CONFLICT, "XAPI_ID_CONFLICT", "Mã statement đã được sử dụng")
            }
            return existing.response(mapper)
        }
        val occurredAt = input.timestamp ?: Instant.now()
        if (occurredAt.isAfter(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            throw ApiException(HttpStatus.BAD_REQUEST, "XAPI_TIMESTAMP_IN_FUTURE", "Thời điểm statement không hợp lệ")
        }
        val entity = repository.save(
            XapiStatementEntity(
                id = statementId,
                actorUserId = userId,
                verb = input.verb.trim().lowercase(),
                objectId = input.objectId.trim(),
                objectType = input.objectType,
                courseId = input.courseId,
                lessonId = input.lessonId,
                enrollmentId = input.enrollmentId,
                resultScore = input.score,
                resultSuccess = input.success,
                resultCompletion = input.completion,
                durationSeconds = input.durationSeconds,
                contextJson = mapper.writeValueAsString(input.context ?: mapper.createObjectNode()),
                occurredAt = occurredAt,
                source = input.source.trim().uppercase(),
            )
        )
        events.publish(
            EventTypes.XAPI_STATEMENT_RECORDED,
            "learning-service",
            entity.id.toString(),
            mapOf(
                "statementId" to entity.id,
                "actorUserId" to userId,
                "verb" to entity.verb,
                "objectId" to entity.objectId,
                "objectType" to entity.objectType.name,
                "courseId" to entity.courseId,
                "lessonId" to entity.lessonId,
                "occurredAt" to entity.occurredAt,
            ),
        )
        return entity.response(mapper)
    }

    @Transactional(readOnly = true)
    fun mine() = repository.findTop200ByActorUserIdOrderByOccurredAtDesc(CurrentUser.id()).map { it.response(mapper) }

    @Transactional(readOnly = true)
    fun byUser(userId: UUID): List<XapiStatementResponse> {
        if (userId != CurrentUser.id() && Permissions.XAPI_READ_SCOPE !in CurrentUser.authorities()) {
            throw ApiException(HttpStatus.FORBIDDEN, "XAPI_OUT_OF_SCOPE", "Không có quyền xem hoạt động của người dùng này")
        }
        return repository.findTop200ByActorUserIdOrderByOccurredAtDesc(userId).map { it.response(mapper) }
    }

    @Transactional(readOnly = true)
    fun byCourse(courseId: UUID) = repository.findTop200ByCourseIdOrderByOccurredAtDesc(courseId).map { it.response(mapper) }
}

private fun XapiStatementEntity.response(mapper: ObjectMapper) = XapiStatementResponse(
    id,
    actorUserId,
    verb,
    objectId,
    objectType,
    courseId,
    lessonId,
    enrollmentId,
    resultScore,
    resultSuccess,
    resultCompletion,
    durationSeconds,
    mapper.readTree(contextJson),
    occurredAt,
    storedAt,
    source,
)

@RestController
@RequestMapping("/api/v1/xapi/statements")
class XapiController(private val service: XapiService) {
    @PostMapping
    @PreAuthorize("hasAuthority('${Permissions.XAPI_WRITE}')")
    fun record(@Valid @RequestBody input: XapiStatementRequest) = service.record(input)

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('${Permissions.XAPI_WRITE}')")
    fun mine() = service.mine()

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyAuthority('${Permissions.XAPI_WRITE}','${Permissions.XAPI_READ_SCOPE}')")
    fun byUser(@PathVariable userId: UUID) = service.byUser(userId)

    @GetMapping
    @PreAuthorize("hasAuthority('${Permissions.XAPI_READ_SCOPE}')")
    fun byCourse(@RequestParam courseId: UUID) = service.byCourse(courseId)
}
