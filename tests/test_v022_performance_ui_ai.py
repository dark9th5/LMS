from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Release022PerformanceUiAiTests(unittest.TestCase):
    def test_version_is_0220(self):
        self.assertEqual("0.24.0", text("VERSION").strip())
        self.assertIn('"version": "0.24.0"', text("apps/web/package.json"))
        self.assertIn('version = "0.24.0"', text("backend/build.gradle.kts"))

    def test_client_api_has_cache_dedupe_timeout_and_retry(self):
        source = text("apps/web/lib/api.ts")
        for marker in [
            "responseCache",
            "requestsInFlight",
            "DEFAULT_GET_TTL_MS",
            "requestTimeout",
            "RETRYABLE_STATUS",
            "invalidateApiCache",
            "CLIENT_UPSTREAM_TIMEOUT",
        ]:
            self.assertIn(marker, source)
        self.assertIn('/api/v1/ai/local-runtime/pull/', source)

    def test_server_upstream_fetch_is_deterministic_and_timeout_bound(self):
        source = text("apps/web/lib/upstream-fetch.ts")
        self.assertIn("LMSPILOT_GATEWAY_URL", source)
        self.assertIn("new AbortController()", source)
        self.assertIn('controller.abort("SERVER_UPSTREAM_TIMEOUT")', source)
        self.assertIn("LMSPILOT_AI_UPSTREAM_TIMEOUT_MS", source)
        self.assertNotIn("keepalive: true", source)

    def test_exam_ui_and_ai_admin_center_are_integrated(self):
        exams = text("apps/web/components/ExamsPage.tsx")
        settings = text("apps/web/components/WorkspaceControlCenter.tsx")
        ai = text("apps/web/components/AiConnectionCenter.tsx")
        self.assertIn("exam-overview-layout", exams)
        self.assertNotIn("BÀI THI XX", exams)
        self.assertIn('"ai"', settings)
        self.assertIn("<AiConnectionCenter", settings)
        for marker in [
            "Tải và tự thiết lập",
            "LOCAL_OPENAI_COMPATIBLE",
            "REMOTE_OPENAI_COMPATIBLE",
            "/api/v1/ai/local-runtime/pull",
            "API key",
        ]:
            self.assertIn(marker, ai)

    def test_async_ollama_pull_and_provider_test_endpoints_exist(self):
        source = text("backend/services/ai-service/src/main/java/com/lmspilot/ai/api/LocalAiRuntimeApi.java")
        for marker in [
            '@PostMapping("/local-runtime/pull")',
            '@GetMapping("/local-runtime/pull/{jobId}")',
            'Thread.ofVirtual()',
            'ResponseEntity.accepted()',
            '@PostMapping("/providers/{id}/test")',
        ]:
            self.assertIn(marker, source)

    def test_login_failure_and_refresh_revocation_are_committed(self):
        source = text("backend/services/identity-service/src/main/java/com/lmspilot/identity/service/AuthService.java")
        self.assertGreaterEqual(source.count("@Transactional(noRollbackFor=ApiException.class)"), 2)
        request_source = text("backend/services/identity-service/src/main/java/com/lmspilot/identity/api/IdentityModels.java")
        self.assertIn("@Size(max=128)", request_source)
        self.assertIn("@Size(max=1024)", request_source)

    def test_backend_bundle_and_ollama_compose_are_present(self):
        dockerfile = text("backend/Dockerfile.bundle")
        compose = text("docker-compose.yml")
        self.assertIn("--parallel", dockerfile)
        self.assertIn("/app/services/${service}.jar", dockerfile)
        self.assertIn("lmspilot/backend-bundle:0.24.0", compose)
        self.assertIn("ollama/ollama", compose)
        self.assertIn("OLLAMA_MANAGEMENT_URL", compose)
        self.assertIn("SPRING_JPA_OPEN_IN_VIEW", compose)

    def test_gateway_pool_and_database_indexes_are_configured(self):
        gateway = text("backend/services/api-gateway/src/main/resources/application.yml")
        for marker in ["connect-timeout", "response-timeout", "pool:", "max-connections"]:
            self.assertIn(marker, gateway)
        migrations = list((ROOT / "backend/services").glob("*/src/main/resources/db/migration/*performance*.sql"))
        self.assertGreaterEqual(len(migrations), 9)
        for migration in migrations:
            sql = migration.read_text(encoding="utf-8")
            self.assertRegex(sql, re.compile(r"CREATE\s+INDEX\s+IF\s+NOT\s+EXISTS", re.I))

    def test_dark_mode_avoids_white_card_borders(self):
        css = text("apps/web/app/unified.css")
        self.assertIn("LMSPilot 0.22", css)
        self.assertIn('[data-theme="unified-dark"]', css)
        self.assertNotIn('[data-theme="unified-dark"]{--ui-border:#fff', css.replace(" ", ""))


if __name__ == "__main__":
    unittest.main()
