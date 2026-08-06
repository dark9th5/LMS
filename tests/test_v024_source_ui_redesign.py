import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Release024SourceUiRedesignTests(unittest.TestCase):
    def test_release_version_is_aligned(self):
        self.assertEqual("0.24.0", text("VERSION").strip())
        self.assertIn('"version": "0.24.0"', text("apps/web/package.json"))
        self.assertIn('version = "0.24.0"', text("backend/build.gradle.kts"))
        self.assertIn("lmspilot/backend-bundle:0.24.0", text("docker-compose.yml"))

    def test_organization_uses_real_chart_components(self):
        source = text("apps/web/components/WorkspaceControlCenter.tsx")
        for token in (
            "OrganizationChart",
            "OrganizationChartBranch",
            'className="org-summary-grid"',
            'className="org-chart-panel"',
            "Sơ đồ cơ cấu tổ chức",
        ):
            self.assertIn(token, source)

    def test_exam_editor_loads_and_updates_question_bank(self):
        source = text("apps/web/components/ExamDetail.tsx")
        for token in (
            'apiRequest<unknown>("/api/v1/questions?size=250")',
            "updateExamQuestions",
            'className="exam-editor-source-layout"',
            "exam.questions.length <= 1",
        ):
            self.assertIn(token, source)

    def test_answer_letter_tiles_replace_visible_browser_radio(self):
        source = text("apps/web/components/ExamDetail.tsx")
        css = text("apps/web/app/unified.css")
        self.assertIn('className="answer-letter"', source)
        self.assertIn(".answer-choice input{position:absolute!important", css)
        self.assertIn(".answer-choice.selected .answer-letter", css)
        self.assertIn("grid-template-columns:50px minmax(0,1fr)", css)

    def test_modal_and_sidebar_are_viewport_safe(self):
        css = text("apps/web/app/unified.css")
        for token in (
            "max-height:calc(100dvh",
            ".modal-body{min-height:0;overflow:auto",
            ".entity-form-actions{position:sticky",
            ".sidebar-footer{position:relative;flex:0 0 auto",
        ):
            self.assertIn(token, css)

    def test_source_ui_qa_outputs_are_reproducible(self):
        manifest_path = ROOT / "docs/screenshots/0.24.0/SOURCE_UI_QA_MANIFEST.json"
        self.assertTrue(manifest_path.exists())
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
        self.assertEqual("0.24.0", data["version"])
        self.assertEqual(4, len(data["screens"]))
        for screen in data["screens"]:
            self.assertTrue((manifest_path.parent / screen["file"]).exists())
            self.assertTrue((ROOT / screen["html"]).exists())


if __name__ == "__main__":
    unittest.main()
