from __future__ import annotations

import re
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


class Release020ThreeRoleCourseOnlyTests(unittest.TestCase):
    def test_release_version_is_020(self) -> None:
        self.assertEqual("0.20.4", text("VERSION").strip())
        self.assertIn('"version": "0.20.4"', text("apps/web/package.json"))

    def test_exactly_three_canonical_access_profiles(self) -> None:
        profiles = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/AccessProfiles.kt")
        codes = re.findall(r'AccessProfileDefinition\(\s*code = "([A-Z_]+)"', profiles)
        self.assertEqual(["ADMIN", "INSTRUCTOR", "STUDENT"], codes)
        self.assertNotIn('code = "BASIC_USER"', profiles)
        self.assertNotIn('code = "COURSE_AUTHOR"', profiles)

    def test_role_permissions_do_not_cross_portal_boundaries(self) -> None:
        permissions = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/Permissions.kt")
        admin = permissions[permissions.index("val ADMIN = setOf("):permissions.index("/** Course authoring")]
        instructor = permissions[permissions.index("val INSTRUCTOR = setOf("):permissions.index("/** Learning, course quizzes")]
        student = permissions[permissions.index("val STUDENT = setOf("):permissions.index("val LEARNER = STUDENT")]
        self.assertIn("Permissions.USERS_CREATE", admin)
        self.assertNotIn("Permissions.COURSES_CREATE", admin)
        self.assertNotIn("Permissions.ASSESSMENTS_TAKE", admin)
        self.assertIn("Permissions.COURSES_CREATE", instructor)
        self.assertIn("Permissions.ASSESSMENTS_GRADE", instructor)
        self.assertNotIn("Permissions.USERS_CREATE", instructor)
        self.assertNotIn("Permissions.ASSESSMENTS_TAKE", instructor)
        self.assertIn("Permissions.COURSES_LEARN", student)
        self.assertIn("Permissions.ASSESSMENTS_TAKE", student)
        self.assertNotIn("Permissions.COURSES_CREATE", student)
        self.assertNotIn("Permissions.ASSESSMENTS_GRADE", student)

    def test_database_and_user_service_enforce_one_role(self) -> None:
        migration = text("backend/services/identity-service/src/main/resources/db/migration/V6__exclusive_three_role_model.sql")
        users = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/UserManagementService.kt")
        self.assertIn("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_single_product_role ON user_roles(user_id)", migration)
        self.assertIn("DELETE FROM authorization_grants", migration)
        self.assertIn("DELETE FROM scoped_role_assignments", migration)
        self.assertIn("codes.size != 1", users)
        self.assertIn("SINGLE_ROLE_REQUIRED", users)
        self.assertIn('requestedRole != "ADMIN"', users)
        self.assertIn("user.accountType = if (user.protectedAccount)", users)

    def test_login_cookie_accepts_only_one_canonical_role(self) -> None:
        for relative in ("apps/web/app/api/auth/login/route.ts", "apps/web/lib/session-cookie.ts"):
            source = text(relative)
            self.assertIn("user.roles.length === 1", source)
            self.assertIn('["ADMIN", "INSTRUCTOR", "STUDENT"]', source)
            self.assertIn("user.primaryRole === user.roles[0]", source)

    def test_portal_role_resolution_rejects_malformed_or_multi_role_sessions(self) -> None:
        role = text("apps/web/lib/role.ts")
        self.assertIn("normalized.length !== 1", role)
        self.assertIn("user.primaryRole.toUpperCase() !== normalized[0]", role)
        self.assertNotIn("if (isPortalRole(user.primaryRole)) return user.primaryRole", role)

    def test_each_role_has_a_separate_portal_and_navigation(self) -> None:
        shell = text("apps/web/components/AppShell.tsx")
        paths = text("apps/web/lib/portal-paths.ts")
        for role, route in (("ADMIN", "/admin"), ("INSTRUCTOR", "/instructor"), ("STUDENT", "/student")):
            self.assertIn(f"{role}: {{", shell)
            self.assertIn(f'home: "{route}"', paths)
            self.assertTrue((ROOT / f"apps/web/app/(portal)/{route[1:]}/page.tsx").is_file())
        self.assertIn('requireRole(user, "ADMIN")', text("apps/web/app/(portal)/admin/page.tsx"))
        self.assertIn('requireRole(user, "INSTRUCTOR")', text("apps/web/app/(portal)/instructor/page.tsx"))
        self.assertIn('requireRole(user, "STUDENT")', text("apps/web/app/(portal)/student/page.tsx"))

    def test_admin_account_ui_uses_one_role_instead_of_cross_role_grants(self) -> None:
        console = text("apps/web/components/WorkspaceControlCenter.tsx")
        tabs = console[console.index("const tabs:"):console.index("useEffect(() =>", console.index("const tabs:"))]
        accounts = console[console.index('title="Danh sách tài khoản"'):console.index('{tab === "grant"')]
        self.assertIn('label: "Tài khoản"', tabs)
        self.assertIn('label: "Nhập từ tệp"', tabs)
        self.assertNotIn('label: "Cấp gói quyền"', tabs)
        self.assertNotIn('label: "Thu hồi quyền"', tabs)
        self.assertIn('title="Tài khoản và vai trò"', console)
        self.assertIn("Mỗi tài khoản có đúng một vai trò", console)
        self.assertIn("<th>Vai trò</th>", accounts)
        self.assertNotIn("SYSTEM ADMIN", accounts)
        self.assertNotIn("<th>Gói quyền cơ sở</th>", accounts)

    def test_public_product_is_course_only(self) -> None:
        workspace = text("apps/web/components/CourseWorkspace.tsx")
        shell = text("apps/web/components/AppShell.tsx")
        self.assertNotIn("ClassesPage", workspace)
        self.assertNotIn('label: "Lớp học"', shell)
        self.assertFalse((ROOT / "apps/web/app/(portal)/classes/page.tsx").exists())
        self.assertFalse((ROOT / "apps/web/app/(portal)/classes/[id]/page.tsx").exists())
        active_ui = "\n".join(
            text(path) for path in (
                "apps/web/components/AppShell.tsx",
                "apps/web/components/CourseWorkspace.tsx",
                "apps/web/components/CourseLearnersPanel.tsx",
            )
        )
        self.assertNotIn("Lớp học", active_ui)
        self.assertNotIn("lớp trung gian", active_ui.lower())
        assignment_api = text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/AssignmentSubmissionApi.kt")
        self.assertNotIn('@GetMapping("/queue/{classId}")', assignment_api)
        self.assertNotIn('@RequestParam classId', assignment_api)
        response_block = assignment_api[assignment_api.index("data class AssignmentSubmissionResponse("):assignment_api.index("data class AssignmentFileMetadata(")]
        self.assertNotIn("classId", response_block)
        learning_api = text("backend/services/learning-service/src/main/kotlin/com/lmspilot/learning/api/LearningApi.kt")
        self.assertNotIn("assignedDeliveryIds", learning_api)
        self.assertIn("metadata.ownerId != currentUserId", learning_api)

    def test_course_quizzes_live_inside_course_and_use_course_documents(self) -> None:
        detail = text("apps/web/components/CourseDetail.tsx")
        panel = text("apps/web/components/CourseAssessmentsPanel.tsx")
        self.assertIn("CourseAssessmentsPanel", detail)
        self.assertIn('contextType: "COURSE_QUIZ"', panel)
        self.assertIn("courseId: course.id", panel)
        self.assertIn("documentFileIds: selectedDocuments", panel)
        self.assertIn('["PDF", "DOCX"]', panel)
        self.assertIn("Tạo bài kiểm tra từ tài liệu", panel)
        self.assertIn("imported.importedQuestionIds.map", panel)
        self.assertIn("const createdExam = await apiRequest<Exam>", panel)

    def test_standalone_exams_are_separate_and_can_be_built_from_documents(self) -> None:
        exams = text("apps/web/components/ExamsPage.tsx")
        ai = text("backend/services/ai-service/src/main/kotlin/com/lmspilot/ai/api/QuestionGenerationApi.kt")
        self.assertIn('contextType: "STANDALONE_EXAM"', exams)
        self.assertIn("STANDALONE_QUESTION_WORKSPACE_ID", exams)
        self.assertIn("Tạo từ PDF/DOCX", exams)
        self.assertIn("QUESTION_SOURCE", exams)
        self.assertIn("STANDALONE_QUESTION_WORKSPACE_ID", ai)
        self.assertIn("documentFileIds", ai)
        self.assertIn("metadata.ownerId != CurrentUser.id()", ai)

    def test_course_player_supports_video_pdf_docx_and_direct_assignments(self) -> None:
        resource = text("apps/web/components/LessonResource.tsx")
        docx = text("apps/web/components/DocxPreview.tsx")
        player = text("apps/web/components/LearningPlayer.tsx")
        storage = text("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileStorageApi.kt")
        self.assertIn("<video controls", resource)
        self.assertIn('if (type === "PDF")', resource)
        self.assertIn('if (type === "DOCX")', resource)
        self.assertIn("DocxPreview", resource)
        self.assertIn("docx-preview", docx)
        self.assertIn("AssignmentSubmissionPanel", player)
        self.assertIn("/api/v1/learning/assignments/", player)
        self.assertIn('GetMapping("/{id}/docx-preview")', storage)
        self.assertIn("DocumentBuilderFactory", storage)
        self.assertIn("disallow-doctype-decl", storage)

    def test_login_background_is_customer_configurable(self) -> None:
        settings = text("apps/web/components/WorkspaceControlCenter.tsx")
        login = text("apps/web/app/login/page.tsx")
        config = text("backend/services/configuration-service/src/main/kotlin/com/lmspilot/configuration/api/CustomizationApi.kt")
        self.assertIn("BRANDING_BACKGROUND", settings)
        self.assertIn("backgroundFileId", settings)
        self.assertIn("branding.backgroundUrl", login)
        self.assertIn("backgroundImage", login)
        self.assertIn("backgroundUrl", config)
        self.assertIn("BrandingFileClient", config)
        self.assertIn('"BRANDING_BACKGROUND"', config)
        self.assertIn("metadata.ownerId != CurrentUser.id()", config)
        self.assertIn('setOf("image/png", "image/jpeg", "image/webp")', config)

    def test_dark_theme_uses_muted_non_white_separators(self) -> None:
        css = text("apps/web/app/unified.css")
        dark = css[css.index('[data-theme="unified-dark"]{'):css.index("@media(max-width:1020px)")]
        self.assertIn("--ui-border:#263247", dark)
        self.assertIn("--ui-border-strong:#3a4963", dark)
        self.assertNotIn("--ui-border:#fff", dark)
        self.assertIn("never bright white outlines", css)
        self.assertIn("border-color:var(--ui-border)!important", css)
        self.assertIn(':root:not([data-theme]),[data-theme="unified-light"]', css)
        self.assertNotIn('\n:root,[data-theme="unified-light"]{\n  --sidebar-width', css)

    def test_services_are_split_and_use_unique_ports(self) -> None:
        service_dirs = sorted(path.name for path in (ROOT / "backend/services").iterdir() if path.is_dir())
        self.assertEqual(19, len(service_dirs))
        result = subprocess.run(
            ["python3", "scripts/validate-service-ports.py"],
            cwd=ROOT,
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("19 backend services", result.stdout)
        self.assertIn("api-gateway", text("docs/SERVICE_CATALOG.md"))

    def test_system_admin_account_type_is_not_a_frontend_permission_bypass(self) -> None:
        for relative in (
            "apps/web/components/CoursesPage.tsx",
            "apps/web/components/CourseDetail.tsx",
            "apps/web/components/WorkspaceControlCenter.tsx",
            "apps/web/components/SectionPage.tsx",
        ):
            self.assertNotIn('accountType === "SYSTEM_ADMIN"', text(relative), relative)

    def test_backend_role_boundaries_do_not_use_system_admin_bypass(self) -> None:
        protected_services = (
            "course-service",
            "assessment-service",
            "grading-service",
            "learning-service",
            "enrollment-service",
            "file-storage-service",
        )
        for service in protected_services:
            root = ROOT / "backend/services" / service / "src/main/kotlin"
            source = "\n".join(path.read_text(encoding="utf-8") for path in root.rglob("*.kt"))
            self.assertNotIn("CurrentUser.isSystemAdmin()", source, service)
            self.assertNotIn("accountType() == \"SYSTEM_ADMIN\"", source, service)
        jwt = text("backend/service-support/src/main/kotlin/com/lmspilot/support/security/JwtSupport.kt")
        self.assertIn("roles().size == 1", jwt)
        self.assertIn('hasRole("ADMIN")', jwt)
        storage = text("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileStorageApi.kt")
        editing = text("backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileEditingApi.kt")
        self.assertNotIn("OPERATIONS_MANAGE in CurrentUser.authorities()", storage)
        self.assertNotIn("OPERATIONS_MANAGE !in CurrentUser.authorities()", storage)
        self.assertNotIn("OPERATIONS_MANAGE !in CurrentUser.authorities()", editing)


if __name__ == "__main__":
    unittest.main()
