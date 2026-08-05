from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


@unittest.skip("Superseded by the LMSPilot 0.16 unified design system")
class Release011CosmicUiTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_previous_visual_systems_are_removed(self) -> None:
        self.assertFalse((ROOT / "apps/web/app/astral-v3.css").exists())
        self.assertFalse((ROOT / "apps/web/components/MysticBackdrop.tsx").exists())
        self.assertFalse((ROOT / "apps/web/public/mystic-mark.svg").exists())
        self.assertFalse((ROOT / "apps/web/components/SpectrumShell.tsx").exists())
        self.assertFalse((ROOT / "apps/web/components/ChromaticField.tsx").exists())
        self.assertFalse((ROOT / "apps/web/public/prism-mark.svg").exists())
        layout = self.read("apps/web/app/layout.tsx")
        self.assertIn('import "./globals.css";', layout)
        self.assertIn('import "./cosmic-v011.css";', layout)
        self.assertNotIn("astral", layout.lower())
        theme = self.read("apps/web/app/cosmic-v011.css")
        self.assertIn("Cosmic Research UI", theme)
        self.assertIn("--cosmic-glow:", theme)
        self.assertNotIn("#ff4f9a", theme.lower())
        self.assertNotIn("#caff4a", theme.lower())

    def test_cosmic_search_is_keyboard_accessible_and_permission_filtered(self) -> None:
        shell = self.read("apps/web/components/CosmicShell.tsx")
        self.assertIn("visibleGroups", shell)
        self.assertIn("permissions.has(permission)", shell)
        self.assertIn('event.key.toLowerCase() === "k"', shell)
        self.assertIn("event.metaKey || event.ctrlKey", shell)
        self.assertIn('role="dialog"', shell)
        self.assertIn('aria-modal="true"', shell)
        self.assertIn("commandItems.map", shell)
        self.assertNotIn('href="#"', shell)

    def test_core_surfaces_use_cosmic_research_structures(self) -> None:
        expected = {
            "apps/web/components/Dashboard.tsx": (
                "cosmic-dashboard-hero",
                "dashboard-progress-art",
                "progress-coordinates",
            ),
            "apps/web/components/CoursesPage.tsx": (
                "course-color-shapes",
                "course-symbol",
                "course-card-index",
            ),
            "apps/web/components/ClassesPage.tsx": (
                "class-atlas",
                "class-number-block",
                "list-entry",
            ),
            "apps/web/components/LearningPage.tsx": (
                "journey-hero",
                "journey-disc",
                "learning-symbol",
            ),
            "apps/web/components/ExamsPage.tsx": (
                "exam-card-scene",
                "exam-color-shapes",
                "exam-card-score",
            ),
        }
        for path, markers in expected.items():
            source = self.read(path)
            for marker in markers:
                self.assertIn(marker, source, f"{path} must include {marker}")

    def test_login_and_password_are_full_cosmic_experiences(self) -> None:
        login = self.read("apps/web/app/login/page.tsx")
        form = self.read("apps/web/app/login/LoginForm.tsx")
        password = self.read("apps/web/app/change-password/page.tsx")
        for marker in (
            "login-showcase",
            "learning-sculpture",
            "login-feature-ticker",
            "login-proof",
        ):
            self.assertIn(marker, login)
        for marker in (
            "cosmic-login-card",
            "cosmic-input",
            "login-security-row",
            "cosmic-login-button",
        ):
            self.assertIn(marker, form)
        for marker in (
            "CosmicField",
            "password-gate-layout",
            "password-shield",
            "password-rules",
        ):
            self.assertIn(marker, password)

    def test_visual_system_is_self_contained(self) -> None:
        theme = (
            self.read("apps/web/app/globals.css")
            + self.read("apps/web/app/cosmic-v011.css")
            + self.read("apps/web/app/themes-v012.css")
        ).lower()
        self.assertNotIn("@import", theme)
        self.assertNotIn("http://", theme)
        self.assertNotIn("https://", theme)
        self.assertNotIn("url(", theme)
        self.assertIn("segoe ui", theme)
        self.assertIn("arial black", theme)

    def test_responsive_motion_and_contrast_preferences_are_explicit(self) -> None:
        theme = self.read("apps/web/app/globals.css") + self.read(
            "apps/web/app/cosmic-v011.css"
        ) + self.read("apps/web/app/themes-v012.css")
        for breakpoint in ("1480px", "1240px", "1080px", "900px", "720px", "480px"):
            self.assertIn(f"max-width: {breakpoint}", theme)
        self.assertIn("prefers-reduced-motion: reduce", theme)
        self.assertIn("prefers-contrast: more", theme)
        self.assertRegex(
            theme,
            r"animation-duration:\s*0?\.01ms\s*!important",
        )
        shell = self.read("apps/web/components/CosmicShell.tsx")
        self.assertIn('aria-label="Điều hướng chính"', shell)
        self.assertIn('aria-label="Tìm kiếm và chuyển trang"', shell)

    def test_advanced_business_centers_share_the_new_material_language(self) -> None:
        theme = self.read("apps/web/app/cosmic-v011.css")
        for selector in (
            ".workspace-hero",
            ".workspace-panel",
            ".workspace-table-wrap",
            ".advanced-panel",
            ".learning-path-card",
            ".import-step",
            ".grading-card",
        ):
            self.assertIn(selector, theme)

    def test_page_headers_are_consistent_on_specialized_workflows(self) -> None:
        header = self.read("apps/web/components/PageHeader.tsx")
        self.assertIn("cosmic-page-header", header)
        self.assertIn("page-header-orbit", header)
        self.assertIn("page-header-sculpture", header)
        self.assertIn("<PageHeader", self.read("apps/web/components/LearningPathCenter.tsx"))
        self.assertIn("<PageHeader", self.read("apps/web/components/UserImportWizard.tsx"))


if __name__ == "__main__":
    unittest.main()
