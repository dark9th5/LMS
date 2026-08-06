from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Release023UiFlowPerformanceTests(unittest.TestCase):
    def test_version_is_0230(self):
        self.assertEqual("0.24.0", text("VERSION").strip())
        self.assertIn('"version": "0.24.0"', text("apps/web/package.json"))
        self.assertIn('version = "0.24.0"', text("backend/build.gradle.kts"))

    def test_modal_is_portaled_focus_trapped_and_viewport_stable(self):
        modal = text("apps/web/components/Modal.tsx")
        css = text("apps/web/app/unified.css")
        for marker in ["createPortal", "FOCUSABLE", "modal-open", "previousFocus", 'event.key !== "Tab"']:
            self.assertIn(marker, modal)
        for marker in ["100dvh", "overscroll-behavior:contain", "entity-form-actions", "form>.modal-actions:last-child"]:
            self.assertIn(marker, css)

    def test_sidebar_search_and_logout_are_anchored_in_footer(self):
        shell = text("apps/web/components/AppShell.tsx")
        css = text("apps/web/app/unified.css")
        self.assertIn('className="sidebar-footer"', shell)
        self.assertIn('className="sidebar-search"', shell)
        self.assertIn('className="sidebar-logout-button"', shell)
        self.assertIn("margin-top:auto", css)
        self.assertIn("height:100dvh", css)

    def test_exam_start_never_accepts_empty_or_expired_attempt(self):
        ui = text("apps/web/components/ExamDetail.tsx")
        service = text("backend/services/assessment-service/src/main/java/com/lmspilot/assessment/api/AssessmentManagementService.java")
        models = text("backend/services/assessment-service/src/main/java/com/lmspilot/assessment/api/AssessmentModels.java")
        for marker in ["validAttempt", "serverRemaining", "sessionReadyAt", "questions.length > 0"]:
            self.assertIn(marker, ui)
        self.assertIn("remainingSeconds", models)
        self.assertIn("EXAM_HAS_NO_QUESTIONS", service)
        self.assertIn("attempt.status=ExamSessionStatus.EXPIRED", service)

    def test_course_completion_uses_full_course_denominator(self):
        learning = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/api/LearningApi.java")
        listener = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/api/LearningGradeEventListener.java")
        for marker in ["learning-metadata", "completionLessonIds", "Math.min(99", "COURSE_COMPLETED"]:
            self.assertIn(marker, learning)
        self.assertIn("effectivePassed", listener)
        self.assertIn("EXAM_GRADED", listener)

    def test_api_timeout_does_not_retry_an_aborted_request(self):
        api = text("apps/web/lib/api.ts")
        self.assertIn("A timeout must fail immediately", api)
        self.assertIn("controller.signal.aborted", api)
        self.assertIn("error instanceof TypeError", api)
        self.assertIn("300_000", api)

    def test_list_endpoints_batch_related_rows(self):
        assessment = text("backend/services/assessment-service/src/main/java/com/lmspilot/assessment/api/AssessmentManagementService.java")
        learning_repo = text("backend/services/learning-service/src/main/java/com/lmspilot/learning/domain/LessonProgressRepository.java")
        identity = text("backend/services/identity-service/src/main/java/com/lmspilot/identity/service/AuthorizationService.java")
        self.assertIn("findAllByExamIdInOrderByExamIdAscSortOrderAsc", assessment)
        self.assertIn("findAllByEnrollmentIdInOrderByUpdatedAtAsc", learning_repo)
        self.assertIn("permissionsForTokens", identity)

    def test_fast_start_waits_for_a_usable_system(self):
        compose = text("docker-compose.yml")
        self.assertIn("SPRING_THREADS_VIRTUAL_ENABLED", compose)
        self.assertIn("UseG1GC", compose)
        self.assertNotIn("TieredStopAtLevel=1", compose)
        self.assertIn("learning-service: { condition: service_healthy }", compose)
        self.assertIn("--wait", text("scripts/start-fast.sh"))
        self.assertIn("--wait", text("scripts/start-fast.ps1"))

    def test_login_and_logout_are_timeout_bound(self):
        logout = text("apps/web/app/api/auth/logout/route.ts")
        password = text("apps/web/app/api/auth/change-password/route.ts")
        self.assertIn("fetchGateway", logout)
        self.assertIn("fetchGateway", password)
        auth = text("backend/services/identity-service/src/main/java/com/lmspilot/identity/service/AuthService.java")
        self.assertGreaterEqual(auth.count("noRollbackFor=ApiException.class"), 2)


if __name__ == "__main__":
    unittest.main()
