package com.lmspilot.assessment.domain

import com.lmspilot.assessment.platform.AssessmentContextType
import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "assessment_contexts")
class AssessmentContextEntity(
    @Id @Column(name = "assessment_id") var assessmentId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 30)
    var contextType: AssessmentContextType = AssessmentContextType.COURSE_QUIZ,
    @Column(name = "course_id") var courseId: UUID? = null,
    @Column(name = "cohort_id") var cohortId: UUID? = null,
    @Column(name = "opens_at") var opensAt: Instant? = null,
    @Column(name = "closes_at") var closesAt: Instant? = null,
    @Column(name = "max_attempts", nullable = false) var maxAttempts: Int = 1,
    @Column(name = "auto_grade", nullable = false) var autoGrade: Boolean = true,
)

interface AssessmentContextRepository : org.springframework.data.jpa.repository.JpaRepository<AssessmentContextEntity, UUID>

enum class LeaderboardVisibility { LIVE, AFTER_CLOSE, ADMIN_ONLY }
enum class CompetitionResultStatus { PROVISIONAL, PUBLISHED, LOCKED }

enum class RewardLedgerStatus { PENDING, ISSUED, FAILED, CANCELLED }

@Entity
@Table(name = "competitions")
class CompetitionEntity(
    @Id @Column(name = "assessment_id") var assessmentId: UUID = UUID.randomUUID(),
    @Column(name = "registration_opens_at") var registrationOpensAt: Instant? = null,
    @Column(name = "registration_closes_at") var registrationClosesAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "leaderboard_visibility", nullable = false, length = 30)
    var leaderboardVisibility: LeaderboardVisibility = LeaderboardVisibility.AFTER_CLOSE,
    @Column(name = "tie_break_rule", nullable = false, length = 80)
    var tieBreakRule: String = "SCORE_DURATION_SUBMITTED_AT",
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    var resultStatus: CompetitionResultStatus = CompetitionResultStatus.PROVISIONAL,
    @Column(name = "published_at") var publishedAt: Instant? = null,
    @Column(name = "published_by") var publishedBy: UUID? = null,
)

interface CompetitionRepository : org.springframework.data.jpa.repository.JpaRepository<CompetitionEntity, UUID>

@Entity
@Table(name = "competition_leaderboard")
@IdClass(CompetitionLeaderboardId::class)
class CompetitionLeaderboardEntity(
    @Id @Column(name = "competition_id") var competitionId: UUID = UUID.randomUUID(),
    @Id @Column(name = "user_id") var userId: UUID = UUID.randomUUID(),
    @Column(name = "attempt_id", nullable = false) var attemptId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var score: Double = 0.0,
    @Column(name = "duration_ms", nullable = false) var durationMs: Long = 0,
    @Column(name = "submitted_at", nullable = false) var submittedAt: Instant = Instant.now(),
    @Column var rank: Int? = null,
    @Column(name = "calculated_at", nullable = false) var calculatedAt: Instant = Instant.now(),
)

data class CompetitionLeaderboardId(var competitionId: UUID? = null, var userId: UUID? = null) : Serializable

interface CompetitionLeaderboardRepository : org.springframework.data.jpa.repository.JpaRepository<CompetitionLeaderboardEntity, CompetitionLeaderboardId> {
    fun findAllByCompetitionIdOrderByRankAsc(competitionId: UUID): List<CompetitionLeaderboardEntity>
    fun findByCompetitionIdAndUserId(competitionId: UUID, userId: UUID): CompetitionLeaderboardEntity?
}

@Entity
@Table(name = "competition_rewards")
class CompetitionRewardEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "competition_id", nullable = false) var competitionId: UUID = UUID.randomUUID(),
    @Column(name = "rank_from", nullable = false) var rankFrom: Int = 1,
    @Column(name = "rank_to", nullable = false) var rankTo: Int = 1,
    @Column(name = "reward_type", nullable = false, length = 40) var rewardType: String = "BADGE",
    @Column(name = "reward_payload_json", nullable = false, columnDefinition = "text") var rewardPayloadJson: String = "{}",
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

interface CompetitionRewardRepository : org.springframework.data.jpa.repository.JpaRepository<CompetitionRewardEntity, UUID> {
    fun findAllByCompetitionIdOrderByRankFromAsc(competitionId: UUID): List<CompetitionRewardEntity>
    fun deleteAllByCompetitionId(competitionId: UUID)
}

@Entity
@Table(name = "reward_ledger", uniqueConstraints = [UniqueConstraint(columnNames = ["competition_id", "user_id", "reward_id"])])
class RewardLedgerEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "competition_id", nullable = false) var competitionId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(name = "reward_id", nullable = false) var rewardId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) var status: RewardLedgerStatus = RewardLedgerStatus.PENDING,
    @Column(name = "issued_at") var issuedAt: Instant? = null,
    @Column(name = "external_reference", length = 160) var externalReference: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)

interface RewardLedgerRepository : org.springframework.data.jpa.repository.JpaRepository<RewardLedgerEntity, UUID> {
    fun existsByCompetitionIdAndUserIdAndRewardId(competitionId: UUID, userId: UUID, rewardId: UUID): Boolean
    fun findAllByCompetitionIdOrderByCreatedAtAsc(competitionId: UUID): List<RewardLedgerEntity>
}
