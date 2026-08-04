package com.lmspilot.operations.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.Permissions
import com.lmspilot.operations.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

data class ServiceHealth(val name: String, val status: String, val version: String? = null, val details: Map<String, Any?> = emptyMap())
data class OperationRequest(val parameters: Map<String, String> = emptyMap())
data class OperationJobResponse(
    val id: UUID,
    val type: OperationType,
    val status: OperationStatus,
    val requestedBy: UUID,
    val requestedAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val resultJson: String?,
    val errorMessage: String?,
    val claimedBy: String?,
    val heartbeatAt: Instant?,
    val attemptCount: Int,
)
data class AgentClaimRequest(@field:NotBlank val agentId: String)
data class AgentClaimResponse(
    val id: UUID,
    val type: OperationType,
    val parameters: Map<String, String>,
    val claimToken: String,
    val leaseUntil: Instant,
    val attempt: Int,
)
data class AgentHeartbeatRequest(@field:NotBlank val claimToken: String)
data class AgentCompleteRequest(@field:NotBlank val claimToken: String, val success: Boolean, val result: Map<String, Any?> = emptyMap())
data class OperationScheduleRequest(
    @field:NotBlank @field:Size(max = 180) val name: String,
    val operationType: OperationType = OperationType.BACKUP,
    val frequency: OperationScheduleFrequency = OperationScheduleFrequency.DAILY,
    @field:Min(1) @field:Max(7) val dayOfWeek: Int? = null,
    @field:Min(0) @field:Max(23) val hourUtc: Int = 0,
    val parameters: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
)
data class OperationScheduleResponse(val id: UUID, val name: String, val operationType: OperationType, val frequency: OperationScheduleFrequency, val dayOfWeek: Int?, val hourUtc: Int, val parameters: Map<String, String>, val enabled: Boolean, val nextRunAt: Instant, val createdBy: UUID, val updatedAt: Instant)

