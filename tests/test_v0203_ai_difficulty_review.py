import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

class Release0203AiDifficultyReviewTests(unittest.TestCase):
    def test_release_version(self):
        self.assertEqual("0.20.4", text("VERSION").strip())
        self.assertIn('"version": "0.20.4"', text("apps/web/package.json"))
        self.assertIn('version = "0.20.4"', text("backend/build.gradle.kts"))

    def test_frontend_has_difficulty_presets_and_review_step(self):
        component = text("apps/web/components/AiQuestionGeneration.tsx")
        self.assertIn('label: "Cân bằng"', component)
        self.assertIn('EASY: 30, MEDIUM: 50, HARD: 20', component)
        self.assertIn('GeneratedQuestionReview', component)
        course = text("apps/web/components/CourseAssessmentsPanel.tsx")
        self.assertIn('Sinh câu hỏi để xem trước', course)
        self.assertIn('selectedExternalIds: reviewSelected', course)
        self.assertNotIn('comments: "Duyệt từ trình biên soạn khóa học"', course)

    def test_backend_enforces_distribution_and_grounding(self):
        contracts = text("backend/services/ai-service/src/main/kotlin/com/lmspilot/ai/platform/QuestionGenerationContracts.kt")
        self.assertIn('object DifficultyDistributionPolicy', contracts)
        self.assertIn('Tổng tỷ lệ độ khó phải bằng 100%', contracts)
        self.assertIn('object GeneratedQuestionQualityValidator', contracts)
        self.assertIn('Trích dẫn không khớp nguyên văn tài liệu nguồn', contracts)
        api = text("backend/services/ai-service/src/main/kotlin/com/lmspilot/ai/api/QuestionGenerationApi.kt")
        self.assertIn('selectedExternalIds', api)
        self.assertIn('GeneratedQuestionQualityValidator.validate', api)
        self.assertIn('callProvider(provider, input, chunks, validation.problems)', api)

    def test_offline_quality_report_is_present(self):
        report = text("docs/ai-quality/0.20.3/AI_QUESTION_QUALITY_REPORT.md")
        self.assertIn('groundedCitations: **10/10**', report)
        self.assertIn('duplicateStems: **0**', report)
        self.assertTrue((ROOT / "docs/ai-quality/0.20.3/mock-generated-question-set.json").exists())
        self.assertTrue((ROOT / "backend/services/ai-service/src/test/resources/fixtures/an-toan-thong-tin.pdf").exists())
        self.assertTrue((ROOT / "backend/services/ai-service/src/test/resources/fixtures/an-toan-thong-tin.docx").exists())

if __name__ == "__main__":
    unittest.main()
