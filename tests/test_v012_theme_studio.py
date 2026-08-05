from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
THEME_KEYS = (
    "soft-spectrum",
    "executive-midnight",
    "heritage-academy",
    "bright-school",
    "civic-trust",
    "creative-pop",
    "nature-learning",
    "editorial-burgundy",
    "minimal-calm",
    "digital-grid",
)


@unittest.skip("Superseded by the LMSPilot 0.16 unified design system")
class Release012ThemeStudioTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_registry_contains_ten_unique_complete_themes(self) -> None:
        source = self.read("apps/web/lib/themes.ts")
        keys = re.findall(r'key: "([a-z0-9-]+)"', source)
        self.assertEqual(list(THEME_KEYS), keys)
        self.assertEqual(10, len(set(keys)))
        for field in ("name", "description", "mode", "category", "palette"):
            self.assertGreaterEqual(source.count(f"{field}:"), 10)
        self.assertIn('DEFAULT_THEME_KEY: ThemeKey = "soft-spectrum"', source)
        self.assertIn("normalizeThemeKey", source)

    def test_root_layout_applies_validated_theme_server_side(self) -> None:
        layout = self.read("apps/web/app/layout.tsx")
        branding = self.read("apps/web/lib/branding.ts")
        self.assertIn('import "./themes-v012.css";', layout)
        self.assertIn('import "./themes-v013.css";', layout)
        self.assertIn('import "./themes-v014.css";', layout)
        self.assertIn("normalizeThemeKey(branding.themeKey)", layout)
        self.assertIn("data-theme={themeKey}", layout)
        self.assertIn("generateViewport", layout)
        self.assertIn("themeKey: string", branding)
        self.assertIn('themeKey: "soft-spectrum"', branding)

    def test_theme_studio_supports_search_filter_preview_reset_and_apply(self) -> None:
        source = self.read("apps/web/components/WorkspaceControlCenter.tsx")
        for marker in (
            "Theme Studio",
            "theme-search",
            "theme-filters",
            "theme-gallery",
            "previewTheme",
            "resetThemePreview",
            "applySelectedTheme",
            "applyBrandingPreview",
            "Áp dụng toàn hệ thống",
        ):
            self.assertIn(marker, source)
        self.assertIn('apiRequest<BrandingRow>("/api/v1/branding"', source)
        self.assertIn("themeKey: normalizeThemeKey(nextBrand.themeKey)", source)
        self.assertNotIn("localStorage", source)

    def test_backend_persists_and_validates_theme_key(self) -> None:
        entity = self.read(
            "backend/services/configuration-service/src/main/java/com/lmspilot/configuration/domain/CustomizationDomain.java"
        )
        api = self.read(
            "backend/services/configuration-service/src/main/java/com/lmspilot/configuration/api/CustomizationApi.java"
        )
        migration = self.read(
            "backend/services/configuration-service/src/main/resources/db/migration/V6__soft_spectrum_default.sql"
        )
        self.assertIn('@Column(name = "theme_key"', entity)
        self.assertIn("THEME_PATTERN", api)
        self.assertIn("entity.themeKey = input.themeKey", api)
        self.assertIn("themeKey = themeKey", api)
        self.assertIn("ALTER COLUMN theme_key SET DEFAULT", migration)
        self.assertIn("ck_branding_theme_key", migration)
        for key in THEME_KEYS:
            self.assertIn(key, api)
            self.assertIn(key, migration)

    def test_all_themes_have_css_tokens(self) -> None:
        theme = self.read("apps/web/app/themes-v013.css") + self.read(
            "apps/web/app/themes-v014.css"
        )
        for key in THEME_KEYS:
            self.assertIn(f'html[data-theme="{key}"]', theme)
        for token in (
            "--preset-page:",
            "--preset-surface:",
            "--preset-primary:",
            "--preset-secondary:",
            "--preset-text:",
            "--preset-radius:",
            "--preset-display-font:",
        ):
            self.assertGreaterEqual(theme.count(token), 10)

    def test_theme_adapter_covers_core_and_admin_surfaces(self) -> None:
        theme = self.read("apps/web/app/themes-v012.css") + self.read(
            "apps/web/app/themes-v013.css"
        ) + self.read("apps/web/app/themes-v014.css")
        for selector in (
            ".cosmic-sidebar",
            ".cosmic-topbar",
            ".cosmic-dashboard-hero",
            ".cosmic-page-header",
            ".course-card",
            ".learning-card",
            ".exam-card",
            ".workspace-panel",
            ".modal-card",
            ".cosmic-login-card",
            ".theme-card",
        ):
            self.assertIn(selector, theme)
        self.assertIn('html[data-theme="heritage-academy"]', theme)
        self.assertIn('html[data-theme="digital-grid"] body', theme)

    def test_theme_system_remains_self_contained_and_accessible(self) -> None:
        theme = self.read("apps/web/app/themes-v013.css").lower()
        self.assertNotIn("@import", theme)
        self.assertNotIn("http://", theme)
        self.assertNotIn("https://", theme)
        self.assertNotIn("url(", theme)
        self.assertIn("prefers-reduced-motion: reduce", theme)
        studio = self.read("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn('aria-labelledby="theme-studio-title"', studio)
        self.assertIn('aria-label="Tìm chủ đề"', studio)
        self.assertIn("aria-pressed={selected}", studio)
        self.assertIn("data-preview-theme={theme.key}", studio)


if __name__ == "__main__":
    unittest.main()
