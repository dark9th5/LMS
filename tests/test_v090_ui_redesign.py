from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class Release090UiRedesignTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_astral_design_system_is_loaded_after_compatibility_styles(self) -> None:
        layout = self.read("apps/web/app/layout.tsx")
        self.assertIn('import "./globals.css";', layout)
        self.assertIn('import "./astral-v3.css";', layout)
        self.assertLess(layout.index('import "./globals.css";'), layout.index('import "./astral-v3.css";'))
        theme = self.read("apps/web/app/astral-v3.css")
        self.assertIn("--v3-void:", theme)
        self.assertIn("--v3-serif:", theme)
        self.assertIn(".app-shell.immersive-shell", theme)

    def test_command_atlas_is_keyboard_accessible_and_uses_allowed_navigation(self) -> None:
        shell = self.read("apps/web/components/PortalShell.tsx")
        self.assertIn("const visibleItems = visibleGroups.flatMap", shell)
        self.assertIn("const commandItems = visibleItems.filter", shell)
        self.assertIn('event.key.toLowerCase() === "k"', shell)
        self.assertIn("event.metaKey || event.ctrlKey", shell)
        self.assertIn('role="dialog"', shell)
        self.assertIn('aria-modal="true"', shell)
        self.assertIn("commandItems.map", shell)
        self.assertNotIn('href="#"', shell)

    def test_core_surfaces_use_new_spatial_structures(self) -> None:
        expected = {
            "apps/web/components/Dashboard.tsx": ("command-welcome", "dashboard-route-deck", "welcome-orbit"),
            "apps/web/components/CoursesPage.tsx": ("course-cover-map", "course-sigil", "course-card-index"),
            "apps/web/components/ClassesPage.tsx": ("class-atlas", "class-rune", "list-entry"),
            "apps/web/components/LearningPage.tsx": ("journey-hero", "journey-orbit", "learning-cover-orbit"),
            "apps/web/components/ExamsPage.tsx": ("exam-card-scene", "exam-card-map", "exam-card-score"),
        }
        for path, markers in expected.items():
            source = self.read(path)
            for marker in markers:
                self.assertIn(marker, source, f"{path} must include {marker}")

    def test_login_and_password_pages_are_full_experiences(self) -> None:
        login = self.read("apps/web/app/login/page.tsx")
        form = self.read("apps/web/app/login/LoginForm.tsx")
        password = self.read("apps/web/app/change-password/page.tsx")
        for marker in ("login-visual-frame", "portal-observatory", "login-manifesto", "realm-features"):
            self.assertIn(marker, login)
        for marker in ("login-card-ornament", "field-title", "login-trust-grid", "secure-note"):
            self.assertIn(marker, form)
        for marker in ("MysticBackdrop", "password-gate-layout", "password-shield", "password-rules"):
            self.assertIn(marker, password)

    def test_theme_is_self_contained_and_does_not_fetch_visual_dependencies(self) -> None:
        theme = self.read("apps/web/app/astral-v3.css").lower()
        self.assertNotIn("@import", theme)
        self.assertNotIn("http://", theme)
        self.assertNotIn("https://", theme)
        self.assertNotIn("url(", theme)
        self.assertIn("georgia", theme)
        self.assertIn("system-ui", theme)

    def test_responsive_motion_and_contrast_preferences_are_explicit(self) -> None:
        theme = self.read("apps/web/app/astral-v3.css")
        for breakpoint in ("1480px", "1240px", "1080px", "900px", "720px", "480px"):
            self.assertIn(f"max-width: {breakpoint}", theme)
        self.assertIn("prefers-reduced-motion: reduce", theme)
        self.assertIn("prefers-contrast: more", theme)
        self.assertIn("animation: none !important", theme)
        shell = self.read("apps/web/components/PortalShell.tsx")
        self.assertIn('aria-label="Điều hướng chính"', shell)
        self.assertIn('aria-label="Tìm kiếm và chuyển trang"', shell)

    def test_advanced_business_centers_share_the_new_material_language(self) -> None:
        theme = self.read("apps/web/app/astral-v3.css")
        for selector in (
            ".realm-hero",
            ".realm-panel",
            ".realm-table-wrap",
            ".realm-grid-three",
            ".advanced-grid",
            ".learning-path-card",
            ".import-step",
            ".grading-card",
        ):
            self.assertIn(selector, theme)

    def test_page_headers_are_consistent_on_specialized_workflows(self) -> None:
        header = self.read("apps/web/components/PageHeader.tsx")
        self.assertIn("celestial-page-header", header)
        self.assertIn("page-header-map", header)
        self.assertIn("page-coordinate", header)
        self.assertIn("<PageHeader", self.read("apps/web/components/LearningPathCenter.tsx"))
        self.assertIn("<PageHeader", self.read("apps/web/components/UserImportWizard.tsx"))


if __name__ == "__main__":
    unittest.main()
