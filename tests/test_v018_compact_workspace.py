from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


class Release018CompactWorkspaceTests(unittest.TestCase):
    def test_release_version_is_bumped(self) -> None:
        self.assertEqual("0.24.0", text("VERSION").strip())
        self.assertIn('"version": "0.24.0"', text("apps/web/package.json"))
        self.assertIn('version = "0.24.0"', text("backend/build.gradle.kts"))

    def test_sidebar_uses_single_line_labels_and_persists_collapse(self) -> None:
        shell = text("apps/web/components/AppShell.tsx")
        self.assertNotIn("<small>{item.hint}</small>", shell)
        self.assertIn('label: "Khóa học"', shell)
        self.assertNotRegex(shell, r'href:\s*"/classes"[\s\S]{0,120}label:')
        self.assertIn('localStorage.getItem("lms-sidebar-collapsed")', shell)
        self.assertIn('localStorage.setItem("lms-sidebar-collapsed"', shell)
        css = text("apps/web/app/unified.css")
        self.assertIn("white-space:nowrap", css)
        self.assertIn(".app-shell.sidebar-collapsed", css)
        self.assertNotIn('className="app-nav-arrow"', shell)
        self.assertIn("0.18 readability patch", css)
        self.assertIn(".app-nav-copy strong{font-size:15px", css)

    def test_course_and_class_are_one_navigation_area(self) -> None:
        workspace = text("apps/web/components/CourseWorkspace.tsx")
        shell = text("apps/web/components/AppShell.tsx")
        self.assertIn("CoursesPage", workspace)
        self.assertNotIn("ClassesPage", workspace)
        self.assertNotIn("Lớp triển khai", workspace)
        self.assertNotIn('label: "Lớp học"', shell)
        self.assertFalse((ROOT / "apps/web/app/(portal)/classes/page.tsx").exists())
        self.assertIn("CourseWorkspace", text("apps/web/app/(portal)/instructor/courses/page.tsx"))
        self.assertIn("PORTAL_PATHS.INSTRUCTOR.courses", text("apps/web/app/(portal)/courses/page.tsx"))

    def test_settings_are_brand_first_without_duplicate_light_dark_selector(self) -> None:
        settings = text("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn("Cấu hình thông tin", settings)
        self.assertIn("BRAND_COLORS", settings)
        self.assertIn("BRANDING_LOGO", settings)
        self.assertIn("logoFileId", settings)
        self.assertIn("Giới thiệu ngắn", settings)
        self.assertIn("lmspilot-settings-tab", settings)
        self.assertNotIn("AppearanceStudio", settings)
        self.assertNotIn('setTab("themes")', settings)
        self.assertNotIn("Chọn chế độ sáng hoặc tối", settings)

    def test_organization_selects_real_users_and_keeps_selected_unit(self) -> None:
        source = text("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn("lmspilot-organization-unit", source)
        self.assertIn("Tên, tài khoản hoặc email", source)
        self.assertIn("member-picker", source)
        organization_section = source[source.index("function OrganizationConsole"):source.index("// Branding and external services")]
        self.assertNotIn("RepeatableField", organization_section)
        self.assertNotIn("Mã người dùng", organization_section)

    def test_external_service_forms_and_backend_validation_are_structured(self) -> None:
        source = text("apps/web/components/WorkspaceControlCenter.tsx")
        for marker in ("Redis", "Email SMTP", "Dịch vụ AI", "Lưu trữ S3", "ONLYOFFICE Docs", "Họp trực tuyến"):
            self.assertIn(marker, source)
        self.assertIn("Mở tài liệu chính thức", source)
        self.assertNotIn('name="configJson"', source)
        backend = "\n".join(path.read_text(encoding="utf-8") for path in (ROOT / "backend/services/configuration-service/src/main/java").rglob("*.java"))
        self.assertIn("validateConfig", backend)
        self.assertIn("HTTP/HTTPS", backend)
        self.assertIn("65535", backend)
        self.assertIn('SMTP', backend)
        self.assertIn('DOCUMENT_EDITOR', backend)

    def test_platform_customizer_can_upload_brand_assets(self) -> None:
        permissions = text("backend/platform-contracts/src/main/java/com/lmspilot/contracts/Permissions.java")
        defaults = text("backend/platform-contracts/src/main/java/com/lmspilot/contracts/DefaultRolePermissions.java")
        self.assertIn("BRANDING_MANAGE", permissions)
        self.assertIn("FILES_READ", permissions)
        self.assertIn("FILES_UPLOAD", permissions)
        self.assertIn("Permissions.BRANDING_MANAGE", defaults)

if __name__ == "__main__":
    unittest.main()
