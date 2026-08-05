from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def service_source(service: str) -> str:
    root = ROOT / "backend/services" / service / "src/main/java"
    return "\n".join(p.read_text(encoding="utf-8") for p in sorted(root.rglob("*.java")))


def contract_source() -> str:
    root = ROOT / "backend/platform-contracts/src/main/java"
    return "\n".join(p.read_text(encoding="utf-8") for p in sorted(root.rglob("*.java")))


class LmsPilotRequirementTraceTests(unittest.TestCase):
    def test_identity_has_exclusive_roles_and_protected_admin(self) -> None:
        source = service_source("identity-service")
        migration = read("backend/services/identity-service/src/main/resources/db/migration/V2__scoped_rbac_and_protected_admin.sql")
        for role in ("ADMIN", "INSTRUCTOR", "STUDENT"):
            self.assertIn(role, source)
        self.assertIn("protectedAccount", source)
        self.assertIn("uq_single_protected_system_admin", migration)
        self.assertNotRegex(source, r'@(PostMapping|RequestMapping)\([^\n]*(register|signup|sign-up)')

    def test_permission_catalog_and_default_profiles(self) -> None:
        source = contract_source()
        self.assertIn("DefaultAccessProfiles", source)
        for role in ("ADMIN", "INSTRUCTOR", "STUDENT"):
            self.assertIn(f'\"{role}\"', source)
        self.assertIn("PermissionCatalog", source)
        self.assertIn("DefaultRolePermissions", source)

    def test_identity_exposes_auth_user_role_and_authorization_apis(self) -> None:
        source = service_source("identity-service")
        for mapping in (
            '/api/v1/auth', '/api/v1/users', '/api/v1/roles', '/api/v1/authorization', '/internal/v1/authorization'
        ):
            self.assertIn(mapping, source)
        self.assertIn('/bulk', source)
        self.assertIn('scopeType', source)
        self.assertIn('scopeId', source)
        self.assertIn("denied", source)
        self.assertIn("allowed", source)

    def test_organization_supports_hierarchy_and_memberships(self) -> None:
        source = service_source("organization-service")
        for token in ("BRANCH", "DEPARTMENT", "GROUP", "materializedPath", "BulkMembershipRequest"):
            self.assertIn(token, source)
        self.assertIn("/api/v1/organization/units", source)
        self.assertIn("/api/v1/organization/memberships", source)

    def test_course_service_owns_courses_lessons_categories_discussions(self) -> None:
        source = service_source("course-service")
        for token in ("CourseEntity", "LessonEntity", "CourseCategoryEntity", "DiscussionThreadEntity"):
            self.assertIn(token, source)
        for mapping in ('/api/v1/courses', '/api/v1/categories', '/api/v1/discussions'):
            self.assertIn(mapping, source)
        self.assertNotIn('/api/v1/classes', source)

    def test_enrollment_service_assigns_courses_without_class_ui(self) -> None:
        source = service_source("enrollment-service")
        self.assertIn("CourseAssignmentEntity", source)
        self.assertIn("dueAt", source)
        self.assertIn("gracePeriodMinutes", source)
        self.assertIn("LiveSessionEntity", source)
        self.assertIn("LearningPathEntity", source)
        self.assertNotIn('/api/v1/classes', source)

    def test_learning_service_tracks_progress_assignments_and_xapi(self) -> None:
        source = service_source("learning-service")
        for token in ("progressPercent", "completedAt", "AssignmentSubmissionEntity", "XapiStatementEntity"):
            self.assertIn(token, source)
        for mapping in ('/api/v1/learning', '/api/v1/learning/assignments', '/api/v1/xapi/statements'):
            self.assertIn(mapping, source)
        self.assertIn('Idempotency-Key', source)

    def test_assessment_contexts_keep_course_quiz_and_standalone_exam_distinct(self) -> None:
        source = service_source("assessment-service")
        for context in ("COURSE_QUIZ", "COURSE_ASSIGNMENT", "STANDALONE_EXAM", "COMPETITION"):
            self.assertIn(context, source)
        self.assertIn("requires courseId", source)
        self.assertIn("must not reference a course", source)
        self.assertIn('/api/v1/exams', source)
        self.assertIn('/api/v1/competitions', source)

    def test_assessment_supports_question_bank_sessions_and_assignments(self) -> None:
        source = service_source("assessment-service")
        for mapping in ('/api/v1/questions', '/api/v1/exam-sessions', '/api/v1/assessment-assignments'):
            self.assertIn(mapping, source)
        for token in ("QuestionEntity", "ExamSessionEntity", "AssessmentAssignmentEntity"):
            self.assertIn(token, source)
        for audience in ("USER", "GROUP", "DEPARTMENT", "BRANCH"):
            self.assertIn(audience, source)

    def test_objective_scoring_and_manual_grading_are_separate(self) -> None:
        assessment = service_source("assessment-service")
        grading = service_source("grading-service")
        for kind in ("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE"):
            self.assertIn(kind, assessment)
        self.assertIn("PENDING_MANUAL", grading)
        self.assertIn("GradingQuestionPayload", grading)
        self.assertIn("answer", grading)

    def test_file_storage_supports_pdf_docx_versions_and_edit_sessions(self) -> None:
        source = service_source("file-storage-service")
        migration = read("backend/services/file-storage-service/src/main/resources/db/migration/V3__file_versions_and_edit_sessions.sql")
        for token in ("application/pdf", "wordprocessingml.document", "FileVersionEntity", "FileEditSessionEntity"):
            self.assertIn(token, source)
        self.assertIn("file_versions_v2", migration)
        self.assertIn("file_edit_sessions", migration)
        self.assertIn("PDF_ANNOTATION", migration)

    def test_ai_generation_is_document_grounded_and_reviewed(self) -> None:
        source = service_source("ai-service")
        schema = json.loads(read("contracts/lmspilot/question-set.schema.json"))
        self.assertEqual("https://json-schema.org/draft/2020-12/schema", schema["$schema"])
        for token in (
            "LOCAL_OPENAI_COMPATIBLE", "REMOTE_OPENAI_COMPATIBLE", "documentVersionIds",
            "citations", "DifficultyDistributionPolicy", "ReviewDecision", "APPROVE",
            "GeneratedQuestionQualityValidator",
        ):
            self.assertIn(token, source)
        self.assertIn('/question-generation-jobs/{id}/review', source)
        self.assertIn('/question-generation-jobs/{id}/import', source)

    def test_ai_difficulty_distribution_is_validated(self) -> None:
        source = service_source("ai-service")
        for level in ("EASY", "MEDIUM", "HARD"):
            self.assertIn(level, source)
        self.assertRegex(source, r"sum\(\).*100|total\s*!=\s*100|total\(\)\s*!=\s*100")
        self.assertIn("difficultyDistribution", source)

    def test_branding_supports_logo_and_login_background(self) -> None:
        source = service_source("configuration-service")
        for token in ("systemName", "logoFileId", "backgroundFileId", "customDomain"):
            self.assertIn(token, source)
        self.assertIn('/public/v1/branding', source)
        self.assertIn('/api/v1/branding', source)

    def test_notifications_include_news_templates_outbox_and_reminders(self) -> None:
        source = service_source("notification-service")
        for token in ("NewsArticleEntity", "NotificationTemplateEntity", "DeliveryStatus", "DEAD", "Reminder"):
            self.assertIn(token, source)
        for mapping in ('/api/v1/news', '/api/v1/notifications', '/api/v1/notifications/templates'):
            self.assertIn(mapping, source)

    def test_reporting_supports_dashboard_kpis_exports_and_schedules(self) -> None:
        source = service_source("reporting-service")
        for token in ("dashboard", "export.csv", "ReportExportJobEntity", "ReportScheduleEntity", "Kpi"):
            self.assertIn(token, source)
        self.assertIn('/api/v1/reports', source)
        self.assertIn('/api/v1/reports/kpis', source)

    def test_operations_use_claim_lease_and_allowlisted_agent(self) -> None:
        source = service_source("operations-service")
        agent = read("scripts/operations-agent.py")
        for token in ("claimToken", "heartbeat", "lease"):
            self.assertIn(token, source)
        self.assertIn("scripts/backup.sh", agent)
        self.assertIn("scripts/restore.sh", agent)
        self.assertNotIn("shell=True", agent)

    def test_license_and_audit_services_have_internal_apis(self) -> None:
        license_source = service_source("license-service")
        audit_source = service_source("audit-service")
        self.assertIn('/internal/v1/license', license_source)
        self.assertIn('/internal/v1/audit', audit_source)
        self.assertIn("LicenseEntitlements", license_source)
        self.assertIn("AuditEntryEntity", audit_source)

    def test_certificate_and_competency_services_are_independent(self) -> None:
        certificate = service_source("certificate-service")
        competency = service_source("competency-service")
        self.assertIn('/api/v1/certificates', certificate)
        self.assertIn('/public/v1/certificates', certificate)
        self.assertIn("CertificateTemplateEntity", certificate)
        self.assertIn('/api/v1/competencies', competency)
        self.assertIn("readinessPercent", competency)

    def test_integration_service_keeps_credentials_out_of_probe_payload(self) -> None:
        source = service_source("integration-service")
        self.assertIn("IntegrationAdapter", source)
        self.assertIn("CREDENTIALS_NOT_ALLOWED", source)
        self.assertIn("probeSocket", source)
        self.assertIn("SSLSocketFactory", source)

    def test_frontend_has_no_public_class_route(self) -> None:
        source = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "apps/web").rglob("*.tsx"))
        self.assertNotIn('href="/classes"', source)
        self.assertFalse((ROOT / "apps/web/app/classes").exists())

    def test_dark_theme_avoids_white_card_borders(self) -> None:
        css = read("apps/web/app/globals.css") + read("apps/web/app/unified.css")
        self.assertIn('[data-theme="unified-dark"]', css)
        self.assertIn("prefers-reduced-motion", css)
        self.assertIn("--border", css)


if __name__ == "__main__":
    unittest.main()