@Service
class OperationsService(
    private val repository: OperationJobRepository,
    private val schedules: OperationScheduleRepository,
    private val mapper: ObjectMapper,
    @Value("\${operations.service-urls:}") rawUrls: String,
    @Value("\${operations.agent-lease:PT2M}") private val agentLease: Duration,
) {
    private val serviceUrls = rawUrls.split(',').mapNotNull {
        val p = it.split('=', limit = 2)
        if (p.size == 2) p[0] to p[1] else null
    }.toMap()

    fun health(): List<ServiceHealth> = serviceUrls.map { (name, url) ->
        runCatching {
            val body = RestClient.create().get().uri("$url/actuator/health").retrieve().body(Map::class.java) ?: emptyMap<String, Any>()
            ServiceHealth(name, body["status"]?.toString() ?: "UNKNOWN", details = body.entries.associate { it.key.toString() to it.value })
        }.getOrElse { ServiceHealth(name, "DOWN", details = mapOf("error" to (it.message ?: "unavailable"))) }
    }

    @Transactional(readOnly = true)
    fun jobs() = repository.findAllByOrderByRequestedAtDesc().map { it.response() }

    @Transactional
    fun request(type: OperationType, input: OperationRequest): OperationJobResponse {
        validate(type, input.parameters)
        return repository.save(OperationJobEntity(type = type, requestedBy = CurrentUser.id(), parametersJson = mapper.writeValueAsString(input.parameters))).response()
    }

    @Transactional
    fun claim(agentId: String): AgentClaimResponse? {
        val job = repository.lockNextClaimable() ?: return null
        val now = Instant.now()
        job.status = OperationStatus.RUNNING
        job.startedAt = job.startedAt ?: now
        job.claimedBy = agentId.trim().take(160)
        job.claimToken = UUID.randomUUID().toString()
        job.heartbeatAt = now
        job.leaseUntil = now.plus(agentLease)
        job.attemptCount += 1
        return AgentClaimResponse(
            job.id,
            job.type,
            mapper.readValue(job.parametersJson, object : TypeReference<Map<String, String>>() {}),
            job.claimToken!!,
            job.leaseUntil!!,
            job.attemptCount,
        )
    }

    @Transactional
    fun heartbeat(id: UUID, claimToken: String): Instant {
        val job = claimed(id, claimToken)
        if (job.status != OperationStatus.RUNNING) throw conflict("Job không còn ở trạng thái RUNNING")
        val now = Instant.now()
        job.heartbeatAt = now
        job.leaseUntil = now.plus(agentLease)
        return job.leaseUntil!!
    }

    @Transactional
    fun complete(id: UUID, input: AgentCompleteRequest): OperationJobResponse {
        val job = claimed(id, input.claimToken)
        if (job.status != OperationStatus.RUNNING) throw conflict("Job không còn ở trạng thái RUNNING")
        job.status = if (input.success) OperationStatus.SUCCEEDED else OperationStatus.FAILED
        job.finishedAt = Instant.now()
        job.resultJson = mapper.writeValueAsString(input.result)
        job.errorMessage = if (input.success) null else input.result["error"]?.toString()?.take(4000) ?: "Operation failed"
        job.leaseUntil = null
        return job.response()
    }

    @Transactional(readOnly = true)
    fun schedules() = schedules.findAllByOrderByCreatedAtDesc().map { it.scheduleResponse(mapper) }

    @Transactional
    fun createSchedule(input: OperationScheduleRequest): OperationScheduleResponse {
        validateSchedule(input)
        val entity = OperationScheduleEntity(
            name = input.name.trim(), operationType = input.operationType, frequency = input.frequency,
            dayOfWeek = input.dayOfWeek, hourUtc = input.hourUtc, parametersJson = mapper.writeValueAsString(input.parameters),
            enabled = input.enabled, nextRunAt = nextRun(input.frequency, input.dayOfWeek, input.hourUtc, Instant.now()), createdBy = CurrentUser.id(),
        )
        return schedules.save(entity).scheduleResponse(mapper)
    }

    @Transactional
    fun updateSchedule(id: UUID, input: OperationScheduleRequest): OperationScheduleResponse {
        validateSchedule(input)
        val entity = schedules.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "OPERATION_SCHEDULE_NOT_FOUND", "Không tìm thấy lịch vận hành") }
        entity.name = input.name.trim(); entity.operationType = input.operationType; entity.frequency = input.frequency; entity.dayOfWeek = input.dayOfWeek
        entity.hourUtc = input.hourUtc; entity.parametersJson = mapper.writeValueAsString(input.parameters); entity.enabled = input.enabled
        entity.nextRunAt = nextRun(input.frequency, input.dayOfWeek, input.hourUtc, Instant.now()); entity.updatedAt = Instant.now()
        return entity.scheduleResponse(mapper)
    }

    @Transactional
    fun deleteSchedule(id: UUID) {
        if (!schedules.existsById(id)) throw ApiException(HttpStatus.NOT_FOUND, "OPERATION_SCHEDULE_NOT_FOUND", "Không tìm thấy lịch vận hành")
        schedules.deleteById(id)
    }

    @Scheduled(fixedDelayString = "\${operations.schedule-worker-delay-ms:60000}")
    @Transactional
    fun runSchedules() {
        val now = Instant.now()
        schedules.findTop50ByEnabledTrueAndNextRunAtBeforeOrderByNextRunAtAsc(now).forEach { schedule ->
            repository.save(OperationJobEntity(type = schedule.operationType, requestedBy = schedule.createdBy, parametersJson = schedule.parametersJson))
            schedule.nextRunAt = nextRun(schedule.frequency, schedule.dayOfWeek, schedule.hourUtc, now.plusSeconds(60))
            schedule.updatedAt = now
        }
    }

    private fun validateSchedule(input: OperationScheduleRequest) {
        if (input.operationType !in setOf(OperationType.BACKUP, OperationType.MAINTENANCE)) throw ApiException(HttpStatus.BAD_REQUEST, "UNSAFE_SCHEDULE_TYPE", "Chỉ cho phép lập lịch sao lưu hoặc chế độ bảo trì")
        if (input.frequency == OperationScheduleFrequency.WEEKLY && input.dayOfWeek == null) throw ApiException(HttpStatus.BAD_REQUEST, "SCHEDULE_DAY_REQUIRED", "Lịch tuần cần ngày trong tuần")
        validate(input.operationType, input.parameters)
    }

    private fun nextRun(frequency: OperationScheduleFrequency, dayOfWeek: Int?, hourUtc: Int, from: Instant): Instant {
        var next = ZonedDateTime.ofInstant(from, ZoneOffset.UTC).withMinute(0).withSecond(0).withNano(0).withHour(hourUtc)
        if (!next.toInstant().isAfter(from)) next = next.plusDays(1)
        if (frequency == OperationScheduleFrequency.WEEKLY) {
            val target = DayOfWeek.of(dayOfWeek ?: 1)
            while (next.dayOfWeek != target) next = next.plusDays(1)
        }
        return next.toInstant()
    }

    private fun claimed(id: UUID, supplied: String): OperationJobEntity {
        val job = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Không tìm thấy job") }
        if (job.claimToken.isNullOrBlank() || job.claimToken != supplied) throw ApiException(HttpStatus.FORBIDDEN, "INVALID_CLAIM_TOKEN", "Claim token không hợp lệ")
        return job
    }

    private fun validate(type: OperationType, parameters: Map<String, String>) {
        if (type == OperationType.RESTORE) {
            if (parameters["confirmation"] != "RESTORE") throw ApiException(HttpStatus.BAD_REQUEST, "RESTORE_CONFIRMATION_REQUIRED", "Phục hồi yêu cầu confirmation=RESTORE")
            if (parameters["backupPath"].isNullOrBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "BACKUP_PATH_REQUIRED", "Phục hồi yêu cầu backupPath")
        }
        if (type == OperationType.MAINTENANCE && parameters["mode"]?.uppercase() !in setOf("ON", "OFF")) {
            throw ApiException(HttpStatus.BAD_REQUEST, "MAINTENANCE_MODE_REQUIRED", "Chế độ bảo trì yêu cầu mode=ON hoặc OFF")
        }
        if (type in setOf(OperationType.UPDATE, OperationType.ROLLBACK) && parameters["packagePath"].isNullOrBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "PACKAGE_PATH_REQUIRED", "Cập nhật/rollback yêu cầu packagePath")
        }
    }

    private fun conflict(message: String): Nothing = throw ApiException(HttpStatus.CONFLICT, "OPERATION_STATE_CONFLICT", message)
}

