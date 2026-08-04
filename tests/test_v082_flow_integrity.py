from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class Release082FlowIntegrityTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_assignment_and_exam_completion_are_server_verified(self) -> None:
        learning = self.read("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/LearningApi.kt")
        assignment = self.read("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/AssignmentSubmissionApi.kt")
        player = self.read("apps/web/components/LearningPlayer.tsx")
        self.assertIn('lessonType in setOf("ASSIGNMENT", "EXAM")', learning)
        self.assertIn('"VERIFIED_OUTCOME_REQUIRED"', learning)
        self.assertIn("fun recordVerifiedOutcome(", learning)
        self.assertGreaterEqual(assignment.count("progress.recordVerifiedOutcome("), 3)
        self.assertIn("completed = accepted != null", assignment)
        self.assertIn("completed = false", assignment)
        self.assertIn('"ASSIGNMENT_NOT_PENDING"', assignment)
        self.assertIn('"ASSIGNMENT_PENDING_GRADE"', assignment)
        self.assertIn('"ASSIGNMENT_SUPERSEDED"', assignment)
        self.assertNotIn("await updateLesson(lesson, true", player)

    def test_exam_sessions_are_bound_to_exact_enrollment_and_attempt_scope(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        migration = self.read("backend/services/assessment-service/src/main/resources/db/migration/V7__exam_session_learning_context.sql")
        self.assertIn("val enrollmentId: UUID? = null", assessment)
        self.assertIn('"ENROLLMENT_REQUIRED"', assessment)
        self.assertIn("findAllByExamIdAndEnrollmentIdOrderByAttemptNoAsc", assessment)
        self.assertIn("uq_exam_enrollment_attempt", migration)
        self.assertIn("where enrollment_id is not null", migration)

    def test_exam_grade_projection_uses_exact_enrollment_and_effective_score(self) -> None:
        reporting = self.read("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/ReportingApi.kt")
        self.assertIn("p.enrollmentId?.let(readModels::findByEnrollmentId)", reporting)
        self.assertIn("p.effectivePercentage ?:", reporting)
        self.assertIn("p.effectivePassed ?: p.passed", reporting)
        self.assertIn("Legacy events without enrollment context", reporting)
        notification = self.read("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationApi.kt")
        self.assertIn("val effectivePassed = it.effectivePassed ?: it.passed", notification)
        self.assertIn('"enrollmentId" to it.enrollmentId', notification)

    def test_competition_start_requires_the_dedicated_participation_permission(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        start = assessment[assessment.index("fun start(input:"):assessment.index("fun saveAnswers(")]
        competition = start[start.index("if (contextType == AssessmentContextType.COMPETITION)"):start.index("} else if (!canTakeAssessments())")]
        self.assertIn("if (!canParticipateCompetitions())", competition)
        self.assertNotIn("!canTakeAssessments()", competition)

    def test_expired_sessions_are_not_sent_to_grading(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        submit = assessment[assessment.index("fun submit(sessionId:"):assessment.index("fun resume(sessionId:")]
        expired_return = submit.index("return sessionResponse(session)")
        publish = submit.index("events.publish(EventTypes.EXAM_SUBMITTED")
        self.assertLess(expired_return, publish)
        self.assertIn("SESSION_NOT_SUBMITTED", assessment)

    def test_exam_flags_have_real_runtime_behavior(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        grading = self.read("backend/services/grading-service/src/main/kotlin/com/lmspilot/grading/api/GradingApi.kt")
        self.assertIn("if (exam.shuffleQuestions)", assessment)
        self.assertIn("if (exam.shuffleAnswers)", assessment)
        self.assertIn("!payload.autoGrade", grading)
        self.assertIn('"HIGHEST" ->', grading)
        self.assertIn('"AVERAGE" ->', grading)
        self.assertIn("effectivePassed", grading)

    def test_manual_grading_contains_question_and_learner_answer(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        grading = self.read("backend/services/grading-service/src/main/kotlin/com/lmspilot/grading/api/GradingApi.kt")
        ui = self.read("apps/web/components/GradingPage.tsx")
        self.assertIn("it.promptSnapshot", assessment)
        self.assertIn("q.prompt, answer", grading)
        self.assertIn("Câu trả lời:", ui)

    def test_attempt_numbering_is_serialized_without_a_long_remote_call_lock(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        repository = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/domain/AssessmentDomain.kt")
        start = assessment[assessment.index("fun start(input:"):assessment.index("fun saveAnswers(")]
        self.assertLess(start.index("resolveEnrollment("), start.index("findStartSnapshotById("))
        self.assertLess(start.index("findStartSnapshotById("), start.index("lockAttemptSequence("))
        self.assertIn("PESSIMISTIC_READ", repository)
        self.assertIn("pg_advisory_xact_lock", repository)

    def test_answer_save_and_submit_are_serialized_on_the_session(self) -> None:
        assessment = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        repository = self.read("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/domain/AssessmentDomain.kt")
        save = assessment[assessment.index("fun saveAnswers("):assessment.index("fun submit(")]
        submit = assessment[assessment.index("fun submit("):assessment.index("fun resume(")]
        self.assertIn("ownedSessionForUpdate(sessionId)", save)
        self.assertIn("ownedSessionForUpdate(sessionId)", submit)
        self.assertIn("PESSIMISTIC_WRITE", repository)
        self.assertIn("findForUpdateById", repository)

    def test_file_access_is_object_granted_not_purpose_wide(self) -> None:
        storage = self.read("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileStorageApi.kt")
        internal = self.read("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/InternalFileApi.kt")
        domain = self.read("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/domain/FileDomain.kt")
        self.assertIn("FileAccessGrantRepository", storage)
        self.assertIn("expiresAt?.isAfter", storage)
        readable = storage[storage.index("private fun readable"):storage.index("private fun hasAdministrativeFileAccess")]
        self.assertNotIn('"COURSE_CONTENT" -> Permissions.FILES_DOWNLOAD', readable)
        self.assertNotIn('"ASSIGNMENT_SUBMISSION" ->', readable)
        self.assertIn("service.internalDownload", internal)
        self.assertIn("HttpRange.parseRanges", storage)
        self.assertIn("HttpStatus.PARTIAL_CONTENT", storage)
        self.assertIn("HttpHeaders.ACCEPT_RANGES", storage)
        self.assertIn("file_access_grants", self.read("backend/services/file-storage-service/src/main/resources/db/migration/V4__file_access_grants.sql"))
        self.assertIn("class FileAccessGrantEntity", domain)

    def test_course_file_attachment_checks_owner_and_purpose(self) -> None:
        course = self.read("backend/services/course-service/src/main/kotlin/com/lmspilot/course/api/CourseApi.kt")
        self.assertIn('file.ownerId != actorId', course)
        self.assertIn('file.purpose != "COURSE_CONTENT"', course)
        self.assertIn("fileAccess.requireAttachable", course)

    def test_assignment_grading_has_a_real_scoped_ui_queue(self) -> None:
        assignment = self.read("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/AssignmentSubmissionApi.kt")
        ui = self.read("apps/web/components/GradingPage.tsx")
        self.assertIn("ASSIGNMENT_QUEUE_CLASS_LIMIT", assignment)
        self.assertIn("classIds.forEach(::requireClassScope)", assignment)
        self.assertIn("/api/v1/learning/assignments/queue?", ui)
        self.assertIn("submitAssignmentGrade", ui)
        self.assertIn("returnForRevision", ui)

    def test_news_html_and_attachments_are_object_scoped(self) -> None:
        news = self.read("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NewsApi.kt")
        ui = self.read("apps/web/components/RealmControlCenter.tsx")
        self.assertIn("val canonical = value", news)
        self.assertIn('.replace("&amp;", "&", ignoreCase = true)', news)
        self.assertIn("val escaped = canonical", news)
        self.assertIn("val safeTag = Regex", news)
        self.assertIn("sanitizeNewsHtml(contentHtml)", news)
        self.assertIn("class NewsFileClient", news)
        self.assertIn('file.purpose != "NEWS_ATTACHMENT"', news)
        self.assertIn('source" to "NEWS_FEED"', news)
        self.assertIn("attachmentFileIds: newsAttachments.map", ui)
        self.assertIn("/api/v1/files?purpose=NEWS_ATTACHMENT", ui)

    def test_cross_service_urls_for_exact_file_and_course_checks_are_wired(self) -> None:
        compose = self.read("docker-compose.yml")
        assessment = compose[compose.index("  assessment-service:"):compose.index("  grading-service:")]
        course = compose[compose.index("  course-service:"):compose.index("  enrollment-service:")]
        notification = compose[compose.index("  notification-service:"):compose.index("  certificate-service:")]
        self.assertIn("COURSE_SERVICE_URL: http://course-service:8083", assessment)
        self.assertIn("FILE_STORAGE_SERVICE_URL: http://file-storage-service:8089", course)
        self.assertIn("FILE_STORAGE_SERVICE_URL: http://file-storage-service:8089", notification)

    def test_exam_result_polling_is_bounded_and_user_refreshable(self) -> None:
        ui = self.read("apps/web/components/ExamDetail.tsx")
        self.assertIn("async function refreshGrade", ui)
        self.assertIn("attempt < 5", ui)
        self.assertIn("Làm mới kết quả", ui)

    def test_domain_events_are_not_emitted_before_database_commit(self) -> None:
        events = self.read("backend/service-support/src/main/kotlin/com/lmspilot/support/events/EventSupport.kt")
        grading = self.read("backend/services/grading-service/src/main/kotlin/com/lmspilot/grading/api/GradingApi.kt")
        self.assertIn("TransactionSynchronizationManager.isActualTransactionActive()", events)
        self.assertIn("override fun afterCommit()", events)
        self.assertIn("if (existing.status == GradeStatus.COMPLETED) publish(existing)", grading)
        competition = grading[grading.index("private fun recordCompetitionResult"):grading.index("private fun manageableExamIds")]
        self.assertNotIn("runCatching", competition)


if __name__ == "__main__":
    unittest.main()
