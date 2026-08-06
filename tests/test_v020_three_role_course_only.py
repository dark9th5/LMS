from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def source(service: str) -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / f"backend/services/{service}/src/main/java").rglob("*.java"))


class ThreeRoleCourseOnlyTests(unittest.TestCase):
    def test_release_version(self) -> None:
        self.assertEqual("0.24.0", read("VERSION").strip())

    def test_exact_three_roles(self) -> None:
        contracts = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "backend/platform-contracts/src/main/java").rglob("*.java"))
        for role in ("ADMIN", "INSTRUCTOR", "STUDENT"):
            self.assertIn(role, contracts)
        self.assertNotIn("BASIC_USER", contracts)
        self.assertNotIn("COURSE_AUTHOR", contracts)

    def test_identity_enforces_single_role(self) -> None:
        identity = source("identity-service")
        self.assertIn("EXCLUSIVE_ROLE_MODEL", identity)
        self.assertIn("codes.size()!=1", identity.replace(" ", ""))

    def test_no_class_route_in_frontend_or_gateway(self) -> None:
        frontend = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "apps/web").rglob("*.tsx"))
        gateway = read("backend/services/api-gateway/src/main/resources/application.yml")
        self.assertNotIn('href="/classes"', frontend)
        self.assertNotIn('/api/v1/classes', gateway)

    def test_course_service_owns_lessons_and_materials(self) -> None:
        course = source("course-service")
        self.assertIn("LessonEntity", course)
        self.assertIn("LessonType", course)
        self.assertIn('/api/v1/courses', course)

    def test_learning_supports_video_documents_and_assignments(self) -> None:
        course = source("course-service")
        learning = source("learning-service")
        storage = source("file-storage-service")
        for token in ("VIDEO", "PDF", "DOCX", "ASSIGNMENT"):
            self.assertIn(token, course + storage)
        self.assertIn("AssignmentSubmissionEntity", learning)

    def test_course_quiz_and_standalone_exam_are_separate(self) -> None:
        assessment = source("assessment-service")
        self.assertIn("COURSE_QUIZ", assessment)
        self.assertIn("STANDALONE_EXAM", assessment)

    def test_ai_only_uses_document_versions(self) -> None:
        ai = source("ai-service")
        self.assertIn("documentVersionIds", ai)
        self.assertIn("citations", ai)
        self.assertIn("courseId", ai)

    def test_branding_supports_login_background(self) -> None:
        config = source("configuration-service")
        self.assertIn("backgroundFileId", config)
        self.assertIn("logoFileId", config)

    def test_backend_services_are_independent_java_apps(self) -> None:
        apps = list((ROOT / "backend/services").glob("*/src/main/java/**/*Application.java"))
        self.assertEqual(19, len(apps))

    def test_each_service_has_own_port(self) -> None:
        ports = []
        for config in (ROOT / "backend/services").glob("*/src/main/resources/application.yml"):
            for line in config.read_text(encoding="utf-8").splitlines():
                if line.strip().startswith("port:"):
                    import re
                    match = re.search(r"(\d{4})", line)
                    self.assertIsNotNone(match)
                    ports.append(int(match.group(1)))
                    break
        self.assertEqual(list(range(8080, 8099)), sorted(ports))

    def test_dark_mode_has_no_white_border_override(self) -> None:
        css = read("apps/web/app/unified.css")
        self.assertIn('[data-theme="unified-dark"]', css)
        self.assertNotRegex(css.lower(), r"\[data-theme=.?dark.?\][^{]*\{[^}]*border-color:\s*#fff")


if __name__ == "__main__":
    unittest.main()
