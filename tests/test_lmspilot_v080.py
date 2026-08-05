from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(service: str) -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / f"backend/services/{service}/src/main/java").rglob("*.java"))


class Release080CapabilityTests(unittest.TestCase):
    def test_learning_paths_and_live_sessions_exist(self) -> None:
        enrollment = source("enrollment-service")
        self.assertIn("LearningPathEntity", enrollment)
        self.assertIn("LiveSessionEntity", enrollment)

    def test_competitions_and_rewards_exist(self) -> None:
        assessment = source("assessment-service")
        self.assertIn("CompetitionEntity", assessment)
        self.assertIn("CompetitionRewardEntity", assessment)
        self.assertIn("RewardLedgerEntity", assessment)

    def test_news_and_notifications_exist(self) -> None:
        notification = source("notification-service")
        self.assertIn("NewsArticleEntity", notification)
        self.assertIn("NotificationEntity", notification)

    def test_file_editing_and_versioning_exist(self) -> None:
        storage = source("file-storage-service")
        self.assertIn("FileVersionEntity", storage)
        self.assertIn("FileEditSessionEntity", storage)

    def test_ai_provider_configuration_exists(self) -> None:
        ai = source("ai-service")
        self.assertIn("AiProviderConfigEntity", ai)
        self.assertIn("LOCAL_OPENAI_COMPATIBLE", ai)

    def test_reporting_read_models_exist(self) -> None:
        reporting = source("reporting-service")
        self.assertIn("LearnerCourseReadModel", reporting)
        self.assertIn("ReportEventEntity", reporting)

    def test_audit_service_records_events(self) -> None:
        audit = source("audit-service")
        self.assertIn("AuditEntryEntity", audit)
        self.assertIn("/api/v1/audit", audit)

    def test_branding_and_external_configuration_exist(self) -> None:
        config = source("configuration-service")
        self.assertIn("BrandingProfileEntity", config)
        self.assertIn("ExternalServiceConfigEntity", config)


if __name__ == "__main__":
    unittest.main()
