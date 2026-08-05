from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def all_java(service: str | None = None) -> str:
    root = ROOT / "backend/services"
    if service:
        root = root / service / "src/main/java"
    return "\n".join(p.read_text(encoding="utf-8") for p in sorted(root.rglob("*.java")))


class RepositoryContractTests(unittest.TestCase):
    def test_repository_validator_passes_current_layout(self) -> None:
        validator = read("scripts/validate-repository.py")
        self.assertIn('src/main/java', validator)
        self.assertIn('*Application.java', validator)
        self.assertNotIn('src/main/kotlin', validator)

    def test_demo_identifiers_are_consistent(self) -> None:
        seeds = "\n".join(p.read_text(encoding="utf-8") for p in ROOT.glob("backend/services/*/src/main/java/**/DevelopmentSeed.java"))
        env = read(".env.example")
        ids = re.findall(r"00000000-0000-0000-0000-[0-9]{12}", seeds)
        self.assertTrue(ids)
        self.assertIn("LMSPILOT_SEED_DEMO", env)

    def test_no_application_kotlin_remains(self) -> None:
        self.assertFalse(list((ROOT / "backend").rglob("*.kt")))
        self.assertTrue(list((ROOT / "backend").rglob("*.java")))

    def test_no_plaintext_runtime_secret_fallbacks(self) -> None:
        configs = "\n".join(p.read_text(encoding="utf-8") for p in ROOT.glob("backend/services/*/src/main/resources/application.yml"))
        self.assertNotIn("change-me", configs.lower())
        self.assertNotIn("secret: dev", configs.lower())

    def test_compose_lists_every_service_and_port(self) -> None:
        compose = read("docker-compose.yml")
        catalog = read("backend/services/SERVICE_CATALOG.md")
        for port in range(8080, 8099):
            self.assertIn(str(port), compose)
            self.assertIn(str(port), catalog)

    def test_contract_json_files_are_valid(self) -> None:
        json_files = list((ROOT / "contracts").rglob("*.json"))
        self.assertTrue(json_files)
        for path in json_files:
            json.loads(path.read_text(encoding="utf-8"))

    def test_gateway_has_no_obsolete_class_routes(self) -> None:
        gateway = read("backend/services/api-gateway/src/main/resources/application.yml")
        self.assertNotIn("/api/v1/classes", gateway)
        self.assertIn("/api/v1/courses", gateway)

    def test_storage_validates_owner_purpose_mime_and_hash(self) -> None:
        source = all_java("file-storage-service")
        for token in ("ownerId", "purpose", "contentType", "sha256", "FILE_TYPE_BLOCKED", "FILE_READ_FORBIDDEN"):
            self.assertIn(token, source)

    def test_course_and_assessment_use_course_quiz_context(self) -> None:
        course = all_java("course-service")
        assessment = all_java("assessment-service")
        self.assertIn("LessonType", course)
        self.assertIn("COURSE_QUIZ", assessment)
        self.assertIn("STANDALONE_EXAM", assessment)

    def test_internal_api_paths_are_service_token_protected(self) -> None:
        source = all_java()
        self.assertIn("InternalTokenAuthorizer", source)
        self.assertIn("X-Service-Token", source)

    def test_all_java_packages_use_lmspilot_namespace(self) -> None:
        for path in (ROOT / "backend").rglob("*.java"):
            first = path.read_text(encoding="utf-8").splitlines()[0]
            self.assertTrue(first.startswith("package com.lmspilot"), path)

    def test_current_product_name_is_lmspilot(self) -> None:
        active_docs = [ROOT / "README.md", ROOT / "BAT_DAU_TAI_DAY.md", ROOT / "DELIVERY_STATUS.md"]
        for path in active_docs:
            content = path.read_text(encoding="utf-8")
            self.assertIn("LMSPilot", content)
            self.assertNotIn("LMS-CLS", content)


if __name__ == "__main__":
    unittest.main()
