from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class ClsV080RequirementTests(unittest.TestCase):
    def test_learning_paths_pin_class_course_versions_and_auto_enroll(self) -> None:
        domain = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/domain/LearningPathDomain.kt")
        api = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/api/LearningPathApi.kt")
        migration = text("backend/services/enrollment-service/src/main/resources/db/migration/V5__learning_paths.sql")
        ui = text("apps/web/components/LearningPathCenter.tsx")
        self.assertIn("courseVersion", domain)
        self.assertIn("LearningPathUnlockMode", domain)
        self.assertIn("enrollments.save", api)
        self.assertIn("LEARNING_PATH_ASSIGNED", api)
        self.assertIn("learning_path_items", migration)
        self.assertIn("Giao và ghi danh", ui)

    def test_learning_path_progress_uses_internal_token_and_sequential_unlock(self) -> None:
        path_api = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/api/LearningPathApi.kt")
        learning_api = text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/LearningApi.kt")
        self.assertIn("previousCompleted", path_api)
        self.assertIn("AFTER_PREVIOUS", path_api)
        self.assertIn('header("X-Service-Token"', path_api)
        self.assertIn('internal.require(token)', learning_api)
        self.assertIn('/users/{userId}/courses', learning_api)

    def test_learning_path_catalog_is_manager_only_and_progress_failure_does_not_regress_state(self) -> None:
        api = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/api/LearningPathApi.kt")
        config = text("backend/services/enrollment-service/src/main/resources/application.yml")
        self.assertIn("LEARNING_PATH_NOT_DRAFT", api)
        self.assertIn("LEARNING_PROGRESS_UNAVAILABLE", api)
        self.assertIn("participant.status = UserLearningPathStatus.CANCELLED", api)
        self.assertIn("SimpleClientHttpRequestFactory", api)
        self.assertIn("connect-timeout-ms", config)
        catalog_guard = "hasAnyAuthority('${Permissions.LEARNING_PATHS_MANAGE}','${Permissions.LEARNING_PATHS_ASSIGN}')"
        self.assertIn(catalog_guard, api)


    def test_kpi_reporting_is_scoped_and_aggregates_operational_metrics(self) -> None:
        api = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/KpiReportingApi.kt")
        ui = text("apps/web/components/AdvancedCenters.tsx")
        self.assertIn("completionRate", api)
        self.assertIn("passRate", api)
        self.assertIn("dueSoon", api)
        self.assertIn("ReportScope.ASSIGNED", api)
        self.assertIn("REPORTS_KPI_READ", api)
        self.assertIn('/api/v1/reports/kpis', ui)
        self.assertIn("Hiệu quả theo khóa học", ui)

    def test_kpi_queries_are_bounded_by_assigned_classes_and_reminder_errors_are_client_errors(self) -> None:
        kpi = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/KpiReportingApi.kt")
        reporting = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/domain/ReportingDomain.kt")
        reminder = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/ReminderReportingApi.kt")
        self.assertIn("findAllByClassIdIn", reporting)
        self.assertIn("readModels.findAllByClassIdIn(classIds)", kpi)
        self.assertIn("!CurrentUser.isSystemAdmin()", kpi)
        self.assertNotIn("CurrentUser.roles()", kpi)
        self.assertIn("Permissions.REPORTS_KPI_READ !in CurrentUser.authorities()", kpi)
        self.assertIn("REMINDER_WINDOW_INVALID", reminder)
        self.assertIn("HttpStatus.BAD_REQUEST", reminder)


    def test_due_reminder_source_is_read_model_and_internal_only(self) -> None:
        api = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/ReminderReportingApi.kt")
        repository = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/domain/ReportingDomain.kt")
        self.assertIn('/internal/v1/reports/reminders', api)
        self.assertIn("internal.require(token)", api)
        self.assertIn("32L * 86400L", api)
        self.assertIn("findAllByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThanOrderByDueAtAsc", repository)

    def test_notification_templates_override_business_events_without_rebuild(self) -> None:
        automation = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationAutomationApi.kt")
        consumer = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationApi.kt")
        migration = text("backend/services/notification-service/src/main/resources/db/migration/V5__notification_templates_and_reminders.sql")
        self.assertIn("SafeNotificationTemplateRenderer", automation)
        self.assertIn("findFirstByEventTypeAndActiveTrueOrderByUpdatedAtDesc", automation)
        self.assertIn("templates.resolve(event.eventType", consumer)
        self.assertIn("notification_templates", migration)
        self.assertIn("COURSE_DUE_REMINDER", migration)

    def test_reminder_dispatch_is_idempotent_and_releases_unavailable_channels(self) -> None:
        domain = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/domain/NotificationAutomationDomain.kt")
        automation = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationAutomationApi.kt")
        notification = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/domain/NotificationDomain.kt")
        self.assertIn("ON CONFLICT (rule_id, enrollment_id, due_at) DO NOTHING", domain)
        self.assertIn("dispatches.release", automation)
        self.assertIn("UUID.nameUUIDFromBytes", automation)
        self.assertIn("ON CONFLICT (source_event_id, user_id, channel) DO NOTHING", notification)

    def test_default_reminder_is_safe_and_disabled_until_review(self) -> None:
        migration = text("backend/services/notification-service/src/main/resources/db/migration/V5__notification_templates_and_reminders.sql")
        self.assertRegex(migration, r"7, 0, false,")
        self.assertIn("Safe starter assets", migration)

    def test_notification_automation_is_permissioned_audited_and_visible(self) -> None:
        permissions = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/Permissions.kt")
        api = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationAutomationApi.kt")
        shell = text("apps/web/components/CosmicShell.tsx")
        advanced = text("apps/web/components/AdvancedCenters.tsx")
        self.assertIn("NOTIFICATION_TEMPLATES_MANAGE", permissions)
        self.assertIn("NOTIFICATION_REMINDERS_MANAGE", permissions)
        self.assertIn("AUDIT_RECORDED", api)
        self.assertIn('/notification-automation', shell)
        self.assertIn("NotificationAutomationCenter", advanced)

    def test_backup_covers_competency_and_handles_runtime_secrets_explicitly(self) -> None:
        backup = text("scripts/backup.sh")
        restore = text("scripts/restore.sh")
        env = text(".env.example")
        self.assertIn("operations competency)", backup)
        self.assertIn("LMSPILOT_BACKUP_INCLUDE_SECRETS", backup)
        self.assertIn("SECRETS_NOT_INCLUDED.txt", backup)
        self.assertIn('manifest.get("formatVersion") != 1', restore)
        self.assertIn("LMSPILOT_BACKUP_INCLUDE_SECRETS=false", env)


    def test_migrations_add_indexes_for_due_reminders_and_widen_delivery_errors(self) -> None:
        self.assertIn("idx_report_due_incomplete", text("backend/services/reporting-service/src/main/resources/db/migration/V3__reminder_due_index.sql"))
        self.assertIn("varchar(2000)", text("backend/services/notification-service/src/main/resources/db/migration/V6__notification_delivery_error_length.sql"))

    def test_notification_event_type_values_match_contract_constants(self) -> None:
        contracts = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/DomainEvents.kt")
        ui = text("apps/web/components/AdvancedCenters.tsx")
        values = dict(re.findall(r'const val (ENROLLED|EXAM_GRADED|COURSE_COMPLETED|GRADE_APPEAL_RESOLVED|CERTIFICATE_ISSUED) = "([^"]+)"', contracts))
        self.assertEqual(len(values), 5)
        for value in values.values():
            self.assertIn(f'value="{value}"', ui)

    def test_reminder_worker_has_bounded_reporting_calls_and_failure_retry(self) -> None:
        automation = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationAutomationApi.kt")
        config = text("backend/services/notification-service/src/main/resources/application.yml")
        ui = text("apps/web/components/AdvancedCenters.tsx")
        self.assertIn("SimpleClientHttpRequestFactory", automation)
        self.assertIn("setConnectTimeout", automation)
        self.assertIn("setReadTimeout", automation)
        self.assertIn("retryDelaySeconds.coerceIn", automation)
        self.assertIn("log.error", automation)
        self.assertIn("reporting-connect-timeout-ms", config)
        self.assertIn("reminder-retry-delay-seconds", config)
        self.assertIn("Không thể xóa quy tắc", ui)


if __name__ == "__main__":
    unittest.main()
