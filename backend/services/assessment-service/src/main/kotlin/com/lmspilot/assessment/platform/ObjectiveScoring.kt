package com.lmspilot.assessment.platform

import java.time.Instant
import java.util.UUID

enum class ObjectiveQuestionType { SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE }

data class ObjectiveQuestionSnapshot(
    val questionId: UUID,
    val type: ObjectiveQuestionType,
    val points: Double,
    val correctOptionIds: Set<String>,
    val partialCredit: Boolean = false,
    val negativeMarking: Double = 0.0,
)

data class ObjectiveAnswer(val questionId: UUID, val selectedOptionIds: Set<String>)
data class QuestionScore(val questionId: UUID, val earned: Double, val maximum: Double, val correct: Boolean)
data class ObjectiveScoreResult(val earned: Double, val maximum: Double, val percent: Double, val details: List<QuestionScore>)

/** Deterministic scorer. It only uses the immutable question snapshot captured when an attempt starts. */
object ObjectiveScorer {
    fun score(questions: List<ObjectiveQuestionSnapshot>, answers: List<ObjectiveAnswer>): ObjectiveScoreResult {
        require(questions.map { it.questionId }.toSet().size == questions.size) { "Duplicate question id" }
        val answerMap = answers.associateBy { it.questionId }
        val details = questions.map { question ->
            val selected = answerMap[question.questionId]?.selectedOptionIds.orEmpty()
            val exact = selected == question.correctOptionIds
            val raw = when {
                exact -> question.points
                question.partialCredit && question.type == ObjectiveQuestionType.MULTIPLE_CHOICE -> partial(question, selected)
                selected.isNotEmpty() -> -question.negativeMarking
                else -> 0.0
            }.coerceIn(0.0, question.points)
            QuestionScore(question.questionId, raw, question.points, exact)
        }
        val earned = details.sumOf { it.earned }
        val maximum = details.sumOf { it.maximum }
        return ObjectiveScoreResult(earned, maximum, if (maximum == 0.0) 0.0 else earned * 100.0 / maximum, details)
    }

    private fun partial(question: ObjectiveQuestionSnapshot, selected: Set<String>): Double {
        if (selected.isEmpty() || selected.any { it !in question.correctOptionIds }) return 0.0
        return question.points * selected.size.toDouble() / question.correctOptionIds.size.toDouble()
    }
}

data class RankedAttempt(
    val userId: UUID,
    val attemptId: UUID,
    val score: Double,
    val durationMs: Long,
    val submittedAt: Instant,
)

data class LeaderboardRow(val rank: Int, val attempt: RankedAttempt)

/** Stable ranking: score desc, duration asc, submittedAt asc, userId asc. */
object CompetitionRanker {
    fun rank(attempts: Collection<RankedAttempt>): List<LeaderboardRow> = attempts
        .groupBy { it.userId }
        .map { (_, userAttempts) ->
            userAttempts.sortedWith(
                compareByDescending<RankedAttempt> { it.score }
                    .thenBy { it.durationMs }
                    .thenBy { it.submittedAt }
                    .thenBy { it.attemptId },
            ).first()
        }
        .sortedWith(
            compareByDescending<RankedAttempt> { it.score }
                .thenBy { it.durationMs }
                .thenBy { it.submittedAt }
                .thenBy { it.userId },
        )
        .mapIndexed { index, attempt -> LeaderboardRow(index + 1, attempt) }
}
