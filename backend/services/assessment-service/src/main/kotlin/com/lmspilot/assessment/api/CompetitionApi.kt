package com.lmspilot.assessment.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.assessment.platform.AssessmentContextType
import com.lmspilot.assessment.platform.CompetitionRanker
import com.lmspilot.assessment.platform.RankedAttempt
import com.lmspilot.assessment.domain.*
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import com.lmspilot.support.security.InternalTokenAuthorizer
import com.lmspilot.support.security.ScopedAuthorizationClient
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class CompetitionRewardRequest(
    @field:Min(1) val rankFrom: Int,
    @field:Min(1) val rankTo: Int,
    @field:NotBlank @field:Size(max = 40) val rewardType: String,
    val rewardPayload: Map<String, Any?> = emptyMap(),
) {
    @AssertTrue(message = "rankTo phải lớn hơn hoặc bằng rankFrom")
    fun validRange() = rankTo >= rankFrom
}

data class CompetitionDetailsRequest(
    val registrationOpensAt: Instant? = null,
    val registrationClosesAt: Instant? = null,
    val leaderboardVisibility: LeaderboardVisibility = LeaderboardVisibility.AFTER_CLOSE,
    @field:Size(max = 20) val rewards: List<CompetitionRewardRequest> = emptyList(),
) {
    @AssertTrue(message = "Thời gian đóng đăng ký phải sau thời gian mở")
    fun validWindow() = registrationClosesAt == null || registrationOpensAt == null || registrationClosesAt.isAfter(registrationOpensAt)
}

data class CreateCompetitionRequest(
    @field:Valid val exam: ExamRequest,
    @field:Valid val competition: CompetitionDetailsRequest = CompetitionDetailsRequest(),
)

data class CompetitionRewardResponse(
    val id: UUID,
    val rankFrom: Int,
    val rankTo: Int,
    val rewardType: String,
    val rewardPayload: Map<String, Any?>,
)

data class CompetitionResponse(
    val id: UUID,
    val exam: ExamResponse,
    val registrationOpensAt: Instant?,
    val registrationClosesAt: Instant?,
    val leaderboardVisibility: LeaderboardVisibility,
    val resultStatus: CompetitionResultStatus,
    val publishedAt: Instant?,
    val rewards: List<CompetitionRewardResponse>,
)

data class LeaderboardEntryResponse(
    val rank: Int?,
    val userId: UUID,
    val attemptId: UUID,
    val score: Double,
    val durationMs: Long,
    val submittedAt: Instant,
    val mine: Boolean,
)

data class CompetitionResultInput(
    val userId: UUID,
    val attemptId: UUID,
    val score: Double,
    val durationMs: Long,
    val submittedAt: Instant,
)

data class RewardLedgerResponse(
    val id: UUID,
    val userId: UUID,
    val rewardId: UUID,
    val status: RewardLedgerStatus,
    val issuedAt: Instant?,
    val externalReference: String?,
)

