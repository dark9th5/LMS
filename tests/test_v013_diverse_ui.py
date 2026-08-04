from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class Release013DiverseUiTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_catalog_targets_distinct_audiences_and_personalities(self) -> None:
        source = self.read("apps/web/lib/themes.ts")
        for category in ("business", "education", "institution", "creative", "minimal"):
            self.assertIn(f'{category}: "', source)
        for phrase in (
            "Sắc màu Cân bằng",
            "Điều hành Cao cấp",
            "Học viện Di sản",
            "Trường học Năng động",
            "Tổ chức Tin cậy",
            "Xưởng Sáng tạo",
            "Giáo dục Xanh",
            "Tạp chí Cổ điển",
            "Tối giản An nhiên",
            "Trung tâm Công nghệ",
        ):
            self.assertIn(phrase, source)

    def test_new_runtime_catalog_does_not_use_space_theme_keys(self) -> None:
        source = self.read("apps/web/lib/themes.ts")
        for obsolete in (
            "cosmic-observatory",
            "quantum-cyan",
            "mars-expedition",
            "abyssal-ocean",
            "solar-archive",
        ):
            self.assertNotIn(obsolete, source)

    def test_sidebar_is_permission_aware_two_level_accordion(self) -> None:
        source = self.read("apps/web/components/CosmicShell.tsx")
        for marker in (
            'label: "Học tập"',
            'label: "Đánh giá"',
            'label: "Quản trị"',
            "expandedGroup",
            "nav-group-toggle",
            "nav-group-panel",
            "nav-group-items-inner",
            "aria-expanded={expanded}",
            "aria-controls={panelId}",
        ):
            self.assertIn(marker, source)
        self.assertIn("visibleGroups.map", source)
        self.assertIn("permissions.has(permission)", source)

    def test_theme_css_changes_structure_not_only_color(self) -> None:
        source = self.read("apps/web/app/themes-v013.css")
        for marker in (
            "Georgia",
            "Arial Rounded MT Bold",
            "Cascadia Mono",
            "border-style: double",
            "border-left-width: 3px",
            'html[data-theme="nature-learning"] .cosmic-dashboard-hero',
            "border-radius: 28px",
            "box-shadow: none",
            "background-size: 18px 18px",
        ):
            self.assertIn(marker, source)

    def test_user_facing_shell_language_is_context_neutral(self) -> None:
        shell = self.read("apps/web/components/CosmicShell.tsx")
        login = self.read("apps/web/app/login/page.tsx")
        dashboard = self.read("apps/web/components/Dashboard.tsx")
        combined = shell + login + dashboard
        for obsolete in (
            "ORBITAL LEARNING",
            "KNOWLEDGE OBSERVATORY",
            "LIVE TELEMETRY",
            "DỮ LIỆU QUỸ ĐẠO",
        ):
            self.assertNotIn(obsolete, combined)


if __name__ == "__main__":
    unittest.main()
