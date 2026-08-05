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
            "learner": "00000000-0000-0000-0000-000000000003",
            "course": "00000000-0000-0000-0000-000000000101",
            "class": "00000000-0000-0000-0000-000000000201",
            "enrollment": "00000000-0000-0000-0000-000000000202",
        }
        for label, value in expected.items():
            self.assertIn(value, seeds, f"Missing shared demo {label} identifier")
        self.assertGreaterEqual(seeds.count(expected["course"]), 4)
        self.assertGreaterEqual(seeds.count(expected["learner"]), 4)

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
            self.assertNotIn("change-this", text)
            self.assertNotRegex(text, r"\$\{(?:AI_SECRET_KEY|CONFIGURATION_SECRET_KEY):[^}]+\}")

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
        self.assertEqual(18, len(ids))

    def test_operational_scripts_are_not_exposed_to_web_container(self) -> None:
        compose = (ROOT / "docker-compose.yml").read_text(encoding="utf-8")
        web_block = compose.split("\n  web:\n", 1)[1].split("\n  prometheus:\n", 1)[0]
        self.assertNotIn("docker.sock", web_block)
        self.assertNotIn("./scripts", web_block)


    def test_role_aware_login_and_portal_routes(self) -> None:
        login_api = (ROOT / "apps/web/app/api/auth/login/route.ts").read_text(encoding="utf-8")
        login_form = (ROOT / "apps/web/app/login/LoginForm.tsx").read_text(encoding="utf-8")
        dashboard = (ROOT / "apps/web/app/(portal)/dashboard/page.tsx").read_text(encoding="utf-8")
        self.assertIn("user: data.user", login_api)
        self.assertIn("encodeUserCookie(data.user)", login_api)
        self.assertNotIn("MOCK_USERS", login_api)
        self.assertIn('path: "/"', login_api)
        self.assertIn("landingForUser(data.user)", login_form)
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

    def test_navigation_and_course_outline_remain_scrollable(self) -> None:
        css = (ROOT / "apps/web/app/unified.css").read_text(encoding="utf-8")
        self.assertIn(".app-nav", css)
        self.assertIn(".player-lesson-list", css)
        self.assertGreaterEqual(len(re.findall(r"overflow(?:-y)?:auto", css)), 2)
        self.assertIn("scrollbar-width:thin", css)


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

    def test_course_and_assessment_crud_contracts_are_complete(self) -> None:
        course_api = (ROOT / "backend/services/course-service/src/main/kotlin/com/lmspilot/course/api/CourseApi.kt").read_text(encoding="utf-8")
        assessment_api = (ROOT / "backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/api/AssessmentApi.kt").read_text(encoding="utf-8")
        course_ui = (ROOT / "apps/web/components/CourseDetail.tsx").read_text(encoding="utf-8")
        exams_ui = (ROOT / "apps/web/components/ExamsPage.tsx").read_text(encoding="utf-8")
        exam_detail = (ROOT / "apps/web/components/ExamDetail.tsx").read_text(encoding="utf-8")
        self.assertIn('@DeleteMapping("/{courseId}/lessons/{lessonId}")', course_api)
        self.assertIn('@DeleteMapping("/{id}")', course_api)
        self.assertIn('method: "DELETE"', course_ui)
        self.assertIn("fun updateExam", assessment_api)
        self.assertIn("fun archiveExam", assessment_api)
        self.assertIn("fun archiveQuestion", assessment_api)
        self.assertIn('/api/v1/questions/${question.id}', exams_ui)
        self.assertIn('/api/v1/exams/${exam.id}', exam_detail)

    def test_demo_course_has_real_multimedia_assets_and_exam(self) -> None:
        course_seed = (ROOT / "backend/services/course-service/src/main/kotlin/com/lmspilot/course/config/DevelopmentSeed.kt").read_text(encoding="utf-8")
        assessment_seed = (ROOT / "backend/services/assessment-service/src/main/kotlin/com/lmspilot/assessment/config/DevelopmentSeed.kt").read_text(encoding="utf-8")
        file_seed = (ROOT / "backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/config/DevelopmentSeed.kt").read_text(encoding="utf-8")
        models = (ROOT / "apps/web/lib/models.ts").read_text(encoding="utf-8")
        for marker in ("Bài 0 - Làm quen với LMSPilot", "LessonType.VIDEO", "LessonType.PDF", "LessonType.DOCX", "LessonType.EXAM"):
            self.assertIn(marker, course_seed)
        self.assertIn("Bài 0 - Kiểm tra làm quen LMSPilot", assessment_seed)
        self.assertGreaterEqual(assessment_seed.count("SampleQuestion("), 5)
        self.assertIn("ClassPathResource", file_seed)
        self.assertIn('"DOCX"', models)
        for name in (
            "LMSPilot_Gioi_thieu_Bai_0.mp4",
            "LMSPilot_Huong_dan_nhanh_hoc_vien.pdf",
            "LMSPilot_Checklist_giang_vien.docx",
        ):
            asset = ROOT / "backend/services/file-storage-service/src/main/resources/demo" / name
            self.assertTrue(asset.is_file(), name)
            self.assertGreater(asset.stat().st_size, 1000, name)

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
