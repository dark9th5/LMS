from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / "backend"
SERVICES = BACKEND / "services"


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def java_sources(path: Path) -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in sorted(path.rglob("*.java")))


class JavaSpringBackendMigrationTests(unittest.TestCase):
    def test_release_version_is_0210(self) -> None:
        self.assertEqual("0.23.0", read("VERSION").strip())
        self.assertIn('"version": "0.23.0"', read("apps/web/package.json"))
        self.assertIn('version = "0.23.0"', read("backend/build.gradle.kts"))

    def test_application_source_is_java_only(self) -> None:
        self.assertEqual([], list(BACKEND.rglob("*.kt")))
        java_files = list(BACKEND.rglob("*.java"))
        self.assertGreaterEqual(len(java_files), 200)

    def test_gradle_uses_java_spring_boot(self) -> None:
        root_build = read("backend/build.gradle.kts")
        self.assertIn('id("org.springframework.boot") version "3.5.16"', root_build)
        self.assertIn("options.release.set(21)", root_build)
        self.assertNotIn("org.jetbrains.kotlin", root_build)
        for build in sorted(SERVICES.glob("*/build.gradle.kts")):
            content = build.read_text(encoding="utf-8")
            self.assertRegex(content, r'plugins\s*\{[\s\S]*\bjava\b')
            self.assertNotIn("kotlin(", content, build)
            self.assertNotIn("org.jetbrains.kotlin", content, build)

    def test_nineteen_independent_spring_boot_services(self) -> None:
        service_dirs = sorted(p for p in SERVICES.iterdir() if p.is_dir())
        self.assertEqual(19, len(service_dirs))
        for service in service_dirs:
            applications = list((service / "src/main/java").rglob("*Application.java"))
            self.assertEqual(1, len(applications), service.name)
            source = applications[0].read_text(encoding="utf-8")
            self.assertIn("@SpringBootApplication", source)
            self.assertRegex(source, r"SpringApplication\.run\([^,]+\.class, args\)")

    def test_ports_are_unique_from_8080_to_8098(self) -> None:
        ports: dict[str, int] = {}
        for service in sorted(p for p in SERVICES.iterdir() if p.is_dir()):
            app = (service / "src/main/resources/application.yml").read_text(encoding="utf-8")
            match = re.search(r"(?m)^\s*port:\s*(?:\$\{SERVER_PORT:)?(\d+)", app)
            self.assertIsNotNone(match, service.name)
            ports[service.name] = int(match.group(1))
        self.assertEqual(list(range(8080, 8099)), sorted(ports.values()))

    def test_database_services_own_flyway_migrations(self) -> None:
        migrations = 0
        for service in sorted(p for p in SERVICES.iterdir() if p.is_dir()):
            migration_dir = service / "src/main/resources/db/migration"
            if migration_dir.exists():
                migrations += 1
                scripts = sorted(migration_dir.glob("V*__*.sql"))
                self.assertTrue(scripts, service.name)
                self.assertTrue(all(p.read_text(encoding="utf-8").strip() for p in scripts))
        self.assertEqual(18, migrations)

    def test_java_junit_tests_exist(self) -> None:
        tests = list(BACKEND.rglob("src/test/java/**/*.java"))
        self.assertGreaterEqual(len(tests), 2)
        combined = "\n".join(p.read_text(encoding="utf-8") for p in tests)
        self.assertIn("org.junit.jupiter.api.Test", combined)
        self.assertNotIn("kotlin.test", combined)

    def test_three_exclusive_roles_are_preserved(self) -> None:
        contracts = java_sources(BACKEND / "platform-contracts/src/main/java")
        identity = java_sources(SERVICES / "identity-service/src/main/java")
        for role in ("ADMIN", "INSTRUCTOR", "STUDENT"):
            self.assertIn(role, contracts)
            self.assertIn(role, identity)
        self.assertIn("EXCLUSIVE_ROLE_MODEL", identity)
        self.assertIn("codes.size()!=1", identity.replace(" ", ""))

    def test_api_gateway_routes_all_backend_services(self) -> None:
        gateway = read("backend/services/api-gateway/src/main/resources/application.yml")
        for route in (
            "identity", "organization", "course", "enrollment", "learning", "assessment",
            "grading", "reporting", "files", "license", "audit", "notification", "certificate",
            "ai", "configuration", "integration", "operations", "competency",
        ):
            self.assertIn(f"id: {route}", gateway)

    def test_internal_endpoints_require_service_token(self) -> None:
        source = java_sources(SERVICES)
        self.assertIn("InternalTokenAuthorizer", source)
        self.assertIn('X-Service-Token', source)
        internal_controllers = [p for p in SERVICES.rglob("*.java") if 'RequestMapping("/internal/' in p.read_text(encoding="utf-8")]
        self.assertGreaterEqual(len(internal_controllers), 10)

    def test_readme_describes_java_backend(self) -> None:
        readme = read("README.md")
        self.assertIn("Java 21", readme)
        self.assertIn("Spring Boot 3.5.16", readme)
        self.assertNotIn("Kotlin + Spring Boot", readme)


if __name__ == "__main__":
    unittest.main()
