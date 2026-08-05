import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class LmsPilotDocumentationTest(unittest.TestCase):
    def test_current_version_is_consistent(self):
        self.assertEqual("0.20.4", text("VERSION").strip())
        self.assertIn('"version": "0.20.4"', text("apps/web/package.json"))
        self.assertIn('version = "0.20.4"', text("backend/build.gradle.kts"))

    def test_readme_contains_roles_services_api_and_database(self):
        readme = text("README.md")
        for role in ("ADMIN", "INSTRUCTOR", "STUDENT"):
            self.assertIn(role, readme)
        for service in ("api-gateway", "identity-service", "course-service", "assessment-service", "ai-service", "competency-service"):
            self.assertIn(service, readme)
        for port in ("8080", "8081", "8083", "8086", "8094", "8098"):
            self.assertIn(port, readme)
        self.assertIn("PostgreSQL", readme)
        self.assertIn("API_DATABASE_MAP.md", readme)

    def test_every_backend_service_has_assignment_ready_readme(self):
        service_root = ROOT / "backend/services"
        services = sorted(path for path in service_root.iterdir() if path.is_dir())
        self.assertEqual(19, len(services))
        for service in services:
            content = (service / "README.md").read_text(encoding="utf-8")
            self.assertIn("Owner", content, service.name)
            self.assertIn("Port mặc định", content, service.name)
            self.assertIn("API chính", content, service.name)
            self.assertIn("PostgreSQL schema", content, service.name)
            self.assertIn("Checklist owner", content, service.name)

    def test_old_product_acronym_is_not_used(self):
        excluded_parts = {".git", "build", ".next", "node_modules"}
        old_token = re.compile(r"\b" + "C" + "LS" + r"\b|LMS-" + "C" + "LS|_" + "C" + "LS_")
        offenders = []
        for path in ROOT.rglob("*"):
            if any(part in excluded_parts for part in path.parts):
                continue
            if "cls" in path.name.lower():
                offenders.append(str(path.relative_to(ROOT)))
                continue
            if not path.is_file() or path.suffix.lower() not in {".md", ".json", ".yml", ".yaml", ".kt", ".py", ".ts", ".tsx", ".css", ".txt"}:
                continue
            try:
                content = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            if old_token.search(content):
                offenders.append(str(path.relative_to(ROOT)))
        self.assertEqual([], offenders)

    def test_new_documentation_paths_exist(self):
        for path in (
            "docs/ARCHITECTURE.md",
            "docs/SERVICE_CATALOG.md",
            "docs/API_DATABASE_MAP.md",
            "docs/TEAM_SERVICE_ASSIGNMENT.md",
            "contracts/lmspilot/question-set.schema.json",
            "MANIFEST_LMSPILOT_0.20.4.json",
            "RELEASE_LMSPILOT_0.20.4.md",
            "TEST_RESULTS_LMSPILOT_0.20.4.md",
        ):
            self.assertTrue((ROOT / path).exists(), path)


if __name__ == "__main__":
    unittest.main()
