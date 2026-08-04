from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


@unittest.skip("Superseded by the CLS 0.16 unified design system")
class Release014SoftSpectrumTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_soft_spectrum_is_the_default_balanced_theme(self) -> None:
        registry = self.read("apps/web/lib/themes.ts")
        branding = self.read("apps/web/lib/branding.ts")
        self.assertIn('key: "soft-spectrum"', registry)
        self.assertIn('name: "Sắc màu Cân bằng"', registry)
        self.assertIn('DEFAULT_THEME_KEY: ThemeKey = "soft-spectrum"', registry)
        self.assertIn('themeKey: "soft-spectrum"', branding)
        for color in ("#B95547", "#5967B8", "#F6F3EF", "#20232E"):
            self.assertIn(color, registry)
            self.assertIn(color, branding)

    def test_sidebar_is_explicitly_monochrome_across_all_groups(self) -> None:
        css = self.read("apps/web/app/themes-v014.css")
        for marker in (
            "--spectrum-rail: #191b28",
            ".accent-cyan .nav-group-toggle",
            ".accent-indigo .nav-group-toggle",
            ".accent-violet .nav-group-toggle",
            ".accent-cyan .nav-link.active",
            ".accent-indigo .nav-link.active",
            ".accent-violet .nav-link.active",
            "background: #f0ede7",
            "box-shadow: none",
        ):
            self.assertIn(marker, css)

    def test_content_keeps_soft_multicolour_hierarchy(self) -> None:
        css = self.read("apps/web/app/themes-v014.css")
        for token in (
            "--spectrum-coral: #e8927e",
            "--spectrum-rose: #e8a1bb",
            "--spectrum-aqua: #77c7cc",
            "--spectrum-violet: #9490db",
            "--spectrum-lime: #b7cf83",
            "--spectrum-yellow: #e8c96e",
            "#edf4df",
            "#dff2f3",
            "#f8edcf",
            "#ece8f7",
        ):
            self.assertIn(token, css)
        self.assertIn('data-preview-theme="soft-spectrum"', css)

    def test_v6_migrates_default_without_overwriting_custom_palette(self) -> None:
        migration = self.read(
            "backend/services/configuration-service/src/main/resources/db/migration/V6__soft_spectrum_default.sql"
        )
        self.assertIn("WHERE theme_key = 'enterprise-blue'", migration)
        self.assertIn("SET theme_key = 'soft-spectrum'", migration)
        self.assertIn("ALTER COLUMN theme_key SET DEFAULT 'soft-spectrum'", migration)
        self.assertIn("Custom branding survives", migration)
        self.assertIn("upper(primary_color) = '#2563EB'", migration)
        self.assertIn("ck_branding_theme_key", migration)

    def test_progress_art_uses_theme_tokens_and_css_stays_self_contained(self) -> None:
        dashboard = self.read("apps/web/components/Dashboard.tsx")
        css = self.read("apps/web/app/themes-v014.css").lower()
        self.assertIn("var(--progress-accent, #72ead6)", dashboard)
        self.assertIn("var(--progress-track", dashboard)
        self.assertNotIn("@import", css)
        self.assertNotIn("http://", css)
        self.assertNotIn("https://", css)
        self.assertNotIn("url(", css)
        self.assertIn("prefers-reduced-motion: reduce", css)


if __name__ == "__main__":
    unittest.main()
