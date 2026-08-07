"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Course, Exam, Question } from "@/lib/models";
import { instructorCourseAssessmentPath } from "@/lib/portal-paths";
import { formatDuration } from "@/lib/models";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Icon } from "./Icon";
import { Modal } from "./Modal";
import { NumberStepper } from "./NumberStepper";
import { StatusBadge } from "./StatusBadge";
import {
  DifficultyDistributionSelector,
  GeneratedQuestionReview,
  type DifficultyDistribution,
  type GeneratedQuestion,
  type GenerationJob,
} from "./AiQuestionGeneration";

type AiProvider = {
  id: string;
  code: string;
  model: string;
  enabled: boolean;
};

type PendingGeneratedAssessment = {
  title: string;
  lessonId: string;
  durationMinutes: number;
  maxAttempts: number;
  passingScore: number;
  status: string;
};

async function waitForGenerationJob(jobId: string): Promise<GenerationJob> {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    const job = await apiRequest<GenerationJob>(
      `/api/v1/ai/question-generation-jobs/${jobId}`,
    );
    if (["REVIEW_REQUIRED", "APPROVED", "FAILED"].includes(job.status)) {
      return job;
    }
    await new Promise((resolve) => window.setTimeout(resolve, 1500));
  }
  throw new Error(
    "Tác vụ AI mất nhiều thời gian hơn dự kiến. Bạn có thể đóng hộp thoại và kiểm tra lại sau.",
  );
}

