from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class RepositoryContractTests(unittest.TestCase):
    def test_demo_identifiers_are_consistent(self) -> None:
        seeds = "\n".join(
            path.read_text(encoding="utf-8")
            for path in ROOT.glob("backend/services/*/src/main/kotlin/**/DevelopmentSeed.kt")
        )
        expected = {
            "admin": "00000000-0000-0000-0000-000000000001",
            "instructor": "00000000-0000-0000-0000-000000000002",
            "student": "00000000-0000-0000-0000-000000000003",
            "course": "00000000-0000-0000-0000-000000000101",
            "class": "00000000-0000-0000-0000-000000000201",
            "enrollment": "00000000-0000-0000-0000-000000000202",
        }
        for label, value in expected.items():
            self.assertIn(value, seeds, f"Missing shared demo {label} identifier")
        self.assertGreaterEqual(seeds.count(expected["course"]), 4)
        self.assertGreaterEqual(seeds.count(expected["student"]), 4)

    def test_frontend_dependencies_are_exact_and_patched(self) -> None:
        package = json.loads((ROOT / "apps/web/package.json").read_text(encoding="utf-8"))
        for group in ("dependencies", "devDependencies"):
            for name, version in package[group].items():
                self.assertRegex(version, r"^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$", f"{name} is not pinned")
        react = tuple(map(int, package["dependencies"]["react"].split(".")[:3]))
        react_dom = tuple(map(int, package["dependencies"]["react-dom"].split(".")[:3]))
        self.assertGreaterEqual(react, (19, 2, 6))
        self.assertEqual(react, react_dom)

    def test_runtime_secrets_have_no_backend_fallback(self) -> None:
        for path in ROOT.glob("backend/services/*/src/main/resources/application.yml"):
            text = path.read_text(encoding="utf-8")
            self.assertNotIn("LMSPILOT_JWT_SECRET:", text)
            self.assertNotIn("LMSPILOT_INTERNAL_TOKEN:development", text)
            if "lmspilot:\n" in text and "jwt:" in text:
                self.assertIn("${LMSPILOT_JWT_SECRET}", text)

    def test_all_internal_controllers_require_service_token(self) -> None:
        for path in ROOT.glob("backend/services/*/src/main/kotlin/**/*.kt"):
            text = path.read_text(encoding="utf-8")
            if '@RequestMapping("/internal/v1' in text:
                self.assertIn("InternalTokenAuthorizer", text, str(path))
                self.assertIn("internal.require(", text, str(path))

    def test_migrations_do_not_use_destructive_drop(self) -> None:
        for path in ROOT.glob("backend/services/*/src/main/resources/db/migration/*.sql"):
            text = path.read_text(encoding="utf-8").upper()
            self.assertNotRegex(text, r"\bDROP\s+(TABLE|SCHEMA|DATABASE)\b", str(path))

    def test_gateway_routes_are_unique(self) -> None:
        text = (ROOT / "backend/services/api-gateway/src/main/resources/application.yml").read_text(encoding="utf-8")
        ids = re.findall(r"^\s+- id:\s+([A-Za-z0-9_-]+)\s*$", text, flags=re.MULTILINE)
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(17, len(ids))

    def test_operational_scripts_are_not_exposed_to_web_container(self) -> None:
        compose = (ROOT / "docker-compose.yml").read_text(encoding="utf-8")
        web_block = compose.split("\n  web:\n", 1)[1].split("\n  prometheus:\n", 1)[0]
        self.assertNotIn("docker.sock", web_block)
        self.assertNotIn("./scripts", web_block)


    def test_role_aware_login_and_portal_routes(self) -> None:
        login_api = (ROOT / "apps/web/app/api/auth/login/route.ts").read_text(encoding="utf-8")
        login_form = (ROOT / "apps/web/app/login/LoginForm.tsx").read_text(encoding="utf-8")
        dashboard = (ROOT / "apps/web/app/(portal)/dashboard/page.tsx").read_text(encoding="utf-8")
        self.assertIn("user: payload.user", login_api)
        self.assertIn('path: "/"', login_api)
        self.assertIn("landingForRoles(data.user.roles)", login_form)
        self.assertIn("window.location.replace", login_form)
        self.assertIn('redirect("/learning")', dashboard)

    def test_real_lms_pages_are_wired_to_backend(self) -> None:
        expected = {
            "apps/web/components/CoursesPage.tsx": ["/api/v1/courses", 'method: \"POST\"'],
            "apps/web/components/ClassDetail.tsx": ["/enrollments", 'method: \"POST\"'],
            "apps/web/components/LearningPlayer.tsx": ["/api/v1/learning/progress", "Idempotency-Key"],
            "apps/web/components/ExamDetail.tsx": ["/api/v1/exams/start", "/answers", "/submit"],
            "apps/web/components/GradingPage.tsx": ["/api/v1/grades/queue", 'method: \"PUT\"'],
        }
        for relative, markers in expected.items():
            path = ROOT / relative
            self.assertTrue(path.exists(), relative)
            text = path.read_text(encoding="utf-8")
            for marker in markers:
                self.assertIn(marker, text, f"{marker} missing from {relative}")

    def test_sidebar_and_course_outline_hide_scrollbars(self) -> None:
        css = (ROOT / "apps/web/app/globals.css").read_text(encoding="utf-8")
        self.assertIn(".sidebar-nav::-webkit-scrollbar", css)
        self.assertIn(".player-lesson-list::-webkit-scrollbar", css)
        self.assertGreaterEqual(css.count("scrollbar-width:none"), 2)


    def test_course_lessons_support_real_editing_and_inline_viewing(self) -> None:
        detail = (ROOT / "apps/web/components/CourseDetail.tsx").read_text(encoding="utf-8")
        resource = (ROOT / "apps/web/components/LessonResource.tsx").read_text(encoding="utf-8")
        storage = (ROOT / "backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileStorageApi.kt").read_text(encoding="utf-8")
        self.assertIn('method: "PUT"', detail)
        self.assertIn('/lessons/${editingLesson.id}', detail)
        self.assertNotIn("vòng tiếp theo", detail.lower())
        self.assertIn("?inline=true", resource)
        self.assertIn("ContentDisposition.inline()", storage)

    def test_assigned_instructors_can_read_but_not_edit_foreign_courses(self) -> None:
        api = (ROOT / "backend/services/course-service/src/main/kotlin/com/lmspilot/course/api/CourseApi.kt").read_text(encoding="utf-8")
        domain = (ROOT / "backend/services/course-service/src/main/kotlin/com/lmspilot/course/domain/CourseDomain.kt").read_text(encoding="utf-8")
        detail = (ROOT / "apps/web/components/CourseDetail.tsx").read_text(encoding="utf-8")
        self.assertIn("assignedCourseIds", api)
        self.assertIn("searchVisible", api)
        self.assertIn("c.id in :assignedCourseIds", domain)
        self.assertIn('course.ownerId === user.id', detail)
        self.assertIn("disabled={!canEdit}", detail)

    def test_idempotency_helpers_are_imported_where_used(self) -> None:
        for path in (ROOT / "apps/web").rglob("*.tsx"):
            text = path.read_text(encoding="utf-8")
            if "createIdempotencyKey(" in text:
                self.assertRegex(text, r'import\s+\{[^}]*createIdempotencyKey[^}]*\}\s+from\s+"@/lib/api"', str(path))

    def test_static_icon_names_exist_in_icon_contract(self) -> None:
        types = (ROOT / "apps/web/lib/types.ts").read_text(encoding="utf-8")
        allowed = set(re.findall(r'"([a-z-]+)"', types.split("export type IconName", 1)[1]))
        for path in (ROOT / "apps/web").rglob("*.tsx"):
            text = path.read_text(encoding="utf-8")
            for name in re.findall(r'<Icon\s+name="([a-z-]+)"', text):
                self.assertIn(name, allowed, f"Unknown icon {name} in {path}")


if __name__ == "__main__":
    unittest.main()
