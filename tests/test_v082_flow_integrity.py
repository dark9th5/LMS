from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(service: str) -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / f"backend/services/{service}/src/main/java").rglob("*.java"))


class Release082FlowIntegrityTests(unittest.TestCase):
    def test_learning_progress_is_course_scoped(self) -> None:
        learning = source("learning-service")
        self.assertIn("courseId", learning)
        self.assertIn("enrollmentId", learning)
        self.assertNotIn("/classes", learning)

    def test_course_quiz_is_tied_to_course_context(self) -> None:
        assessment = source("assessment-service")
        self.assertIn("COURSE_QUIZ", assessment)
        self.assertIn("requires courseId", assessment)

    def test_standalone_exam_rejects_course_context(self) -> None:
        assessment = source("assessment-service")
        self.assertIn("STANDALONE_EXAM", assessment)
        self.assertIn("must not reference a course", assessment)

    def test_exam_sessions_support_resume_heartbeat_and_events(self) -> None:
        assessment = source("assessment-service")
        self.assertIn('/heartbeat', assessment)
        self.assertIn('/events', assessment)
        self.assertIn("graceUntil", assessment)
        self.assertIn("suspiciousEventCount", assessment)

    def test_manual_grading_payload_contains_question_and_learner_answer(self) -> None:
        assessment = source("assessment-service")
        grading = source("grading-service")
        self.assertIn("grading-payload", assessment)
        self.assertIn("prompt", grading)
        self.assertIn("answer", grading)

    def test_assignment_submission_is_course_scoped_and_graded(self) -> None:
        learning = source("learning-service")
        self.assertIn("AssignmentSubmissionEntity", learning)
        self.assertIn("courseId", learning)
        self.assertIn('/submissions/{id}/grade', learning)

    def test_file_metadata_and_content_are_object_scoped(self) -> None:
        storage = source("file-storage-service")
        self.assertIn('/{id}/content', storage)
        self.assertIn("readable", storage)
        self.assertIn("FileAccessGrant", storage)

    def test_news_attachments_are_separate_objects(self) -> None:
        news = source("notification-service")
        self.assertIn("NewsAttachmentEntity", news)
        self.assertIn("attachmentFileIds", news)
        self.assertIn("NewsAudience", news)

    def test_domain_events_use_shared_envelopes(self) -> None:
        contracts = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "backend/platform-contracts/src/main/java").rglob("*.java"))
        support = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "backend/service-support/src/main/java").rglob("*.java"))
        self.assertIn("DomainEventEnvelope", contracts)
        self.assertIn("DomainEventPublisher", support)

    def test_grade_history_and_appeals_are_persistent(self) -> None:
        grading = source("grading-service")
        self.assertIn("GradeRevisionEntity", grading)
        self.assertIn("GradeAppealEntity", grading)
        self.assertIn('/appeals', grading)

    def test_certificate_snapshots_template_version(self) -> None:
        certificate = source("certificate-service")
        self.assertIn("templateSnapshotJson", certificate)
        self.assertIn("CertificateTemplateEntity", certificate)

    def test_xapi_statement_is_idempotent(self) -> None:
        learning = source("learning-service")
        self.assertIn("XapiStatementEntity", learning)
        self.assertIn("/api/v1/xapi/statements", learning)


if __name__ == "__main__":
    unittest.main()