private fun OperationScheduleEntity.scheduleResponse(mapper: ObjectMapper) = OperationScheduleResponse(id, name, operationType, frequency, dayOfWeek, hourUtc, mapper.readValue(parametersJson, object : TypeReference<Map<String, String>>() {}), enabled, nextRunAt, createdBy, updatedAt)

private fun OperationJobEntity.response() = OperationJobResponse(id, type, status, requestedBy, requestedAt, startedAt, finishedAt, resultJson, errorMessage, claimedBy, heartbeatAt, attemptCount)

@RestController
@RequestMapping("/api/v1/operations")
@PreAuthorize("hasAuthority('${Permissions.OPERATIONS_MANAGE}')")
class OperationsController(private val service: OperationsService) {
    @GetMapping("/health") fun health() = service.health()
    @GetMapping("/jobs") fun jobs() = service.jobs()
    @PostMapping("/jobs/{type}") fun request(@PathVariable type: OperationType, @Valid @RequestBody input: OperationRequest) = service.request(type, input)
    @GetMapping("/schedules") fun schedules() = service.schedules()
    @PostMapping("/schedules") fun createSchedule(@Valid @RequestBody input: OperationScheduleRequest) = service.createSchedule(input)
    @PutMapping("/schedules/{id}") fun updateSchedule(@PathVariable id: UUID, @Valid @RequestBody input: OperationScheduleRequest) = service.updateSchedule(id, input)
    @DeleteMapping("/schedules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) fun deleteSchedule(@PathVariable id: UUID) = service.deleteSchedule(id)
}

@RestController
@RequestMapping("/internal/v1/operations/jobs")
class InternalOperationsAgentController(
    private val service: OperationsService,
    private val internal: InternalTokenAuthorizer,
) {
    @PostMapping("/claim")
    fun claim(
        @Valid @RequestBody input: AgentClaimRequest,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ): ResponseEntity<AgentClaimResponse> {
        internal.require(token)
        val job = service.claim(input.agentId) ?: return ResponseEntity.noContent().build<AgentClaimResponse>()
        return ResponseEntity.ok(job)
    }

    @PostMapping("/{id}/heartbeat")
    fun heartbeat(
        @PathVariable id: UUID,
        @Valid @RequestBody input: AgentHeartbeatRequest,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ) = internal.require(token).let { mapOf("leaseUntil" to service.heartbeat(id, input.claimToken)) }

    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: UUID,
        @Valid @RequestBody input: AgentCompleteRequest,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ) = internal.require(token).let { service.complete(id, input) }
}
