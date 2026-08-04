from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


class ClsRequirementTraceTests(unittest.TestCase):
    def test_two_account_types_and_protected_bootstrap_admin(self) -> None:
        model = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/domain/Entities.kt")
        migration = text("backend/services/identity-service/src/main/resources/db/migration/V2__scoped_rbac_and_protected_admin.sql")
        self.assertIn("enum class AccountType { SYSTEM_ADMIN, USER }", model)
        self.assertIn("protectedAccount", model)
        self.assertIn("uq_single_protected_system_admin", migration)

    def test_no_public_registration_endpoint(self) -> None:
        sources = "\n".join(
            p.read_text(encoding="utf-8")
            for p in (ROOT / "backend/services/identity-service/src/main/kotlin").rglob("*.kt")
        )
        self.assertNotRegex(sources, r'@(PostMapping|RequestMapping)\([^\n]*(register|signup|sign-up)')
        self.assertFalse((ROOT / "apps/web/app/register").exists())
        self.assertFalse((ROOT / "apps/web/app/signup").exists())

    def test_default_roles_custom_roles_and_bulk_account_creation(self) -> None:
        permissions = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/Permissions.kt")
        users = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Controllers.kt")
        self.assertIn("val ADMIN", permissions)
        self.assertIn("val INSTRUCTOR", permissions)
        self.assertIn("val LEARNER", permissions)
        self.assertIn('@PostMapping("/bulk")', users)
        models = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Models.kt")
        self.assertIn("operationId", models)
        self.assertIn("RoleRequest", models)

    def test_scoped_bulk_rbac_and_deny_precedence(self) -> None:
        controllers = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Controllers.kt")
        models = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Models.kt")
        service = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/AuthorizationService.kt")
        self.assertIn('@PostMapping("/grants/bulk")', controllers)
        self.assertIn("BulkGrantRequest", models)
        self.assertIn("scopeType", models)
        self.assertIn("scopeId", models)
        self.assertIn("val denied", service)
        self.assertIn("return allowed && !denied", service)
        self.assertIn("Base account roles are intentionally excluded", service)

    def test_organization_hierarchy_membership_and_scoped_enforcement(self) -> None:
        organization = text("backend/services/organization-service/src/main/kotlin/com/lmspilot/organization/api/OrganizationApi.kt")
        membership = text("backend/services/organization-service/src/main/kotlin/com/lmspilot/organization/api/MembershipApi.kt")
        self.assertIn("BRANCH", organization)
        self.assertIn("DEPARTMENT", organization)
        self.assertIn("GROUP", organization)
        self.assertIn("materializedPath", organization)
        self.assertIn("BulkMembershipRequest", membership)
        self.assertIn("requireUnitPermission", membership)
        self.assertIn("ORGANIZATION_MEMBERSHIP_OUT_OF_SCOPE", membership)

    def test_course_class_assignment_deadline_progress_and_live_learning(self) -> None:
        enrollment = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/api/AssignmentAndLiveApi.kt")
        learning = text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/LearningApi.kt")
        self.assertIn("CreateCourseAssignmentRequest", enrollment)
        self.assertIn("dueAt", enrollment)
        self.assertIn("gracePeriodMinutes", enrollment)
        self.assertIn("CreateLiveSessionRequest", enrollment)
        self.assertIn("progressPercent", learning)
        self.assertIn("completedAt", learning)

    def test_course_quiz_standalone_exam_and_competition_are_distinct(self) -> None:
        policy = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/cls/AssessmentContextPolicy.kt")
        competition = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/CompetitionApi.kt")
        self.assertIn("COURSE_QUIZ", policy)
        self.assertIn("COURSE_ASSIGNMENT", policy)
        self.assertIn("STANDALONE_EXAM", policy)
        self.assertIn("COMPETITION", policy)
        self.assertIn("CompetitionRanker", competition)
        self.assertIn("issueRewardsInternal", competition)
        self.assertIn("existsByCompetitionIdAndUserIdAndRewardId", competition)

    def test_standalone_exam_audience_supports_user_and_org_scopes(self) -> None:
        assignment = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentAssignmentApi.kt")
        migration = text("backend/services/assessment-service/src/main/resources/db/migration/V5__assessment_assignments.sql")
        gateway = text("backend/services/api-gateway/src/main/resources/application.yml")
        self.assertIn("USER, GROUP, DEPARTMENT, BRANCH", text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/domain/AssessmentAssignmentDomain.kt"))
        self.assertIn("assessment_assignments", migration)
        self.assertIn("isEligible", assignment)
        self.assertIn("no assignment rows is open", assignment)
        self.assertIn("/api/v1/assessment-assignments/**", gateway)

    def test_objective_questions_are_auto_graded(self) -> None:
        scoring = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/cls/ObjectiveScoring.kt")
        grading = text("backend/services/grading-service/src/main/kotlin/com/lmspilot/grading/api/GradingApi.kt")
        self.assertIn("SINGLE_CHOICE", scoring)
        self.assertIn("MULTIPLE_CHOICE", scoring)
        self.assertIn("TRUE_FALSE", scoring)
        self.assertIn("PENDING_MANUAL", grading)
        self.assertIn("matches(answer", grading)

    def test_branding_news_and_optional_external_services(self) -> None:
        customization = text("backend/services/configuration-service/src/main/kotlin/com/lmspilot/configuration/api/CustomizationApi.kt")
        news = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NewsApi.kt")
        compose = text("docker-compose.yml")
        self.assertIn("systemName", customization)
        self.assertIn("logoFileId", customization)
        self.assertIn("backgroundColor", customization)
        self.assertIn("customDomain", customization)
        self.assertIn("ExternalService", customization)
        self.assertIn("News", news)
        self.assertRegex(compose, r"profiles:\s*\[redis\]")
        self.assertIn("profiles: [extended]", compose)

    def test_ai_question_generation_has_local_api_and_common_schema(self) -> None:
        api = text("backend/services/ai-service/src/main/kotlin/com/lmspilot/ai/api/QuestionGenerationApi.kt")
        contracts = text("backend/services/ai-service/src/main/kotlin/com/lmspilot/ai/cls/QuestionGenerationContracts.kt")
        schema_path = ROOT / "contracts/cls/question-set.schema.json"
        self.assertTrue(schema_path.exists())
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        self.assertEqual(schema.get("$schema"), "https://json-schema.org/draft/2020-12/schema")
        self.assertIn("LOCAL_OPENAI_COMPATIBLE", contracts)
        self.assertIn("REMOTE_OPENAI_COMPATIBLE", contracts)
        self.assertIn("citations", contracts)
        self.assertIn("documentVersionIds", contracts)
        self.assertIn("ReviewDecision.APPROVE", api)

    def test_docx_pdf_editing_is_versioned_and_permission_guarded(self) -> None:
        editing = text("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileEditingApi.kt")
        migration = text("backend/services/file-storage-service/src/main/resources/db/migration/V3__file_versions_and_edit_sessions.sql")
        self.assertIn("DOCX", editing)
        self.assertIn("PDF", editing)
        self.assertIn("FILES_EDIT", editing)
        self.assertIn("file_versions_v2", migration)
        self.assertIn("file_edit_sessions", migration)
        self.assertIn("PDF_ANNOTATION", migration)

    def test_fantasy_ui_keeps_accessibility_and_reduced_motion(self) -> None:
        css = text("apps/web/app/globals.css")
        shell = text("apps/web/components/PortalShell.tsx")
        login = text("apps/web/app/login/LoginForm.tsx")
        self.assertIn("prefers-reduced-motion", css)
        self.assertIn("prefers-contrast", css)
        self.assertIn(":focus-visible", css)
        self.assertIn("MysticBackdrop", shell)
        self.assertIn("portal", login.lower())

    def test_competition_only_permission_and_registration_window(self) -> None:
        assessment = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        competition = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/CompetitionApi.kt")
        self.assertIn("COMPETITIONS_PARTICIPATE", assessment)
        self.assertIn("canParticipateCompetitions() && audience.isEligible", assessment)
        self.assertIn("COMPETITION_REGISTRATION_CLOSED", assessment)
        self.assertIn("registrationOpensAt", competition)
        self.assertIn("registrationClosesAt", competition)

    def test_learner_live_sessions_use_private_schedule_endpoint(self) -> None:
        api = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/api/AssignmentAndLiveApi.kt")
        ui = text("apps/web/components/RealmControlCenter.tsx")
        self.assertIn('@GetMapping("/me")', api)
        self.assertIn("fun mine() = service.myLiveSessions()", api)
        self.assertIn("LIVE_SESSIONS_JOIN", api)
        self.assertIn('/api/v1/live-sessions/me', ui)

    def test_ai_generation_is_course_scoped_and_schema_is_packaged(self) -> None:
        api = text("backend/services/ai-service/src/main/kotlin/com/lmspilot/ai/api/QuestionGenerationApi.kt")
        packaged = ROOT / "backend/services/ai-service/src/main/resources/schemas/question-set.schema.json"
        canonical = ROOT / "contracts/cls/question-set.schema.json"
        self.assertIn("ScopedAuthorizationClient", api)
        self.assertIn("requireCoursePermission(input.courseId", api)
        self.assertIn("GENERATION_JOB_OUT_OF_SCOPE", api)
        self.assertTrue(packaged.exists())
        self.assertEqual(json.loads(packaged.read_text(encoding="utf-8")), json.loads(canonical.read_text(encoding="utf-8")))
        self.assertFalse(json.loads(packaged.read_text(encoding="utf-8"))["additionalProperties"])

    def test_ai_service_is_core_but_model_provider_remains_optional(self) -> None:
        compose = text("docker-compose.yml")
        ai_block = compose.split("\n  ai-service:\n", 1)[1].split("\n  configuration-service:\n", 1)[0]
        self.assertNotIn("profiles:", ai_block)
        self.assertIn("AI_ENABLED: ${AI_ENABLED:-false}", ai_block)
        self.assertIn("AI_BASE_URL", ai_block)
        self.assertRegex(compose, r"profiles:\s*\[redis\]")

    def test_default_instructor_can_manage_classes_and_enrollments(self) -> None:
        catalog = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/Permissions.kt")
        instructor = catalog.split("val INSTRUCTOR = setOf(", 1)[1].split("\n    )", 1)[0]
        self.assertIn("Permissions.CLASSES_WRITE", instructor)
        self.assertIn("Permissions.ENROLLMENTS_WRITE", instructor)
        self.assertIn("Permissions.LIVE_SESSIONS_MANAGE", instructor)

    def test_competition_ui_uses_valid_visibility_and_exam_route(self) -> None:
        ui = text("apps/web/components/RealmControlCenter.tsx")
        self.assertIn("ADMIN_ONLY", ui)
        self.assertNotIn("AFTER_PUBLISH", ui)
        self.assertIn('href={`/exams/${active.id}`}', ui)
        self.assertIn('stem: string', ui)

    def test_runtime_secrets_are_generated_not_hard_coded(self) -> None:
        setup = text("scripts/setup.sh")
        preflight = text("scripts/preflight.sh")
        ai_config = text("backend/services/ai-service/src/main/resources/application.yml")
        config_config = text("backend/services/configuration-service/src/main/resources/application.yml")
        self.assertIn("AI_SECRET_KEY", setup)
        self.assertIn("CONFIGURATION_SECRET_KEY", setup)
        self.assertIn("AI_SECRET_KEY", preflight)
        self.assertNotIn("change-me", ai_config)
        self.assertNotIn("change-me", config_config)


    def test_ba_user_import_supports_csv_xlsx_preview_and_row_policies(self) -> None:
        service = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/UserImportService.kt")
        controller = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Controllers.kt")
        wizard = text("apps/web/components/UserImportWizard.tsx")
        self.assertIn("parseCsv", service)
        self.assertIn("parseXlsx", service)
        self.assertIn("MAX_IMPORT_ROWS", service)
        self.assertIn("UserImportFailurePolicy.ATOMIC", service)
        self.assertIn("PROPAGATION_REQUIRES_NEW", service)
        self.assertIn("existingActiveUnitIds", service)
        self.assertIn('@PostMapping("/import/inspect"', controller)
        self.assertIn('@PostMapping("/import/preview"', controller)
        self.assertIn('@PostMapping("/import/commit"', controller)
        self.assertIn("Ánh xạ dữ liệu", wizard)
        self.assertIn("Nhập các dòng hợp lệ", wizard)

    def test_license_enforces_capacity_features_grace_and_secure_default(self) -> None:
        api = text("backend/services/license-service/src/main/kotlin/com/lmspilot/license/api/LicenseApi.kt")
        guard = text("backend/service-support/src/main/kotlin/com/lmspilot/support/security/LicenseGuard.kt")
        capacity = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/UserCapacityService.kt")
        repositories = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/domain/Repositories.kt")
        identity_lock = text("backend/services/identity-service/src/main/resources/db/migration/V3__identity_system_locks.sql")
        config = text("backend/services/license-service/src/main/resources/application.yml")
        self.assertIn("GRACE_PERIOD", api)
        self.assertIn("readOnly", api)
        self.assertIn("maxUsers", api)
        self.assertIn("requireFeature", guard)
        self.assertIn("validateEnabledFeatures", guard)
        self.assertIn("requireCapacity", capacity)
        self.assertIn("PESSIMISTIC_WRITE", repositories)
        self.assertIn("identity_system_locks", identity_lock)
        self.assertIn("LMSPILOT_ALLOW_DEVELOPMENT_LICENSE:false", config)
        self.assertIn("if (allowDevelopment) developmentLicense() else missingLicense()", api)
        self.assertIn("status = LicenseStatus.INVALID", api)
        self.assertIn("readOnly = true", api)

    def test_course_versions_are_immutable_and_pinned_to_enrollment(self) -> None:
        course = text("backend/services/course-service/src/main/kotlin/com/lmspilot/course/api/CourseApi.kt")
        enrollment = text("backend/services/enrollment-service/src/main/kotlin/com/lmspilot/enrollment/api/EnrollmentApi.kt")
        course_migration = text("backend/services/course-service/src/main/resources/db/migration/V4__immutable_course_versions.sql")
        learning_migration = text("backend/services/learning-service/src/main/resources/db/migration/V2__course_version_and_assignments.sql")
        self.assertIn("CourseSnapshot", course)
        self.assertIn("IMMUTABLE_COURSE_VERSION", course)
        self.assertIn("accessibleVersions", course)
        self.assertIn("version !in enrollmentScope.accessibleVersions", course)
        self.assertIn("course.status != CourseStatus.PUBLISHED", course)
        self.assertIn("return snapshotAt(course, pinnedVersion)", course)
        self.assertIn("accessibleCourseVersions", enrollment)
        self.assertIn("course_versions", course_migration)
        self.assertIn("course_version", learning_migration)

    def test_assignment_submission_is_a_dedicated_scoped_aggregate(self) -> None:
        api = text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/AssignmentSubmissionApi.kt")
        migration = text("backend/services/learning-service/src/main/resources/db/migration/V2__course_version_and_assignments.sql")
        file_api = text("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/InternalFileApi.kt")
        self.assertIn("AssignmentSubmissionEntity", api)
        self.assertIn("ASSIGNMENT_SUBMISSION", api)
        self.assertIn("ASSIGNMENT_FILE_OWNER_MISMATCH", api)
        self.assertIn("ASSIGNMENT_ATTEMPT_LIMIT", api)
        self.assertIn("requireClassScope", api)
        self.assertIn("uq_assignment_submission_idempotency", migration)
        self.assertIn("lockAttemptSequence", api)
        self.assertIn("pg_advisory_xact_lock", text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/domain/LearningDomain.kt"))
        self.assertIn('@GetMapping("/{id}")', file_api)

    def test_email_outbox_has_retry_dead_letter_and_crash_recovery_lease(self) -> None:
        api = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/api/NotificationApi.kt")
        domain = text("backend/services/notification-service/src/main/kotlin/com/lmspilot/notification/domain/NotificationDomain.kt")
        recovery = text("backend/services/notification-service/src/main/resources/db/migration/V4__recover_stale_email_leases.sql")
        config = text("backend/services/notification-service/src/main/resources/application.yml")
        self.assertIn("DeliveryStatus.DEAD", api)
        self.assertIn("backoff", api)
        self.assertIn("emailProcessingLease", api)
        self.assertIn("nextAttemptAt = leaseUntil", api)
        self.assertIn("'PROCESSING'", domain)
        self.assertIn("Recovered after an interrupted email delivery", recovery)
        self.assertIn("NOTIFICATION_EMAIL_PROCESSING_LEASE", config)

    def test_operations_run_only_through_a_leased_allowlisted_host_agent(self) -> None:
        api = text("backend/services/operations-service/src/main/kotlin/com/lmspilot/operations/api/OperationsApi.kt")
        domain = text("backend/services/operations-service/src/main/kotlin/com/lmspilot/operations/domain/OperationsDomain.kt")
        agent = text("scripts/operations-agent.py")
        self.assertIn("claimToken", api)
        self.assertIn("heartbeat", api)
        self.assertIn("agentLease", api)
        self.assertIn("FOR UPDATE SKIP LOCKED", domain)
        self.assertIn("scripts/backup.sh", agent)
        self.assertIn("scripts/restore.sh", agent)
        self.assertIn("arbitrary shell execution is disabled", agent)
        self.assertNotIn("shell=True", agent)

    def test_ldap_authentication_preserves_local_bootstrap_access(self) -> None:
        auth = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/AuthService.kt")
        ldap = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/LdapAuthenticationService.kt")
        config = text("backend/services/identity-service/src/main/resources/application.yml")
        self.assertIn("localAuthenticated", auth)
        self.assertIn("!user.protectedAccount", auth)
        self.assertIn("LOGIN_SUCCESS_LDAP", auth)
        self.assertIn("user-dn-pattern", config)
        self.assertIn("escapeFilter", ldap)
        self.assertIn("connect.timeout", ldap)
        self.assertIn('requireFeature("LDAP", write = false)', ldap)

    def test_adapter_connection_probe_keeps_credentials_out_of_endpoint(self) -> None:
        api = text("backend/services/integration-service/src/main/kotlin/com/lmspilot/integration/api/IntegrationApi.kt")
        self.assertIn("testEndpoint", api)
        self.assertIn("probeSocket", api)
        self.assertIn("SSLSocketFactory", api)
        self.assertIn("CREDENTIALS_NOT_ALLOWED", api)
        self.assertIn("transport-level probe", api)

    def test_report_export_is_controlled_by_license(self) -> None:
        api = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/ReportingApi.kt")
        self.assertIn('requireFeature("REPORT_EXPORT", write = false)', api)
        self.assertIn("Prevent spreadsheet formula execution", api)

    def test_user_import_serializes_operation_ids_and_limits_xlsx_expansion(self) -> None:
        service = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/UserImportService.kt")
        guard = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/BulkOperationGuard.kt")
        migration = text("backend/services/identity-service/src/main/resources/db/migration/V5__bulk_operation_serialization.sql")
        self.assertIn("IDENTITY_BULK_OPERATION_SERIALIZATION", migration)
        self.assertIn("MAX_XLSX_ENTRY_BYTES", service)
        self.assertIn("MAX_XLSX_TOTAL_BYTES", service)
        self.assertIn('bulkGuard.replay(operationId, "USER_FILE_IMPORT", requestedBy)', service)
        self.assertIn("locks.lock(BULK_OPERATION_LOCK)", guard)
        self.assertIn("capacityDelta", service)
        self.assertIn("saveOperation(operationId, rejected)", service)


if __name__ == "__main__":
    unittest.main()

class ClsV070RequirementTests(unittest.TestCase):
    def test_competency_service_supports_catalog_profiles_gaps_and_readiness(self) -> None:
        api = text("backend/services/competency-service/src/main/kotlin/com/lmspilot/competency/api/CompetencyApi.kt")
        domain = text("backend/services/competency-service/src/main/kotlin/com/lmspilot/competency/domain/CompetencyDomain.kt")
        compose = text("docker-compose.yml")
        gateway = text("backend/services/api-gateway/src/main/resources/application.yml")
        self.assertIn("CompetencyEntity", domain)
        self.assertIn("UserCompetencyAssessmentEntity", domain)
        self.assertIn("GapRow", api)
        self.assertIn("readinessPercent", api)
        self.assertIn("competency-service:", compose)
        self.assertIn("id: competency", gateway)

    def test_password_policy_sessions_and_forced_password_change_are_enforced(self) -> None:
        auth = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Controllers.kt")
        service = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/AuthService.kt")
        filter_text = text("backend/services/api-gateway/src/main/kotlin/com/lmspilot/gateway/GatewayFilters.kt")
        migration = text("backend/services/identity-service/src/main/resources/db/migration/V4__password_policy_and_sessions.sql")
        self.assertIn("change-password", auth)
        self.assertIn("sessions", auth)
        self.assertIn("passwordPolicy.change", service)
        self.assertIn("failedLoginCount", service)
        self.assertIn("password_history", migration)
        self.assertIn("mustChangePassword", filter_text)

    def test_xapi_lrs_records_idempotent_learning_statements(self) -> None:
        api = text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/XapiApi.kt")
        migration = text("backend/services/learning-service/src/main/resources/db/migration/V3__xapi_learning_record_store.sql")
        gateway = text("backend/services/api-gateway/src/main/resources/application.yml")
        self.assertIn('@RequestMapping("/api/v1/xapi/statements")', api)
        self.assertIn("statementId", api)
        self.assertIn("XAPI_STATEMENT_RECORDED", api)
        self.assertIn("xapi_statements", migration)
        self.assertIn("/api/v1/xapi/**", gateway)

    def test_course_discussions_are_scoped_and_moderated(self) -> None:
        api = text("backend/services/course-service/src/main/kotlin/com/lmspilot/course/api/DiscussionApi.kt")
        migration = text("backend/services/course-service/src/main/resources/db/migration/V5__course_discussions.sql")
        ui = text("apps/web/components/CourseDetail.tsx")
        self.assertIn("requireRead", api)
        self.assertIn("DISCUSSIONS_MODERATE", api)
        self.assertIn("THREAD_LOCKED", api)
        self.assertIn("discussion_threads", migration)
        self.assertIn("CourseDiscussion", ui)

    def test_grade_history_and_appeals_are_auditable(self) -> None:
        api = text("backend/services/grading-service/src/main/kotlin/com/lmspilot/grading/api/GradingApi.kt")
        domain = text("backend/services/grading-service/src/main/kotlin/com/lmspilot/grading/domain/GradingDomain.kt")
        migration = text("backend/services/grading-service/src/main/resources/db/migration/V3__grade_history_and_appeals.sql")
        self.assertIn("GradeRevisionEntity", domain)
        self.assertIn("GradeAppealEntity", domain)
        self.assertIn('@PostMapping("/{id}/appeals")', api)
        self.assertIn("GRADE_APPEAL_RESOLVED", api)
        self.assertIn("grade_revisions", migration)

    def test_exam_sessions_support_resume_heartbeat_grace_and_security_events(self) -> None:
        api = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt")
        domain = text("backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/domain/AssessmentDomain.kt")
        migration = text("backend/services/assessment-service/src/main/resources/db/migration/V6__resumable_exam_sessions.sql")
        self.assertIn('@GetMapping("/{id}")', api)
        self.assertIn('@PostMapping("/{id}/heartbeat")', api)
        self.assertIn('@PostMapping("/{id}/events")', api)
        self.assertIn("graceUntil", domain)
        self.assertIn("suspiciousEventCount", domain)
        self.assertIn("exam_session_events", migration)

    def test_certificate_templates_are_versioned_into_issued_certificates(self) -> None:
        api = text("backend/services/certificate-service/src/main/kotlin/com/lmspilot/certificate/api/CertificateApi.kt")
        domain = text("backend/services/certificate-service/src/main/kotlin/com/lmspilot/certificate/domain/CertificateDomain.kt")
        migration = text("backend/services/certificate-service/src/main/resources/db/migration/V2__certificate_templates.sql")
        self.assertIn("CertificateTemplateEntity", domain)
        self.assertIn("templateSnapshotJson", domain)
        self.assertIn('@GetMapping("/templates")', api)
        self.assertIn("snapshot", api.lower())
        self.assertIn("certificate_templates", migration)

    def test_scheduled_report_exports_are_background_and_scoped(self) -> None:
        api = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/ScheduledReportingApi.kt")
        domain = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/domain/ReportingDomain.kt")
        migration = text("backend/services/reporting-service/src/main/resources/db/migration/V2__scheduled_report_exports.sql")
        app = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/ReportingServiceApplication.kt")
        self.assertIn("ReportExportJobEntity", domain)
        self.assertIn("ReportScheduleEntity", domain)
        self.assertIn("@Scheduled", api)
        self.assertIn("REPORT_EXPORT", api)
        self.assertIn("report_export_jobs", migration)
        self.assertIn("@EnableScheduling", app)

    def test_audit_export_and_operation_schedules_have_bounded_contracts(self) -> None:
        audit = text("backend/services/audit-service/src/main/kotlin/com/lmspilot/audit/api/AuditApi.kt")
        operations = text("backend/services/operations-service/src/main/kotlin/com/lmspilot/operations/api/OperationsApi.kt")
        migration = text("backend/services/operations-service/src/main/resources/db/migration/V3__operation_schedules.sql")
        self.assertIn("export.csv", audit)
        self.assertIn("AUDIT_EXPORT", audit)
        self.assertIn("csvCell", audit)
        self.assertIn("OperationType.BACKUP", operations)
        self.assertIn("OperationType.MAINTENANCE", operations)
        self.assertIn("operation_schedules", migration)
