import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

class Release0202VisualSpacingTests(unittest.TestCase):
    def test_release_version(self):
        self.assertEqual("0.24.0", text("VERSION").strip())

    def test_answer_choices_use_letter_tiles(self):
        source = text("apps/web/components/ExamDetail.tsx")
        self.assertIn('className="answer-letter"', source)
        self.assertIn('className="answer-text"', source)
        self.assertIn('className={`answer-choice ${checked ? "selected" : ""}`}', source)

    def test_organization_tree_separates_name_and_metadata(self):
        source = text("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn('className="org-node-name"', source)
        self.assertIn('className="org-node-side"', source)

    def test_spacing_css_exists(self):
        css = text("apps/web/app/unified.css")
        self.assertIn("0.20.2 visual-spacing repair", css)
        self.assertIn(".exam-grid{gap:22px}", css)
        self.assertIn(".answer-choice.selected", css)

if __name__ == "__main__":
    unittest.main()
