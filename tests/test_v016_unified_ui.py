from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


@unittest.skip("Superseded by the complete LMSPilot 0.17 interface rebuild")
class Release016UnifiedUiTests(unittest.TestCase):
    def test_only_unified_runtime_styles_are_loaded(self) -> None:
        layout = text("apps/web/app/layout.tsx")
        self.assertIn('import "./globals.css";', layout)
        self.assertIn('import "./unified.css";', layout)
        for obsolete in (
            "cosmic-v011.css",
            "themes-v012.css",
            "themes-v013.css",
            "themes-v014.css",
        ):
            self.assertNotIn(obsolete, layout)
            self.assertFalse((ROOT / "apps/web/app" / obsolete).exists())

    def test_theme_registry_is_one_system_with_light_and_dark_modes(self) -> None:
        registry = text("apps/web/lib/themes.ts")
        keys = re.findall(r'key: "([a-z0-9-]+)"', registry)
        self.assertEqual(["unified-light", "unified-dark"], keys)
        self.assertIn('DEFAULT_THEME_KEY: ThemeKey = "unified-light"', registry)
        self.assertIn('"executive-midnight"', registry)
        self.assertIn('"digital-grid"', registry)
        self.assertIn('return LEGACY_DARK.has', registry)

    def test_login_controls_keep_explicit_readable_states(self) -> None:
        css = text("apps/web/app/unified.css")
        form = text("apps/web/app/login/LoginForm.tsx")
        page = text("apps/web/app/login/page.tsx")
        for marker in (
            "--ui-surface:",
            "--ui-text:",
            "--ui-muted:",
            "input:-webkit-autofill",
            ":focus-visible",
            "font-size: 16px",
        ):
            self.assertIn(marker, css)
        self.assertIn('autoComplete="username"', form)
        self.assertIn('autoComplete="current-password"', form)
        self.assertIn("aria-describedby", form)
        self.assertNotIn("Fail-closed", page)
        self.assertNotIn("19 dịch vụ", page)

    def test_learning_and_exam_content_receive_primary_space(self) -> None:
        css = text("apps/web/app/unified.css")
        self.assertIn(".player-layout", css)
        self.assertIn(".exam-taking-layout", css)
        self.assertIn("grid-template-columns: minmax(0, 1fr) 310px", css)
        self.assertIn(".player-content,", css)
        self.assertIn(".exam-question-panel", css)
        self.assertIn("grid-column: 1", css)
        self.assertIn("font-size: 1.125rem", css)

    def test_admin_navigation_contains_only_core_product_areas(self) -> None:
        shell = text("apps/web/components/CosmicShell.tsx")
        for href in (
            'href: "/users"',
            'href: "/organization"',
            'href: "/settings"',
            'href: "/courses"',
            'href: "/classes"',
            'href: "/exams"',
        ):
            self.assertIn(href, shell)
        for href in (
            "/competitions",
            "/ai-lab",
            "/competencies",
            "/operations",
            "/notification-automation",
        ):
            self.assertNotIn(href, shell)

    def test_non_core_centers_are_retired_from_public_routes(self) -> None:
        route = text("apps/web/app/(portal)/[section]/page.tsx")
        for section in (
            '"competitions"',
            '"ai-lab"',
            '"competencies"',
            '"operations"',
            '"notification-automation"',
        ):
            self.assertIn(section, route)
        self.assertIn("notFound()", route)

    def test_structured_inputs_replace_delimited_question_fields(self) -> None:
        exams = text("apps/web/components/ExamsPage.tsx")
        workspace = text("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn("RepeatableField", exams)
        self.assertIn("NumberStepper", exams)
        self.assertIn('.getAll("options")', exams)
        self.assertIn('.getAll("correctAnswers")', exams)
        self.assertIn("RepeatableField", workspace)
        self.assertNotIn('name="configJson"', workspace)

    def test_configuration_service_accepts_and_migrates_unified_modes(self) -> None:
        api = text("backend/services/configuration-service/src/main/kotlin/com/lmspilot/configuration/api/CustomizationApi.kt")
        domain = text("backend/services/configuration-service/src/main/kotlin/com/lmspilot/configuration/domain/CustomizationDomain.kt")
        migration = text("backend/services/configuration-service/src/main/resources/db/migration/V7__unified_design_system.sql")
        self.assertIn('^(unified-light|unified-dark)$', api)
        self.assertIn('themeKey: String = "unified-light"', api)
        self.assertIn('themeKey: String = "unified-light"', domain)
        self.assertIn("executive-midnight", migration)
        self.assertIn("digital-grid", migration)
        self.assertIn("theme_key IN ('unified-light', 'unified-dark')", migration)

    def test_brand_text_is_calculated_instead_of_trusted(self) -> None:
        branding = text("apps/web/lib/branding.ts")
        color = text("apps/web/lib/color.ts")
        self.assertIn('"--brand-on-primary": readableText(primary)', branding)
        self.assertIn('"--brand-on-background": readableText(background)', branding)
        self.assertIn("whiteContrast", color)
        self.assertIn("darkContrast", color)


if __name__ == "__main__":
    unittest.main()
