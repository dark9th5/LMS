from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


class Release017AccessibleLmsUiTests(unittest.TestCase):
    def test_legacy_runtime_ui_is_deleted(self) -> None:
        for relative in (
            "apps/web/components/CosmicShell.tsx",
            "apps/web/components/CosmicField.tsx",
            "apps/web/app/cosmic-v011.css",
            "apps/web/app/themes-v012.css",
            "apps/web/app/themes-v013.css",
            "apps/web/app/themes-v014.css",
        ):
            self.assertFalse((ROOT / relative).exists(), relative)
        layout = text("apps/web/app/(portal)/layout.tsx")
        self.assertIn("AppShell", layout)
        self.assertNotIn("CosmicShell", layout)

    def test_one_design_system_supports_light_dark_and_saved_preference(self) -> None:
        layout = text("apps/web/app/layout.tsx")
        css = text("apps/web/app/unified.css")
        self.assertIn('import "./unified.css"', layout)
        self.assertIn('localStorage.getItem("lms-theme")', layout)
        self.assertIn('[data-theme="unified-light"]', css)
        self.assertIn('[data-theme="unified-dark"]', css)
        self.assertIn('window.localStorage.setItem("lms-theme"', text("apps/web/components/AppShell.tsx"))

    def test_typography_controls_focus_and_autofill_are_readable(self) -> None:
        base = text("apps/web/app/globals.css")
        self.assertRegex(base, r"body\{[^}]*font-size:16px")
        self.assertRegex(base, r"input,select,textarea\{[^}]*font-size:16px")
        self.assertIn(":focus-visible", base)
        self.assertIn("input:-webkit-autofill", base)
        self.assertIn("prefers-reduced-motion", base)
        self.assertIn("prefers-contrast:more", text("apps/web/app/unified.css"))

    def test_login_is_modern_and_has_no_legacy_cosmic_controls(self) -> None:
        page = text("apps/web/app/login/page.tsx")
        form = text("apps/web/app/login/LoginForm.tsx")
        combined = page + form
        for marker in ("auth-page", "auth-showcase", "auth-card", "input-shell"):
            self.assertIn(marker, combined)
        for obsolete in ("cosmic-login", "cosmic-input", "Fail-closed", "MEMBER ACCESS"):
            self.assertNotIn(obsolete, combined)
        self.assertIn('autoComplete="username"', form)
        self.assertIn('autoComplete="current-password"', form)

    def test_dashboard_has_no_legacy_hero_or_neon_copy(self) -> None:
        source = text("apps/web/components/RoleDashboard.tsx")
        self.assertNotIn("cosmic-dashboard-hero", source)
        self.assertNotIn("neon", source.lower())
        self.assertIn("role-dashboard", source)
        self.assertIn("roleLabel(role)", source)

    def test_core_navigation_is_short_permission_filtered_and_accessible(self) -> None:
        shell = text("apps/web/components/AppShell.tsx")
        paths = text("apps/web/lib/portal-paths.ts")
        self.assertIn("ROLE_NAVIGATION", shell)
        self.assertIn("navigation.items.map", shell)
        self.assertIn('href="#main-content"', shell)
        for label in ("Người dùng", "Tổ chức", "Khóa học", "Bài thi", "Chấm điểm", "Kết quả"):
            self.assertIn(f'label: "{label}"', shell)
        for route in ("/admin", "/instructor", "/student"):
            self.assertIn(route, paths)
        self.assertNotIn('label: "Lớp học"', shell)

    def test_learning_and_exam_give_content_primary_space(self) -> None:
        css = text("apps/web/app/unified.css")
        self.assertRegex(css, r"\.player-layout\{[^}]*grid-template-columns:minmax\(0,1fr\) 310px")
        self.assertRegex(css, r"\.exam-taking-layout\{[^}]*grid-template-columns:minmax\(0,1fr\) 260px")
        self.assertIn("grid-template-areas:\"content outline\"", css)
        self.assertIn("grid-template-areas:\"question navigator\"", css)
        self.assertIn("font-size:18px", css)
        exam = text("apps/web/components/ExamDetail.tsx")
        self.assertIn("submitConfirmOpen", exam)
        self.assertIn('aria-label={`Câu ${index + 1}', exam)

    def test_catalog_copy_is_vietnamese_and_plain(self) -> None:
        combined = "".join(text(f"apps/web/components/{name}") for name in (
            "CoursesPage.tsx", "CourseAssessmentsPanel.tsx", "LearningPage.tsx", "ExamsPage.tsx"
        ))
        for marker in ("Khóa học", "Bài kiểm tra", "Bài thi"):
            self.assertIn(marker, combined)
        self.assertNotIn("Lớp học", combined)
        self.assertNotIn("cosmic", combined.lower())

    def test_repeatable_fields_and_number_steppers_remain(self) -> None:
        exams = text("apps/web/components/ExamsPage.tsx")
        workspace = text("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn("RepeatableField", exams)
        self.assertIn("NumberStepper", exams)
        self.assertIn("RepeatableField", workspace)
        self.assertNotIn('name="configJson"', workspace)

    def test_every_literal_component_class_has_runtime_css(self) -> None:
        classes: set[str] = set()
        for source_path in (ROOT / "apps/web").rglob("*.tsx"):
            source = source_path.read_text(encoding="utf-8")
            for match in re.finditer(r'className\s*=\s*["\']([^"\']+)["\']', source):
                classes.update(match.group(1).split())
        css = text("apps/web/app/globals.css") + text("apps/web/app/unified.css")
        missing = [
            name for name in sorted(classes)
            if not re.search(r"\." + re.escape(name) + r"(?![\w-])", css)
        ]
        self.assertEqual([], missing)

    def test_fantasy_legacy_copy_and_assets_are_removed(self) -> None:
        combined = "".join(
            path.read_text(encoding="utf-8")
            for path in (ROOT / "apps/web").rglob("*")
            if path.is_file() and path.suffix in {".tsx", ".ts", ".css", ".svg"}
        )
        for obsolete in (
            "LEARNING MANAGEMENT / OPERATIONS", "TRUNG TÂM HỌC TẬP",
            "IDENTITY FORGE", "preview-stars", "orbit-mark.svg",
            "Đang khắc ghi", "cosmic-dashboard-hero",
        ):
            self.assertNotIn(obsolete, combined)

    def test_exam_navigator_styles_only_question_buttons(self) -> None:
        css = text("apps/web/app/unified.css")
        self.assertNotIn(".exam-navigator button{aspect-ratio", css)
        self.assertIn(".exam-navigator>div:nth-child(2)>button{aspect-ratio", css)

    def test_release_prunes_obsolete_preview_bundles(self) -> None:
        self.assertFalse((ROOT / "docs/screenshots").exists())
        self.assertTrue((ROOT / "docs/JAVA_SPRING_MIGRATION_0.21.0.md").exists())

if __name__ == "__main__":
    unittest.main()