@Service
class CompetitionService(
    private val assessment: AssessmentManagementService,
    private val exams: ExamRepository,
    private val contexts: AssessmentContextRepository,
    private val competitions: CompetitionRepository,
    private val leaderboard: CompetitionLeaderboardRepository,
    private val rewards: CompetitionRewardRepository,
    private val ledgers: RewardLedgerRepository,
    private val mapper: ObjectMapper,
    private val scopedAuthorization: ScopedAuthorizationClient,
    private val audience: AssessmentAudienceService,
) {
    @Transactional
    fun create(input: CreateCompetitionRequest): CompetitionResponse {
        val examInput = input.exam.copy(courseId = null, contextType = AssessmentContextType.COMPETITION)
        val exam = assessment.createExam(examInput)
        configureEntity(exam.id, input.competition)
        return response(exam.id)
    }

    @Transactional
    fun update(id: UUID, input: CompetitionDetailsRequest): CompetitionResponse {
        requireManageable(id)
        configureEntity(id, input)
        return response(id)
    }

    @Transactional(readOnly = true)
    fun list(): List<CompetitionResponse> {
        val canManage = Permissions.COMPETITIONS_MANAGE in CurrentUser.authorities()
        val now = Instant.now()
        return competitions.findAll()
            .filter { competition ->
                exams.findById(competition.assessmentId).orElse(null)?.let { exam ->
                    (canManage && manageable(exam, Permissions.COMPETITIONS_MANAGE)) ||
                        (exam.status == ExamStatus.ACTIVE &&
                            audience.isEligible(exam.id, CurrentUser.id(), now) &&
                            (exam.opensAt == null || !exam.opensAt!!.isAfter(now)) &&
                            (exam.closesAt == null || exam.closesAt!!.isAfter(now)))
                } == true
            }
            .sortedByDescending { exams.findById(it.assessmentId).orElse(null)?.updatedAt }
            .map { response(it.assessmentId) }
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): CompetitionResponse {
        competitions.findById(id).orElseThrow { notFound() }
        val exam = exams.findById(id).orElseThrow { notFound() }
        val canManage = Permissions.COMPETITIONS_MANAGE in CurrentUser.authorities() &&
            manageable(exam, Permissions.COMPETITIONS_MANAGE)
        val now = Instant.now()
        val publiclyVisible = exam.status == ExamStatus.ACTIVE &&
            audience.isEligible(id, CurrentUser.id(), now) &&
            (exam.opensAt == null || !exam.opensAt!!.isAfter(now)) &&
            (exam.closesAt == null || exam.closesAt!!.isAfter(now))
        if (!canManage && !publiclyVisible) throw notFound()
        return response(id)
    }

    @Transactional
    fun record(id: UUID, input: CompetitionResultInput) {
        competitions.findById(id).orElseThrow { notFound() }
        require(input.durationMs >= 0) { "durationMs must be non-negative" }
        val previous = leaderboard.findByCompetitionIdAndUserId(id, input.userId)
        val candidate = RankedAttempt(input.userId, input.attemptId, input.score, input.durationMs, input.submittedAt)
        val shouldReplace = previous == null || CompetitionRanker.rank(
            listOf(
                candidate,
                RankedAttempt(previous.userId, previous.attemptId, previous.score, previous.durationMs, previous.submittedAt),
            )
        ).first().attempt.attemptId == candidate.attemptId
        if (shouldReplace) {
            leaderboard.save(
                CompetitionLeaderboardEntity(
                    competitionId = id,
                    userId = input.userId,
                    attemptId = input.attemptId,
                    score = input.score,
                    durationMs = input.durationMs,
                    submittedAt = input.submittedAt,
                )
            )
        }
        recalculate(id)
    }

    @Transactional(readOnly = true)
    fun leaderboard(id: UUID): List<LeaderboardEntryResponse> {
        val competition = competitions.findById(id).orElseThrow { notFound() }
        val exam = exams.findById(id).orElseThrow { notFound() }
        val canManage = Permissions.COMPETITIONS_MANAGE in CurrentUser.authorities() &&
            manageable(exam, Permissions.COMPETITIONS_MANAGE)
        if (!canManage && !audience.isEligible(id, CurrentUser.id())) {
            throw notFound()
        }
        val visible = canManage || competition.leaderboardVisibility == LeaderboardVisibility.LIVE ||
            (competition.leaderboardVisibility == LeaderboardVisibility.AFTER_CLOSE &&
                (competition.resultStatus != CompetitionResultStatus.PROVISIONAL || exam.closesAt?.isBefore(Instant.now()) == true))
        val rows = leaderboard.findAllByCompetitionIdOrderByRankAsc(id)
        return (if (visible) rows else rows.filter { it.userId == CurrentUser.id() }).map { it.response() }
    }

    @Transactional
    fun publish(id: UUID): CompetitionResponse {
        requireManageable(id)
        recalculate(id)
        val entity = competitions.findById(id).orElseThrow { notFound() }
        entity.resultStatus = CompetitionResultStatus.PUBLISHED
        entity.publishedAt = Instant.now()
        entity.publishedBy = CurrentUser.id()
        issueRewardsInternal(id)
        return response(id)
    }

    @Transactional
    fun issueRewards(id: UUID): List<RewardLedgerResponse> {
        requireManageable(id, Permissions.COMPETITIONS_REWARD)
        return issueRewardsInternal(id)
    }

    private fun issueRewardsInternal(id: UUID): List<RewardLedgerResponse> {
        val rules = rewards.findAllByCompetitionIdOrderByRankFromAsc(id)
        val rows = leaderboard.findAllByCompetitionIdOrderByRankAsc(id)
        for (row in rows) {
            val rank = row.rank ?: continue
            rules.filter { rank in it.rankFrom..it.rankTo }.forEach { reward ->
                if (!ledgers.existsByCompetitionIdAndUserIdAndRewardId(id, row.userId, reward.id)) {
                    ledgers.save(
                        RewardLedgerEntity(
                            competitionId = id,
                            userId = row.userId,
                            rewardId = reward.id,
                            status = RewardLedgerStatus.ISSUED,
                            issuedAt = Instant.now(),
                            externalReference = "LMS-${id.toString().take(8)}-$rank",
                        )
                    )
                }
            }
        }
        return ledgers.findAllByCompetitionIdOrderByCreatedAtAsc(id).map { it.response() }
    }

    @Transactional(readOnly = true)
    fun rewardLedger(id: UUID): List<RewardLedgerResponse> {
        requireManageable(id)
        return ledgers.findAllByCompetitionIdOrderByCreatedAtAsc(id).map { it.response() }
    }

    private fun configureEntity(id: UUID, input: CompetitionDetailsRequest) {
        val exam = requireManageable(id)
        if (exam.courseId != null) throw ApiException(HttpStatus.CONFLICT, "COMPETITION_COURSE_LINK", "Cuộc thi không được gắn với khóa học")
        contexts.save(
            AssessmentContextEntity(
                assessmentId = id,
                contextType = AssessmentContextType.COMPETITION,
                courseId = null,
                opensAt = exam.opensAt,
                closesAt = exam.closesAt,
                maxAttempts = exam.maxAttempts,
                autoGrade = true,
            )
        )
        val entity = competitions.findById(id).orElse(CompetitionEntity(assessmentId = id))
        entity.registrationOpensAt = input.registrationOpensAt
        entity.registrationClosesAt = input.registrationClosesAt
        entity.leaderboardVisibility = input.leaderboardVisibility
        competitions.save(entity)
        rewards.deleteAllByCompetitionId(id)
        input.rewards.forEach { reward ->
            rewards.save(
                CompetitionRewardEntity(
                    competitionId = id,
                    rankFrom = reward.rankFrom,
                    rankTo = reward.rankTo,
                    rewardType = reward.rewardType.trim().uppercase(),
                    rewardPayloadJson = mapper.writeValueAsString(reward.rewardPayload),
                )
            )
        }
    }

    private fun recalculate(id: UUID) {
        val rows = leaderboard.findAllByCompetitionIdOrderByRankAsc(id)
        val ranked = CompetitionRanker.rank(rows.map { RankedAttempt(it.userId, it.attemptId, it.score, it.durationMs, it.submittedAt) })
        val byAttempt = ranked.associate { it.attempt.attemptId to it.rank }
        rows.forEach { row ->
            row.rank = byAttempt[row.attemptId]
            row.calculatedAt = Instant.now()
            leaderboard.save(row)
        }
    }

    private fun response(id: UUID): CompetitionResponse {
        val entity = competitions.findById(id).orElseThrow { notFound() }
        val exam = assessment.getExam(id)
        return CompetitionResponse(
            id = id,
            exam = exam,
            registrationOpensAt = entity.registrationOpensAt,
            registrationClosesAt = entity.registrationClosesAt,
            leaderboardVisibility = entity.leaderboardVisibility,
            resultStatus = entity.resultStatus,
            publishedAt = entity.publishedAt,
            rewards = rewards.findAllByCompetitionIdOrderByRankFromAsc(id).map { it.response(mapper) },
        )
    }

    private fun requireManageable(id: UUID, permission: String = Permissions.COMPETITIONS_MANAGE): ExamEntity {
        val exam = exams.findById(id).orElseThrow { notFound() }
        if (permission !in CurrentUser.authorities() && Permissions.COMPETITIONS_MANAGE !in CurrentUser.authorities()) {
            throw ApiException(HttpStatus.FORBIDDEN, "COMPETITION_OUT_OF_SCOPE", "Bạn không có quyền quản lý cuộc thi")
        }
        if (!manageable(exam, permission)) {
            throw ApiException(HttpStatus.FORBIDDEN, "COMPETITION_OUT_OF_SCOPE", "Cuộc thi ngoài phạm vi quản lý")
        }
        return exam
    }

    private fun manageable(exam: ExamEntity, permission: String): Boolean =
        permission in CurrentUser.authorities() ||
            Permissions.COMPETITIONS_MANAGE in CurrentUser.authorities() || exam.ownerId == CurrentUser.id() ||
            scopedAuthorization.allowed(permission, "EXAM", exam.id) ||
            scopedAuthorization.allowed(Permissions.COMPETITIONS_MANAGE, "EXAM", exam.id)

    private fun notFound() = ApiException(HttpStatus.NOT_FOUND, "COMPETITION_NOT_FOUND", "Không tìm thấy cuộc thi")
}

