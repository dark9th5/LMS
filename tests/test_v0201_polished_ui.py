from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


class Release0201PolishedUiTests(unittest.TestCase):
    def test_release_version(self) -> None:
        self.assertEqual("0.21.0", text("VERSION").strip())
        self.assertIn('"version": "0.21.0"', text("apps/web/package.json"))
        self.assertIn('version = "0.21.0"', text("backend/build.gradle.kts"))

    def test_sidebar_keeps_role_separation_without_crowding(self) -> None:
        shell = text("apps/web/components/AppShell.tsx")
        self.assertNotIn('className="role-identity"', shell)
        self.assertIn("roleLabel(role)", shell)
        self.assertIn("navigation.workspaceLabel", shell)
        self.assertIn("data-portal-role", shell)

    def test_polish_layer_is_present(self) -> None:
        css = text("apps/web/app/unified.css")
        self.assertIn("LMSPilot 0.20.1 — visual polish pass", css)
        self.assertIn("--sidebar-width:228px", css)
        self.assertIn(".role-identity{display:none!important}", css)
        self.assertIn("Dashboard returns to the balanced 0.18 proportions", css)

    def test_dark_mode_keeps_muted_borders(self) -> None:
        css = text("apps/web/app/unified.css")
        self.assertIn("--ui-border:#2a374b", css)
        self.assertNotIn("--ui-border:#ffffff", css.lower())
        self.assertIn("border-color:var(--ui-border)!important", css)

    def test_historical_preview_bundle_is_pruned(self) -> None:
        self.assertFalse((ROOT / "docs/screenshots/0.20.1").exists())
        self.assertTrue((ROOT / "apps/web/app/unified.css").exists())

if __name__ == "__main__":
    unittest.main()