export function CourseAssessmentsPanel({ course }: { course: Course }) {
  const examLessons = useMemo(
    () => (course.lessons ?? []).filter((lesson) => lesson.type === "EXAM"),
    [course.lessons],
  );
  const documentLessons = useMemo(
    () =>
      (course.lessons ?? []).filter(
        (lesson) =>
          Boolean(lesson.fileId) && ["PDF", "DOCX"].includes(lesson.type),
      ),
    [course.lessons],
  );
  const [exams, setExams] = useState<Exam[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [providers, setProviders] = useState<AiProvider[]>([]);
  const [selectedQuestions, setSelectedQuestions] = useState<string[]>([]);
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");
  const [aiQuestionCount, setAiQuestionCount] = useState(10);
  const [difficultyDistribution, setDifficultyDistribution] =
    useState<DifficultyDistribution>({ EASY: 30, MEDIUM: 50, HARD: 20 });
  const [reviewJob, setReviewJob] = useState<GenerationJob | null>(null);
  const [reviewSelected, setReviewSelected] = useState<string[]>([]);
  const [reviewQuestions, setReviewQuestions] = useState<GeneratedQuestion[]>(
    [],
  );
  const [pendingGeneratedAssessment, setPendingGeneratedAssessment] =
    useState<PendingGeneratedAssessment | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [examPayload, questionPayload, providerPayload] = await Promise.all(
        [
          apiRequest<unknown>("/api/v1/exams"),
          apiRequest<unknown>("/api/v1/questions"),
          apiRequest<unknown>("/api/v1/ai/providers").catch(() => []),
        ],
      );
      setExams(
        unwrapItems<Exam>(examPayload as never).filter(
          (exam) =>
            exam.courseId === course.id && exam.contextType === "COURSE_QUIZ",
        ),
      );
      const courseTag = `course:${course.id}`;
      setQuestions(
        unwrapItems<Question>(questionPayload as never).filter((question) =>
          question.tags.includes(courseTag),
        ),
      );
      setProviders(
        unwrapItems<AiProvider>(providerPayload as never).filter(
          (item) => item.enabled,
        ),
      );
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Không thể tải bài kiểm tra của khóa học",
      );
    } finally {
      setLoading(false);
    }
  }, [course.id]);

  useEffect(() => {
    setSelectedDocuments(
      documentLessons.map((lesson) => lesson.fileId!).filter(Boolean),
    );
  }, [documentLessons]);

  useEffect(() => {
    void load();
  }, [load]);

  async function createAssessment(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    const lessonId = String(data.get("lessonId") ?? "");
    try {
      const created = await apiRequest<Exam>("/api/v1/exams", {
        method: "POST",
        body: JSON.stringify({
          title: String(data.get("title") ?? ""),
          courseId: course.id,
          lessonId,
          contextType: "COURSE_QUIZ",
          autoGrade: data.get("autoGrade") === "on",
          durationMinutes: Number(data.get("durationMinutes") || 15),
          opensAt: null,
          closesAt: null,
          maxAttempts: Number(data.get("maxAttempts") || 2),
          waitMinutesBetweenAttempts: 0,
          passingScore: Number(
            data.get("passingScore") || course.passingScore || 70,
          ),
          shuffleQuestions: data.get("shuffleQuestions") === "on",
          shuffleAnswers: data.get("shuffleAnswers") === "on",
          scoreStrategy: "HIGHEST",
          status: String(data.get("status") ?? "DRAFT"),
          questions: selectedQuestions.map((id, index) => ({
            questionId: id,
            points:
              questions.find((item) => item.id === id)?.defaultPoints ?? 1,
            sortOrder: index + 1,
          })),
        }),
      });
      setCreateOpen(false);
      setSelectedQuestions([]);
      setToast("Đã tạo bài kiểm tra và gắn trực tiếp vào khóa học.");
      await load();
      window.setTimeout(
        () =>
          window.location.assign(
            instructorCourseAssessmentPath(course.id, created.id),
          ),
        200,
      );
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể tạo bài kiểm tra",
      );
    } finally {
      setBusy(false);
    }
  }

  async function generateFromDocuments(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();
    setBusy(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    try {
      if (!selectedDocuments.length)
        throw new Error("Hãy chọn ít nhất một tài liệu PDF hoặc DOCX.");
      const providerConfigId = String(data.get("providerConfigId") ?? "");
      const lessonId = String(data.get("lessonId") ?? "");
      const title = String(data.get("title") ?? "").trim();
      const difficultyTotal = Object.values(difficultyDistribution).reduce(
        (sum, value) => sum + value,
        0,
      );
      if (!providerConfigId) throw new Error("Chưa có cấu hình AI đang bật.");
      if (!lessonId)
        throw new Error("Hãy chọn bài học Bài kiểm tra để gắn đề.");
      if (!title) throw new Error("Hãy nhập tên bài kiểm tra.");
      if (difficultyTotal !== 100)
        throw new Error("Tổng tỷ lệ độ khó phải bằng 100%.");
      const pending: PendingGeneratedAssessment = {
        title,
        lessonId,
        durationMinutes: Number(data.get("durationMinutes") || 15),
        maxAttempts: Number(data.get("maxAttempts") || 2),
        passingScore: Number(
          data.get("passingScore") || course.passingScore || 70,
        ),
        status: String(data.get("status") ?? "DRAFT"),
      };
      const created = await apiRequest<GenerationJob>(
        "/api/v1/ai/question-generation-jobs",
        {
          method: "POST",
          body: JSON.stringify({
            courseId: course.id,
            providerConfigId,
            documentFileIds: selectedDocuments,
            language: "vi",
            numberOfQuestions: aiQuestionCount,
            questionTypes: ["SINGLE_CHOICE", "TRUE_FALSE"],
            difficultyDistribution,
          }),
        },
      );
      const job = ["REVIEW_REQUIRED", "APPROVED", "FAILED"].includes(
        created.status,
      )
        ? created
        : await waitForGenerationJob(created.id);
      if (job.status === "FAILED")
        throw new Error(
          job.errorMessage || "AI không tạo được bộ câu hỏi hợp lệ.",
        );
      const generatedQuestions = job.questionSet?.questions ?? [];
      if (!generatedQuestions.length)
        throw new Error("AI không trả về câu hỏi để xem trước.");
      setPendingGeneratedAssessment(pending);
      setReviewJob(job);
      setReviewQuestions(generatedQuestions);
      setReviewSelected(
        generatedQuestions.map((question) => question.externalId),
      );
      setGenerateOpen(false);
    } catch (caught) {
      setFormError(
        caught instanceof Error
          ? caught.message
          : "Không thể tạo bài kiểm tra từ tài liệu",
      );
    } finally {
      setBusy(false);
    }
  }

  async function approveGeneratedQuestions() {
    if (!reviewJob || !pendingGeneratedAssessment) return;
    if (!reviewSelected.length) {
      setFormError("Hãy chọn ít nhất một câu hỏi để nhập.");
      return;
    }
    setBusy(true);
    setFormError("");
    try {
      if (reviewJob.status === "REVIEW_REQUIRED") {
        await apiRequest(
          `/api/v1/ai/question-generation-jobs/${reviewJob.id}/review`,
          {
            method: "POST",
            body: JSON.stringify({
              decision: "APPROVE",
              comments:
                "Giảng viên đã xem trước và chọn câu hỏi trong trình biên soạn khóa học",
              selectedExternalIds: reviewSelected,
              questionSet: {
                ...reviewJob.questionSet,
                questions: reviewQuestions,
              },
            }),
          },
        );
      }
      const imported = await apiRequest<{ importedQuestionIds: string[] }>(
        `/api/v1/ai/question-generation-jobs/${reviewJob.id}/import`,
        { method: "POST" },
      );
      if (!imported.importedQuestionIds.length)
        throw new Error("Không nhận được câu hỏi đã nhập từ tài liệu.");
      const createdExam = await apiRequest<Exam>("/api/v1/exams", {
        method: "POST",
        body: JSON.stringify({
          title: pendingGeneratedAssessment.title,
          courseId: course.id,
          lessonId: pendingGeneratedAssessment.lessonId,
          contextType: "COURSE_QUIZ",
          autoGrade: true,
          durationMinutes: pendingGeneratedAssessment.durationMinutes,
          opensAt: null,
          closesAt: null,
          maxAttempts: pendingGeneratedAssessment.maxAttempts,
          waitMinutesBetweenAttempts: 0,
          passingScore: pendingGeneratedAssessment.passingScore,
          shuffleQuestions: true,
          shuffleAnswers: true,
          scoreStrategy: "HIGHEST",
          status: pendingGeneratedAssessment.status,
          questions: imported.importedQuestionIds.map((questionId, index) => ({
            questionId,
            points: 1,
            sortOrder: index + 1,
          })),
        }),
      });
      setReviewJob(null);
      setReviewSelected([]);
      setReviewQuestions([]);
      setPendingGeneratedAssessment(null);
      setToast(
        `Đã duyệt ${imported.importedQuestionIds.length} câu và tạo bài kiểm tra. Hãy kiểm tra lần cuối trước khi giao học viên.`,
      );
      await load();
      window.setTimeout(
        () =>
          window.location.assign(
            instructorCourseAssessmentPath(course.id, createdExam.id),
          ),
        200,
      );
    } catch (caught) {
      setFormError(
        caught instanceof Error
          ? caught.message
          : "Không thể duyệt và nhập câu hỏi",
      );
    } finally {
      setBusy(false);
    }
  }

  if (loading)
    return <LoadingState label="Đang tải bài kiểm tra khóa học..." />;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <>
      <section className="section-card course-assessment-panel">
        <div className="section-title">
          <div>
            <h2>Bài kiểm tra trong khóa học</h2>
            <p>
              Bài kiểm tra chỉ thuộc khóa học này, được tạo từ tài liệu bài học
              và hoàn thành ngay trong luồng học.
            </p>
          </div>
          <div className="page-actions">
            <button
              className="button secondary"
              onClick={() => {
                setFormError("");
                setGenerateOpen(true);
              }}
              disabled={!documentLessons.length}
            >
              <Icon name="question" /> Tạo bài kiểm tra từ tài liệu
            </button>
            <button
              className="button primary"
              onClick={() => {
                setFormError("");
                setCreateOpen(true);
              }}
              disabled={!examLessons.length}
            >
              <Icon name="plus" /> Tạo bài kiểm tra
            </button>
          </div>
        </div>

        {!examLessons.length && (
          <div className="form-alert info">
            <Icon name="warning" />
            Hãy thêm một bài học loại “Bài kiểm tra” vào mục lục trước khi tạo
            đề.
          </div>
        )}
        {!documentLessons.length && (
          <div className="form-alert info">
            <Icon name="file" />
            Thêm tài liệu PDF hoặc DOCX để tạo câu hỏi tự động từ đúng nội dung
            khóa học.
          </div>
        )}

        {exams.length ? (
          <div className="question-list course-assessment-list">
            {exams.map((exam, index) => (
              <Link
                className="question-preview"
                href={instructorCourseAssessmentPath(course.id, exam.id)}
                key={exam.id}
              >
                <span className="question-number">{index + 1}</span>
                <div>
                  <div className="question-heading">
                    <strong>{exam.title}</strong>
                    <StatusBadge value={exam.status} />
                  </div>
                  <p>
                    {exam.questions.length} câu ·{" "}
                    {formatDuration(exam.durationMinutes)} · điểm đạt{" "}
                    {exam.passingScore}%
                  </p>
                </div>
                <Icon name="arrow" />
              </Link>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Chưa có bài kiểm tra"
            description="Bài kiểm tra của khóa học sẽ xuất hiện tại đây, không hiển thị chung với kỳ thi độc lập."
          />
        )}
      </section>

      <Modal
        open={createOpen}
        onClose={() => !busy && setCreateOpen(false)}
        title="Tạo bài kiểm tra khóa học"
        description="Đề được gắn với một bài học loại Bài kiểm tra và chỉ dùng trong khóa học này."
      >
        <form className="form-stack" onSubmit={createAssessment}>
          <div className="form-grid two">
            <label>
              Tên bài kiểm tra <b>*</b>
              <input name="title" required />
            </label>
            <label>
              Bài học <b>*</b>
              <select name="lessonId" required>
                <option value="">Chọn bài học</option>
                {examLessons.map((lesson) => (
                  <option value={lesson.id} key={lesson.id}>
                    {lesson.title}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="form-grid four">
            <label>
              Thời lượng
              <NumberStepper
                name="durationMinutes"
                defaultValue={15}
                min={1}
                max={240}
                step={5}
                ariaLabel="Thời lượng"
              />
            </label>
            <label>
              Số lần làm
              <NumberStepper
                name="maxAttempts"
                defaultValue={2}
                min={1}
                max={20}
                ariaLabel="Số lần làm"
              />
            </label>
            <label>
              Điểm đạt
              <NumberStepper
                name="passingScore"
                defaultValue={course.passingScore || 70}
                min={0}
                max={100}
                step={5}
                ariaLabel="Điểm đạt"
              />
            </label>
            <label>
              Trạng thái
              <select name="status" defaultValue="DRAFT">
                <option value="DRAFT">Bản nháp</option>
                <option value="ACTIVE">Hoạt động</option>
              </select>
            </label>
          </div>
          <div className="check-group">
            <label className="check-row">
              <input type="checkbox" name="autoGrade" defaultChecked />
              <span>
                <strong>Tự chấm câu khách quan</strong>
              </span>
            </label>
            <label className="check-row">
              <input type="checkbox" name="shuffleQuestions" />
              <span>
                <strong>Trộn câu hỏi</strong>
              </span>
            </label>
            <label className="check-row">
              <input type="checkbox" name="shuffleAnswers" />
              <span>
                <strong>Trộn phương án</strong>
              </span>
            </label>
          </div>
          <div className="question-picker">
            <div className="section-title">
              <div>
                <h3>Ngân hàng câu hỏi</h3>
                <p>{selectedQuestions.length} câu đã chọn</p>
              </div>
            </div>
            {questions.length ? (
              questions.map((question) => (
                <label className="question-option" key={question.id}>
                  <input
                    type="checkbox"
                    checked={selectedQuestions.includes(question.id)}
                    onChange={(event) =>
                      setSelectedQuestions((current) =>
                        event.target.checked
                          ? [...current, question.id]
                          : current.filter((id) => id !== question.id),
                      )
                    }
                  />
                  <span>
                    <strong>{question.prompt}</strong>
                    <small>
                      {question.type} · {question.defaultPoints} điểm
                    </small>
                  </span>
                </label>
              ))
            ) : (
              <p className="selection-empty">
                Chưa có câu hỏi. Hãy tạo từ tài liệu trước.
              </p>
            )}
          </div>
          {formError && (
            <div className="form-alert error">
              <Icon name="warning" />
              {formError}
            </div>
          )}
          <div className="modal-actions">
            <button
              type="button"
              className="button secondary"
              onClick={() => setCreateOpen(false)}
            >
              Hủy
            </button>
            <button
              className="button primary"
              disabled={busy || !selectedQuestions.length}
            >
              {busy ? "Đang tạo..." : "Tạo bài kiểm tra"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={generateOpen}
        onClose={() => !busy && setGenerateOpen(false)}
        title="Tạo bài kiểm tra từ tài liệu"
        description="AI chỉ đọc PDF/DOCX thuộc khóa học. Bạn chọn độ khó, xem trước từng câu rồi mới duyệt và nhập vào đề."
      >
        <form className="form-stack" onSubmit={generateFromDocuments}>
          <div className="form-grid two">
            <label>
              Tên bài kiểm tra <b>*</b>
              <input
                name="title"
                required
                placeholder="Ví dụ: Bài kiểm tra cuối chương 1"
              />
            </label>
            <label>
              Bài học <b>*</b>
              <select name="lessonId" required>
                <option value="">Chọn bài học</option>
                {examLessons.map((lesson) => (
                  <option value={lesson.id} key={lesson.id}>
                    {lesson.title}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Cấu hình AI <b>*</b>
              <select name="providerConfigId" required>
                <option value="">Chọn cấu hình</option>
                {providers.map((provider) => (
                  <option value={provider.id} key={provider.id}>
                    {provider.code} · {provider.model}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Số câu hỏi
              <NumberStepper
                name="numberOfQuestions"
                defaultValue={10}
                value={aiQuestionCount}
                onChange={setAiQuestionCount}
                min={1}
                max={100}
                ariaLabel="Số câu hỏi"
              />
            </label>
          </div>
          <DifficultyDistributionSelector
            value={difficultyDistribution}
            onChange={setDifficultyDistribution}
            numberOfQuestions={aiQuestionCount}
          />
          <div className="form-grid four">
            <label>
              Thời lượng
              <NumberStepper
                name="durationMinutes"
                defaultValue={15}
                min={1}
                max={240}
                step={5}
                ariaLabel="Thời lượng"
              />
            </label>
            <label>
              Số lần làm
              <NumberStepper
                name="maxAttempts"
                defaultValue={2}
                min={1}
                max={20}
                ariaLabel="Số lần làm"
              />
            </label>
            <label>
              Điểm đạt
              <NumberStepper
                name="passingScore"
                defaultValue={course.passingScore || 70}
                min={0}
                max={100}
                step={5}
                ariaLabel="Điểm đạt"
              />
            </label>
            <label>
              Trạng thái
              <select name="status" defaultValue="DRAFT">
                <option value="DRAFT">Bản nháp</option>
                <option value="ACTIVE">Hoạt động</option>
              </select>
            </label>
          </div>
          <div className="question-picker">
            <div className="section-title">
              <div>
                <h3>Tài liệu nguồn</h3>
                <p>{selectedDocuments.length} tệp đã chọn</p>
              </div>
            </div>
            {documentLessons.map((lesson) => (
              <label className="question-option" key={lesson.id}>
                <input
                  type="checkbox"
                  checked={selectedDocuments.includes(lesson.fileId!)}
                  onChange={(event) =>
                    setSelectedDocuments((current) =>
                      event.target.checked
                        ? [...current, lesson.fileId!]
                        : current.filter((id) => id !== lesson.fileId),
                    )
                  }
                />
                <span>
                  <strong>{lesson.title}</strong>
                  <small>{lesson.type} · nội dung khóa học</small>
                </span>
              </label>
            ))}
          </div>
          {!providers.length && (
            <div className="form-alert info">
              <Icon name="warning" />
              Quản trị viên cần bật một dịch vụ AI trong Cài đặt → Dịch vụ
              ngoài.
            </div>
          )}
          {formError && (
            <div className="form-alert error">
              <Icon name="warning" />
              {formError}
            </div>
          )}
          <div className="modal-actions">
            <button
              type="button"
              className="button secondary"
              onClick={() => setGenerateOpen(false)}
            >
              Hủy
            </button>
            <button
              className="button primary"
              disabled={
                busy ||
                !providers.length ||
                !selectedDocuments.length ||
                !examLessons.length
              }
            >
              {busy ? "AI đang đọc tài liệu..." : "Sinh câu hỏi để xem trước"}
            </button>
          </div>
        </form>
      </Modal>
      <Modal
        open={Boolean(reviewJob)}
        onClose={() => {
          if (!busy) {
            setReviewJob(null);
            setReviewSelected([]);
            setReviewQuestions([]);
            setPendingGeneratedAssessment(null);
            setFormError("");
          }
        }}
        title="Duyệt câu hỏi do AI tạo"
        description="Kiểm tra độ khó, đáp án, lời giải và trích dẫn. Chỉ những câu được chọn mới được nhập vào ngân hàng câu hỏi."
        wide
      >
        {reviewJob && (
          <>
            <GeneratedQuestionReview
              job={reviewJob}
              questions={reviewQuestions}
              selectedIds={reviewSelected}
              onToggle={(id) =>
                setReviewSelected((current) =>
                  current.includes(id)
                    ? current.filter((item) => item !== id)
                    : [...current, id],
                )
              }
              onSelectAll={(selected) =>
                setReviewSelected(
                  selected
                    ? reviewQuestions.map((question) => question.externalId)
                    : [],
                )
              }
              onQuestionChange={(updated) =>
                setReviewQuestions((current) =>
                  current.map((question) =>
                    question.externalId === updated.externalId
                      ? updated
                      : question,
                  ),
                )
              }
            />
            {formError && (
              <div className="form-alert error">
                <Icon name="warning" />
                {formError}
              </div>
            )}
            <div className="modal-actions">
              <button
                type="button"
                className="button secondary"
                onClick={() => {
                  setReviewJob(null);
                  setReviewSelected([]);
                  setReviewQuestions([]);
                  setPendingGeneratedAssessment(null);
                }}
              >
                Hủy bộ câu hỏi
              </button>
              <button
                type="button"
                className="button primary"
                disabled={busy || !reviewSelected.length}
                onClick={() => void approveGeneratedQuestions()}
              >
                {busy
                  ? "Đang nhập câu hỏi..."
                  : `Duyệt và nhập ${reviewSelected.length} câu`}
              </button>
            </div>
          </>
        )}
      </Modal>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </>
  );
}