private fun CompetitionLeaderboardEntity.response() = LeaderboardEntryResponse(
    rank, userId, attemptId, score, durationMs, submittedAt, userId == CurrentUser.id(),
)

private fun CompetitionRewardEntity.response(mapper: ObjectMapper) = CompetitionRewardResponse(
    id, rankFrom, rankTo, rewardType,
    mapper.readValue(rewardPayloadJson, object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {}),
)

private fun RewardLedgerEntity.response() = RewardLedgerResponse(id, userId, rewardId, status, issuedAt, externalReference)

@RestController
@RequestMapping("/api/v1/competitions")
class CompetitionController(private val service: CompetitionService) {
    @GetMapping
    @PreAuthorize("hasAnyAuthority('${Permissions.COMPETITIONS_PARTICIPATE}','${Permissions.COMPETITIONS_MANAGE}')")
    fun list() = service.list()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('${Permissions.COMPETITIONS_PARTICIPATE}','${Permissions.COMPETITIONS_MANAGE}')")
    fun get(@PathVariable id: UUID) = service.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.COMPETITIONS_MANAGE}')")
    fun create(@Valid @RequestBody input: CreateCompetitionRequest) = service.create(input)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.COMPETITIONS_MANAGE}')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody input: CompetitionDetailsRequest) = service.update(id, input)

    @GetMapping("/{id}/leaderboard")
    @PreAuthorize("hasAnyAuthority('${Permissions.COMPETITIONS_PARTICIPATE}','${Permissions.COMPETITIONS_MANAGE}')")
    fun leaderboard(@PathVariable id: UUID) = service.leaderboard(id)

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('${Permissions.COMPETITIONS_MANAGE}')")
    fun publish(@PathVariable id: UUID) = service.publish(id)

    @PostMapping("/{id}/rewards/issue")
    @PreAuthorize("hasAuthority('${Permissions.COMPETITIONS_REWARD}')")
    fun issueRewards(@PathVariable id: UUID) = service.issueRewards(id)

    @GetMapping("/{id}/rewards/ledger")
    @PreAuthorize("hasAnyAuthority('${Permissions.COMPETITIONS_REWARD}','${Permissions.COMPETITIONS_MANAGE}')")
    fun rewardLedger(@PathVariable id: UUID) = service.rewardLedger(id)
}

@RestController
@RequestMapping("/internal/v1/competitions")
class InternalCompetitionController(
    private val service: CompetitionService,
    private val internal: InternalTokenAuthorizer,
) {
    @PostMapping("/{id}/results")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun result(
        @PathVariable id: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
        @RequestBody input: CompetitionResultInput,
    ) {
        internal.require(token)
        service.record(id, input)
    }
}
