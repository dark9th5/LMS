from pathlib import Path
import re
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Release023RuntimeContractTests(unittest.TestCase):
    def test_exam_attempt_uses_immutable_question_and_grading_snapshots(self):
        migration = text("backend/services/assessment-service/src/main/resources/db/migration/V9__immutable_exam_session_snapshot.sql")
        entity = text("backend/services/assessment-service/src/main/java/com/lmspilot/assessment/domain/ExamSessionEntity.java")
        service = text("backend/services/assessment-service/src/main/java/com/lmspilot/assessment/api/AssessmentManagementService.java")
        for column in (
            "questions_snapshot_json", "grading_snapshot_json", "passing_score_snapshot",
            "auto_grade_snapshot", "context_type_snapshot", "score_strategy_snapshot",
        ):
            self.assertIn(column, migration)
        for field in (
            "questionsSnapshotJson", "gradingSnapshotJson", "passingScoreSnapshot",
            "autoGradeSnapshot", "contextTypeSnapshot", "scoreStrategySnapshot",
        ):
            self.assertIn(field, entity)
        for marker in ("captureSessionSnapshot", "List<ExamQuestionView> candidate", "gradingSnapshotJson", "sessionQuestions"):
            self.assertIn(marker, service)

    def test_exam_start_rejects_unpublished_empty_or_expired_attempts(self):
        service = text("backend/services/assessment-service/src/main/java/com/lmspilot/assessment/api/AssessmentManagementService.java")
        ui = text("apps/web/components/ExamDetail.tsx")
        for marker in ("EXAM_NOT_OPEN", "EXAM_HAS_NO_QUESTIONS", "ExamSessionStatus.EXPIRED", "ATTEMPT_LIMIT"):
            self.assertIn(marker, service)
        for marker in ("validAttempt", "questions.length > 0", "serverRemaining", "exam-not-ready-panel"):
            self.assertIn(marker, ui)

    def test_assignment_submission_contract_matches_frontend_and_database(self):
        api = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/api/AssignmentSubmissionApi.java")
        entity = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/domain/AssignmentSubmissionEntity.java")
        migration = text("backend/services/learning-service/src/main/resources/db/migration/V2__course_version_and_assignments.sql")
        for marker in (
            "UUID enrollmentId", "UUID fileId", "Idempotency-Key", "ASSIGNMENT_SUBMISSION",
            "ASSIGNMENT_ATTEMPT_LIMIT", "completeGradedAssignmentLesson",
        ):
            self.assertIn(marker, api)
        for marker in ("enrollmentId", "lessonId", "attemptNumber", "fileId", "idempotencyKey"):
            self.assertIn(marker, entity)
        for column in ("enrollment_id", "lesson_id", "attempt_number", "file_id", "idempotency_key"):
            self.assertIn(column, migration)

    def test_learning_completion_is_event_driven_for_exams_and_assignments(self):
        learning = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/api/LearningApi.java")
        listener = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/api/LearningGradeEventListener.java")
        for marker in ("completionLessonIds", "requiredLessonIds", "Math.min(99", "COURSE_COMPLETED"):
            self.assertIn(marker, learning)
        for marker in ("EXAM_GRADED", "effectivePassed", "completePassedExamLesson"):
            self.assertIn(marker, listener)

    def test_xapi_accepts_string_object_urn_and_is_idempotent(self):
        api = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/api/XapiApi.java")
        entity = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/domain/XapiStatementEntity.java")
        self.assertIn("String objectId", api)
        self.assertIn("findById", api)
        self.assertIn("XAPI_STATEMENT_RECORDED", api)
        self.assertIn("String objectId", entity)

    def test_notification_feed_batches_attachments_and_receipts(self):
        api = text("backend/services/notification-service/src/main/java/com/lmspilot/notification/api/NewsApi.java")
        attachment_repo = text("backend/services/notification-service/src/main/java/com/lmspilot/notification/domain/NewsAttachmentRepository.java")
        receipt_repo = text("backend/services/notification-service/src/main/java/com/lmspilot/notification/domain/NewsReceiptRepository.java")
        self.assertIn("findAllByNewsIdInOrderByNewsIdAscSortOrderAsc", api)
        self.assertIn("findAllByNewsIdInAndUserId", api)
        self.assertIn("findAllByNewsIdInOrderByNewsIdAscSortOrderAsc", attachment_repo)
        self.assertIn("findAllByNewsIdInAndUserId", receipt_repo)

    def test_jpa_entities_match_the_repaired_learning_and_notification_columns(self):
        checks = {
            "backend/services/learning-service/src/main/java/com/lmspilot/learning/domain/CourseProgressEntity.java": [
                "enrollment_id", "course_id", "course_version", "progress_percent", "completion_event_published",
            ],
            "backend/services/learning-service/src/main/java/com/lmspilot/learning/domain/LessonProgressEntity.java": [
                "enrollment_id", "lesson_id", "completed_at", "learning_seconds",
            ],
            "backend/services/notification-service/src/main/java/com/lmspilot/notification/domain/NotificationEntity.java": [
                "user_id", "read_at", "created_at",
            ],
            "backend/services/notification-service/src/main/java/com/lmspilot/notification/domain/NewsArticleEntity.java": [
                "content_html", "audience_type", "acknowledgement_required", "published_at",
            ],
        }
        for path, columns in checks.items():
            source = text(path)
            for column in columns:
                self.assertIn(column, source, f"{path}: {column}")

    def test_client_data_layer_deduplicates_caches_and_bounds_requests(self):
        api = text("apps/web/lib/api.ts")
        upstream = text("apps/web/lib/upstream-fetch.ts")
        for marker in ("responseCache", "requestsInFlight", "requestTimeout", "invalidateApiCache", "controller.signal.aborted"):
            self.assertIn(marker, api)
        for marker in ("new AbortController()", "SERVER_UPSTREAM_TIMEOUT", "LMSPILOT_GATEWAY_URL"):
            self.assertIn(marker, upstream)

    def test_dialogs_and_command_palette_are_portaled_and_viewport_safe(self):
        modal = text("apps/web/components/Modal.tsx")
        shell = text("apps/web/components/AppShell.tsx")
        css = text("apps/web/app/unified.css")
        self.assertIn("createPortal", modal)
        self.assertIn("createPortal", shell)
        self.assertIn('body.classList.add("command-open")', shell)
        for marker in ("max-height:min(680px,calc(100dvh - 24px))", "scrollbar-gutter:stable", "max-height:calc(100dvh", "entity-form-actions"):
            self.assertIn(marker, css)

    def test_sidebar_footer_keeps_search_profile_and_logout_together(self):
        shell = text("apps/web/components/AppShell.tsx")
        footer = re.search(r'<div className="sidebar-footer">([\s\S]*?)</div>\s*</aside>', shell)
        self.assertIsNotNone(footer)
        block = footer.group(1)
        self.assertIn('className="sidebar-search"', block)
        self.assertIn('className="sidebar-profile"', block)
        self.assertIn('className="sidebar-logout-button"', block)
        css = text("apps/web/app/unified.css")
        self.assertIn("margin-top:auto", css)
        self.assertIn("height:100dvh", css)

    def test_admin_ai_supports_quick_install_local_endpoint_and_remote_key(self):
        ui = text("apps/web/components/AiConnectionCenter.tsx")
        api = text("backend/services/ai-service/src/main/java/com/lmspilot/ai/api/LocalAiRuntimeApi.java")
        for marker in (
            "Tải và tự thiết lập", "LOCAL_OPENAI_COMPATIBLE", "REMOTE_OPENAI_COMPATIBLE",
            "API key", "/api/v1/ai/local-runtime/pull",
        ):
            self.assertIn(marker, ui)
        for marker in ('@PostMapping("/local-runtime/pull")', 'Thread.ofVirtual()', '@PostMapping("/providers/{id}/test")'):
            self.assertIn(marker, api)

    def test_jpa_flyway_static_contract_scanner_passes(self):
        result = subprocess.run(
            [sys.executable, str(ROOT / "scripts/check-jpa-flyway-contracts.py")],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("76 JPA entities", result.stdout)

    def test_fast_start_and_login_smoke_checks_are_shipped(self):
        compose = text("docker-compose.yml")
        for marker in ("SPRING_THREADS_VIRTUAL_ENABLED", "UseG1GC", "condition: service_healthy", "OLLAMA_MANAGEMENT_URL"):
            self.assertIn(marker, compose)
        self.assertIn("--wait", text("scripts/start-fast.sh"))
        self.assertIn("--wait", text("scripts/start-fast.ps1"))
        self.assertTrue((ROOT / "scripts/smoke-login-roles.mjs").exists())
        self.assertTrue((ROOT / "scripts/performance-smoke.mjs").exists())


if __name__ == "__main__":
    unittest.main()
