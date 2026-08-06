"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Exam, Question } from "@/lib/models";
import { formatDate, formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { resolvePortalRole } from "@/lib/role";
import { standaloneExamPath } from "@/lib/portal-paths";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Modal } from "./Modal";
import { NumberStepper } from "./NumberStepper";
import { RepeatableField } from "./RepeatableField";
import { StatusBadge } from "./StatusBadge";
import { DifficultyDistributionSelector, GeneratedQuestionReview, type DifficultyDistribution, type GeneratedQuestion, type GenerationJob } from "./AiQuestionGeneration";

type AiProvider = { id: string; code: string; model: string; enabled: boolean };
type PendingGeneratedExam = {
  title: string;
  durationMinutes: number;
  maxAttempts: number;
  passingScore: number;
  status: string;
};

const STANDALONE_QUESTION_WORKSPACE_ID = "00000000-0000-0000-0000-00000000a11e";

async function waitForStandaloneGeneration(jobId: string): Promise<GenerationJob> {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    const job = await apiRequest<GenerationJob>(`/api/v1/ai/question-generation-jobs/${jobId}`);
    if (["REVIEW_REQUIRED", "APPROVED", "FAILED"].includes(job.status)) return job;
    await new Promise((resolve) => window.setTimeout(resolve, 1500));
  }
  throw new Error("Tác vụ AI mất nhiều thời gian hơn dự kiến. Vui lòng kiểm tra lại sau.");
}

export function ExamsPage({
  user,
  standaloneOnly = false,
}: {
  user: PortalUser;
  standaloneOnly?: boolean;
}) {
  const role = resolvePortalRole(user);
  const canManage = role === "INSTRUCTOR";
  const [exams, setExams] = useState<Exam[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [providers, setProviders] = useState<AiProvider[]>([]);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [sortOrder, setSortOrder] = useState("NEWEST");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [examModal, setExamModal] = useState(false);
  const [documentExamModal, setDocumentExamModal] = useState(false);
  const [questionModal, setQuestionModal] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState<Question | null>(null);
  const [selectedQuestions, setSelectedQuestions] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");
  const [aiQuestionCount, setAiQuestionCount] = useState(20);
  const [difficultyDistribution, setDifficultyDistribution] = useState<DifficultyDistribution>({ EASY: 30, MEDIUM: 50, HARD: 20 });
  const [reviewJob, setReviewJob] = useState<GenerationJob | null>(null);
  const [reviewSelected, setReviewSelected] = useState<string[]>([]);
  const [reviewQuestions, setReviewQuestions] = useState<GeneratedQuestion[]>([]);
  const [pendingGeneratedExam, setPendingGeneratedExam] = useState<PendingGeneratedExam | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [examData, questionData, providerData] = await Promise.all([
        apiRequest<unknown>("/api/v1/exams"),
        canManage ? apiRequest<unknown>("/api/v1/questions") : Promise.resolve([]),
        canManage
          ? apiRequest<unknown>("/api/v1/ai/providers").catch(() => [])
          : Promise.resolve([]),
      ]);
      const loadedExams = unwrapItems<Exam>(examData as never);
      setExams(
        standaloneOnly
          ? loadedExams.filter((exam) => !exam.courseId && exam.contextType !== "COURSE_QUIZ")
          : loadedExams,
      );
      if (canManage) {
        setQuestions(unwrapItems<Question>(questionData as never));
        setProviders(
          unwrapItems<AiProvider>(providerData as never).filter((item) => item.enabled),
        );
      }
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể tải bài kiểm tra",
      );
    } finally {
      setLoading(false);
    }
  }, [canManage, standaloneOnly]);

  useEffect(() => {
    void load();
  }, [load]);

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi-VN");
    const result = exams.filter((exam) => {
      const matchesQuery = `${exam.title} ${exam.contextType ?? ""} ${exam.id}`
        .toLocaleLowerCase("vi-VN")
        .includes(normalizedQuery);
      return matchesQuery && (statusFilter === "ALL" || exam.status === statusFilter);
    });
    return [...result].sort((left, right) => {
      if (sortOrder === "TITLE") return left.title.localeCompare(right.title, "vi");
      const leftTime = Date.parse(left.opensAt ?? left.closesAt ?? "") || 0;
      const rightTime = Date.parse(right.opensAt ?? right.closesAt ?? "") || 0;
      return sortOrder === "OLDEST" ? leftTime - rightTime : rightTime - leftTime;
    });
  }, [exams, query, sortOrder, statusFilter]);

  const examStats = useMemo(() => ({
    total: exams.length,
    active: exams.filter((item) => item.status === "ACTIVE").length,
    draft: exams.filter((item) => item.status === "DRAFT").length,
    completed: exams.filter((item) => ["INACTIVE", "ARCHIVED"].includes(item.status)).length,
  }), [exams]);

  function openQuestion(question: Question | null = null) {
    setEditingQuestion(question);
    setFormError("");
    setQuestionModal(true);
  }

  async function saveQuestion(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    const type = String(data.get("type") ?? "SINGLE_CHOICE");
    const rawOptions = data
      .getAll("options")
      .map(String)
      .map((value) => value.trim())
      .filter(Boolean);
    const options = type === "TRUE_FALSE" ? ["Đúng", "Sai"] : rawOptions;
    const correctAnswers = data
      .getAll("correctAnswers")
      .map(String)
      .map((value) => value.trim())
      .filter(Boolean);
    const payload = {
      type,
      prompt: String(data.get("prompt") ?? ""),
      options,
      correctAnswers,
      explanation: String(data.get("explanation") ?? "") || null,
      difficulty: Number(data.get("difficulty") || 1),
      tags: data
        .getAll("tags")
        .map(String)
        .map((value) => value.trim())
        .filter(Boolean),
      defaultPoints: Number(data.get("defaultPoints") || 1),
    };
    try {
      await apiRequest(
        editingQuestion
          ? `/api/v1/questions/${editingQuestion.id}`
          : "/api/v1/questions",
        {
          method: editingQuestion ? "PUT" : "POST",
          body: JSON.stringify(payload),
        },
      );
      setQuestionModal(false);
      setEditingQuestion(null);
      setToast(
        editingQuestion
          ? "Đã cập nhật câu hỏi. Các đề cũ vẫn giữ bản chụp trước đó."
          : "Đã thêm câu hỏi vào ngân hàng.",
      );
      await load();
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể lưu câu hỏi",
      );
    } finally {
      setSaving(false);
    }
  }

  async function archiveQuestion(question: Question) {
    if (
      !window.confirm(
        `Xóa câu hỏi “${question.prompt}” khỏi ngân hàng? Các đề đã tạo vẫn giữ bản chụp câu hỏi này.`,
      )
    )
      return;
    setSaving(true);
    try {
      await apiRequest(`/api/v1/questions/${question.id}`, {
        method: "DELETE",
      });
      setToast("Đã lưu trữ câu hỏi. Các bài kiểm tra cũ không bị thay đổi.");
      await load();
    } catch (caught) {
      setToast(
        caught instanceof Error ? caught.message : "Không thể lưu trữ câu hỏi",
      );
    } finally {
      setSaving(false);
    }
  }

  async function createExam(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    const courseId = standaloneOnly ? "" : String(data.get("courseId") ?? "").trim();
    const lessonId = standaloneOnly ? "" : String(data.get("lessonId") ?? "").trim();
    try {
      const created = await apiRequest<Exam>("/api/v1/exams", {
        method: "POST",
        body: JSON.stringify({
          title: String(data.get("title") ?? ""),
          courseId: courseId || null,
          lessonId: lessonId || null,
          contextType: courseId ? "COURSE_QUIZ" : "STANDALONE_EXAM",
          autoGrade: data.get("autoGrade") === "on",
          durationMinutes: Number(data.get("durationMinutes") || 30),
          opensAt: null,
          closesAt: null,
          maxAttempts: Number(data.get("maxAttempts") || 1),
          waitMinutesBetweenAttempts: Number(
            data.get("waitMinutesBetweenAttempts") || 0,
          ),
          passingScore: Number(data.get("passingScore") || 70),
          shuffleQuestions: data.get("shuffleQuestions") === "on",
          shuffleAnswers: data.get("shuffleAnswers") === "on",
          scoreStrategy: String(data.get("scoreStrategy") ?? "HIGHEST"),
          status: String(data.get("status") ?? "DRAFT"),
          questions: selectedQuestions.map((id, index) => ({
            questionId: id,
            points:
              questions.find((item) => item.id === id)?.defaultPoints ?? 1,
            sortOrder: index + 1,
          })),
        }),
      });
      setExamModal(false);
      setSelectedQuestions([]);
      setToast("Đã tạo kỳ thi độc lập.");
      await load();
      window.setTimeout(() => window.location.assign(standaloneExamPath(role, created.id)), 250);
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể tạo bài kiểm tra",
      );
    } finally {
      setSaving(false);
    }
  }


  async function createExamFromDocuments(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    try {
      const providerConfigId = String(data.get("providerConfigId") ?? "");
      if (!providerConfigId) throw new Error("Chưa có cấu hình AI đang bật.");
      const sourceFiles = data.getAll("documents").filter((item): item is File => item instanceof File && item.size > 0);
      if (!sourceFiles.length) throw new Error("Hãy chọn ít nhất một tài liệu PDF hoặc DOCX.");
      const allowedTypes = new Set(["application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"]);
      const isSupportedDocument = (file: File) => allowedTypes.has(file.type) || file.name.toLocaleLowerCase("vi-VN").endsWith(".pdf") || file.name.toLocaleLowerCase("vi-VN").endsWith(".docx");
      if (sourceFiles.some((file) => !isSupportedDocument(file))) throw new Error("Chỉ hỗ trợ tài liệu PDF và DOCX.");
      if (Object.values(difficultyDistribution).reduce((sum, value) => sum + value, 0) !== 100) throw new Error("Tổng tỷ lệ độ khó phải bằng 100%.");
      const pending: PendingGeneratedExam = {
        title: String(data.get("title") ?? "").trim(),
        durationMinutes: Number(data.get("durationMinutes") || 45),
        maxAttempts: Number(data.get("maxAttempts") || 1),
        passingScore: Number(data.get("passingScore") || 70),
        status: String(data.get("status") ?? "DRAFT"),
      };
      if (!pending.title) throw new Error("Hãy nhập tên bài thi.");
      const documentFileIds: string[] = [];
      for (const file of sourceFiles) {
        const upload = new FormData();
        upload.set("file", file);
        const stored = await apiRequest<{ id: string }>("/api/v1/files?purpose=QUESTION_SOURCE", { method: "POST", body: upload });
        documentFileIds.push(stored.id);
      }
      const createdJob = await apiRequest<GenerationJob>("/api/v1/ai/question-generation-jobs", {
        method: "POST",
        body: JSON.stringify({
          courseId: STANDALONE_QUESTION_WORKSPACE_ID,
          providerConfigId,
          documentFileIds,
          language: "vi",
          numberOfQuestions: aiQuestionCount,
          questionTypes: ["SINGLE_CHOICE", "TRUE_FALSE"],
          difficultyDistribution,
        }),
      });
      const job = ["REVIEW_REQUIRED", "APPROVED", "FAILED"].includes(createdJob.status) ? createdJob : await waitForStandaloneGeneration(createdJob.id);
      if (job.status === "FAILED") throw new Error(job.errorMessage || "AI không tạo được bộ câu hỏi hợp lệ.");
      const questions = job.questionSet?.questions ?? [];
      if (!questions.length) throw new Error("AI không trả về câu hỏi để xem trước.");
      setPendingGeneratedExam(pending);
      setReviewJob(job);
      setReviewQuestions(questions);
      setReviewSelected(questions.map((question) => question.externalId));
      setDocumentExamModal(false);
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Không thể tạo bài thi từ tài liệu");
    } finally {
      setSaving(false);
    }
  }

  async function approveGeneratedExamQuestions() {
    if (!reviewJob || !pendingGeneratedExam) return;
    if (!reviewSelected.length) {
      setFormError("Hãy chọn ít nhất một câu hỏi để nhập.");
      return;
    }
    setSaving(true);
    setFormError("");
    try {
      if (reviewJob.status === "REVIEW_REQUIRED") {
        await apiRequest(`/api/v1/ai/question-generation-jobs/${reviewJob.id}/review`, {
          method: "POST",
          body: JSON.stringify({ decision: "APPROVE", comments: "Giảng viên đã xem trước câu hỏi kỳ thi độc lập", selectedExternalIds: reviewSelected, questionSet: { ...reviewJob.questionSet, questions: reviewQuestions } }),
        });
      }
      const imported = await apiRequest<{ importedQuestionIds: string[] }>(`/api/v1/ai/question-generation-jobs/${reviewJob.id}/import`, { method: "POST" });
      if (!imported.importedQuestionIds.length) throw new Error("Không nhận được câu hỏi đã nhập từ tài liệu.");
      const createdExam = await apiRequest<Exam>("/api/v1/exams", {
        method: "POST",
        body: JSON.stringify({
          title: pendingGeneratedExam.title,
          courseId: null,
          lessonId: null,
          contextType: "STANDALONE_EXAM",
          autoGrade: true,
          durationMinutes: pendingGeneratedExam.durationMinutes,
          opensAt: null,
          closesAt: null,
          maxAttempts: pendingGeneratedExam.maxAttempts,
          waitMinutesBetweenAttempts: 0,
          passingScore: pendingGeneratedExam.passingScore,
          shuffleQuestions: true,
          shuffleAnswers: true,
          scoreStrategy: "HIGHEST",
          status: pendingGeneratedExam.status,
          questions: imported.importedQuestionIds.map((questionId, index) => ({ questionId, points: 1, sortOrder: index + 1 })),
        }),
      });
      setReviewJob(null);
      setReviewSelected([]);
      setReviewQuestions([]);
      setPendingGeneratedExam(null);
      setToast(`Đã duyệt ${imported.importedQuestionIds.length} câu và tạo kỳ thi độc lập.`);
      await load();
      window.setTimeout(() => window.location.assign(standaloneExamPath(role, createdExam.id)), 200);
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Không thể duyệt và nhập câu hỏi");
    } finally {
      setSaving(false);
    }
  }



  return (
    <>
      <header className="exam-page-heading">
        <div>
          <nav aria-label="Đường dẫn"><span>Trang chủ</span><i>•</i><strong>Bài thi</strong></nav>
          <h1>{standaloneOnly ? "Bài thi" : "Đánh giá"}</h1>
          <p>{canManage
            ? "Quản lý, tạo và theo dõi các kỳ thi độc lập trong hệ thống."
            : "Theo dõi và tham gia các kỳ thi đã được giao cho bạn."}</p>
        </div>
        {canManage && (
          <div className="exam-heading-actions">
            <button className="button secondary" onClick={() => { setFormError(""); setDocumentExamModal(true); }}>
              <Icon name="file" />Tạo từ PDF/DOCX
            </button>
            <button className="button primary" onClick={() => { setFormError(""); setExamModal(true); }}>
              <Icon name="plus" />Tạo bài thi
            </button>
          </div>
        )}
      </header>

      <section className="exam-stat-grid" aria-label="Tổng quan bài thi">
        {[
          ["Tổng bài thi", examStats.total, "Tất cả kỳ thi", "exam", "violet"],
          ["Đang mở", examStats.active, "Đang diễn ra", "check", "green"],
          ["Bản nháp", examStats.draft, "Chưa xuất bản", "file", "amber"],
          ["Đã hoàn thành", examStats.completed, "Đã kết thúc", "report", "blue"],
        ].map(([label, value, hint, icon, tone]) => (
          <article className={`exam-stat-card ${tone}`} key={String(label)}>
            <span><Icon name={icon as any} size={22} /></span>
            <div><small>{label}</small><strong>{value}</strong><p>{hint}</p></div>
          </article>
        ))}
      </section>

      <div className="exam-overview-layout">
        <section className="exam-main-column">
          <div className="exam-filter-panel">
            <label className="exam-search-field">
              <Icon name="search" size={18} />
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên hoặc mã bài thi…" />
            </label>
            <div className="exam-filter-row">
              <label><span>Trạng thái</span><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="ALL">Tất cả</option><option value="ACTIVE">Đang mở</option><option value="DRAFT">Bản nháp</option><option value="INACTIVE">Đã đóng</option><option value="ARCHIVED">Lưu trữ</option></select></label>
              <label><span>Sắp xếp</span><select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}><option value="NEWEST">Mới nhất</option><option value="OLDEST">Cũ nhất</option><option value="TITLE">Theo tên</option></select></label>
              <button className="button secondary compact" onClick={() => void load()}><Icon name="refresh" />Làm mới</button>
            </div>
          </div>

          {loading ? (
            <LoadingState />
          ) : error ? (
            <ErrorState message={error} onRetry={() => void load()} />
          ) : filtered.length === 0 ? (
            <EmptyState title="Chưa có bài thi phù hợp" description={canManage ? "Tạo kỳ thi mới hoặc thay đổi bộ lọc tìm kiếm." : "Bài thi sẽ xuất hiện khi giảng viên giao cho bạn."} />
          ) : (
            <div className="exam-list">
              {filtered.map((exam) => (
                <Link className="exam-list-item" href={standaloneExamPath(role, exam.id)} key={exam.id}>
                  <span className={`exam-list-icon status-${exam.status.toLowerCase()}`}><Icon name="exam" size={22} /></span>
                  <div className="exam-list-primary">
                    <div className="exam-title-line"><h2>{exam.title}</h2><span className="exam-code">{exam.id.slice(0, 8).toUpperCase()}</span><StatusBadge value={exam.status} /></div>
                    <p>{exam.courseId ? "Bài kiểm tra khóa học" : "Kỳ thi độc lập"}<i>•</i>Phiên bản {exam.version}</p>
                    <div className="exam-date-line"><Icon name="clock" size={15} /><span>Mở: {formatDate(exam.opensAt)}</span><i>•</i><span>Đóng: {formatDate(exam.closesAt)}</span></div>
                  </div>
                  <dl className="exam-list-metrics">
                    <div><dt>Thời lượng</dt><dd>{formatDuration(exam.durationMinutes)}</dd></div>
                    <div><dt>Số câu</dt><dd>{exam.questions.length}</dd></div>
                    <div><dt>Điểm đạt</dt><dd>{exam.passingScore}%</dd></div>
                    <div><dt>Lượt làm</dt><dd>{exam.maxAttempts}</dd></div>
                  </dl>
                  <span className="exam-row-action" aria-hidden="true"><Icon name="arrow" size={18} /></span>
                </Link>
              ))}
            </div>
          )}
        </section>

        <aside className="exam-side-column">
          <section className="exam-side-card">
            <header><h2>Tổng quan nhanh</h2></header>
            <dl className="exam-quick-stats">
              <div><dt>Kỳ thi đang mở</dt><dd>{examStats.active}</dd></div>
              <div><dt>Tổng câu hỏi</dt><dd>{exams.reduce((sum, item) => sum + item.questions.length, 0)}</dd></div>
              <div><dt>Cần xuất bản</dt><dd>{examStats.draft}</dd></div>
              <div><dt>Tỷ lệ hoạt động</dt><dd>{examStats.total ? Math.round((examStats.active / examStats.total) * 100) : 0}%</dd></div>
            </dl>
          </section>
          <section className="exam-side-card">
            <header><h2>Bài thi gần đây</h2></header>
            <div className="upcoming-exams">
              {exams.slice(0, 3).map((item, index) => (
                <Link href={standaloneExamPath(role, item.id)} key={item.id}>
                  <span><b>{String(index + 1).padStart(2, "0")}</b><small>KỲ THI</small></span>
                  <div><strong>{item.title}</strong><small>{formatDuration(item.durationMinutes)} · {item.questions.length} câu</small></div>
                </Link>
              ))}
              {!exams.length && <p className="muted">Chưa có kỳ thi.</p>}
            </div>
          </section>
          {canManage && (
            <section className="exam-side-card">
              <header><h2>Thao tác nhanh</h2></header>
              <div className="exam-quick-actions">
                <button onClick={() => { setFormError(""); setExamModal(true); }}><Icon name="plus" />Tạo bài thi mới</button>
                <button onClick={() => { setFormError(""); setDocumentExamModal(true); }}><Icon name="file" />Sinh đề từ tài liệu</button>
                <button onClick={() => openQuestion()}><Icon name="question" />Thêm câu hỏi</button>
              </div>
            </section>
          )}
        </aside>
      </div>

      {canManage && !loading && !error && (
        <section className="section-card" style={{ marginTop: 22 }}>
          <div className="section-title">
            <div>
              <h2>Ngân hàng câu hỏi</h2>
              <p>
                {questions.length} câu đang sử dụng · sửa câu hỏi không làm thay
                đổi đề đã chụp trước đó
              </p>
            </div>
            <button
              className="button secondary compact"
              onClick={() => openQuestion()}
            >
              <Icon name="plus" />
              Thêm câu
            </button>
          </div>
          {questions.length === 0 ? (
            <EmptyState
              title="Ngân hàng đang trống"
              description="Tạo câu hỏi đầu tiên để xây dựng bài kiểm tra."
            />
          ) : (
            <div className="question-list">
              {questions.map((question, index) => (
                <article className="question-preview" key={question.id}>
                  <span className="question-number">{index + 1}</span>
                  <div>
                    <div className="question-heading">
                      <strong>{question.prompt}</strong>
                      <span>{question.defaultPoints} điểm</span>
                    </div>
                    <p>
                      {question.type.replaceAll("_", " ")} · phiên bản{" "}
                      {question.version} · độ khó {question.difficulty}
                    </p>
                    {question.options.length > 0 && (
                      <ol>
                        {question.options.map((option) => (
                          <li key={option}>{option}</li>
                        ))}
                      </ol>
                    )}
                    <div
                      className="page-actions"
                      style={{ justifyContent: "flex-start", marginTop: 10 }}
                    >
                      <button
                        className="button secondary compact"
                        onClick={() => openQuestion(question)}
                      >
                        <Icon name="edit" />
                        Sửa
                      </button>
                      <button
                        className="button danger compact"
                        disabled={saving}
                        onClick={() => void archiveQuestion(question)}
                      >
                        <Icon name="trash" />
                        Xóa
                      </button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      )}

      <Modal
        open={documentExamModal}
        onClose={() => !saving && setDocumentExamModal(false)}
        title="Tạo bài thi từ PDF/DOCX"
        description="Tài liệu được tải lên riêng cho giảng viên, AI tạo bản nháp theo độ khó đã chọn. Bạn xem trước từng câu rồi mới duyệt và dựng kỳ thi."
      >
        <form className="form-stack" onSubmit={createExamFromDocuments}>
          <label>Tên bài thi <b>*</b><input name="title" required /></label>
          <label>Tài liệu nguồn <b>*</b><input name="documents" type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" multiple required /><small>Có thể chọn nhiều PDF/DOCX. Nội dung cần ở dạng văn bản có thể trích xuất.</small></label>
          <label>Cấu hình AI<select name="providerConfigId" required defaultValue={providers[0]?.id ?? ""}><option value="">Chọn cấu hình AI</option>{providers.map((provider) => <option key={provider.id} value={provider.id}>{provider.code} · {provider.model}</option>)}</select></label>
          <DifficultyDistributionSelector value={difficultyDistribution} onChange={setDifficultyDistribution} numberOfQuestions={aiQuestionCount} />
          <div className="form-grid four">
            <label>Số câu<NumberStepper name="numberOfQuestions" defaultValue={20} value={aiQuestionCount} onChange={setAiQuestionCount} min={1} max={100} ariaLabel="Số câu hỏi" /></label>
            <label>Thời lượng<NumberStepper name="durationMinutes" defaultValue={45} min={1} max={480} step={5} ariaLabel="Thời lượng" /></label>
            <label>Điểm đạt<NumberStepper name="passingScore" defaultValue={70} min={0} max={100} step={5} ariaLabel="Điểm đạt" /></label>
            <label>Trạng thái<select name="status" defaultValue="DRAFT"><option value="DRAFT">Bản nháp</option><option value="ACTIVE">Hoạt động</option></select></label>
          </div>
          <label>Số lần làm<NumberStepper name="maxAttempts" defaultValue={1} min={1} max={20} ariaLabel="Số lần làm" /></label>
          {providers.length === 0 && <div className="form-alert info"><Icon name="warning" />Quản trị viên cần cấu hình và bật dịch vụ AI trước.</div>}
          {formError && <div className="form-alert error"><Icon name="warning" />{formError}</div>}
          <div className="modal-actions"><button type="button" className="button secondary" onClick={() => setDocumentExamModal(false)}>Hủy</button><button className="button primary" disabled={saving || providers.length === 0}>{saving ? "AI đang đọc tài liệu..." : "Sinh câu hỏi để xem trước"}</button></div>
        </form>
      </Modal>

      <Modal
        open={Boolean(reviewJob)}
        onClose={() => {
          if (!saving) {
            setReviewJob(null);
            setReviewSelected([]);
            setReviewQuestions([]);
            setPendingGeneratedExam(null);
            setFormError("");
          }
        }}
        title="Duyệt câu hỏi cho bài thi"
        description="Kiểm tra câu hỏi, đáp án, lời giải, độ khó và trích dẫn trước khi nhập vào kỳ thi."
        wide
      >
        {reviewJob && (
          <>
            <GeneratedQuestionReview
              job={reviewJob}
              questions={reviewQuestions}
              selectedIds={reviewSelected}
              onToggle={(id) => setReviewSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])}
              onSelectAll={(selected) => setReviewSelected(selected ? reviewQuestions.map((question) => question.externalId) : [])}
              onQuestionChange={(updated) => setReviewQuestions((current) => current.map((question) => question.externalId === updated.externalId ? updated : question))}
            />
            {formError && <div className="form-alert error"><Icon name="warning" />{formError}</div>}
            <div className="modal-actions">
              <button type="button" className="button secondary" onClick={() => { setReviewJob(null); setReviewSelected([]); setReviewQuestions([]); setPendingGeneratedExam(null); }}>Hủy bộ câu hỏi</button>
              <button type="button" className="button primary" disabled={saving || !reviewSelected.length} onClick={() => void approveGeneratedExamQuestions()}>{saving ? "Đang nhập câu hỏi..." : `Duyệt và nhập ${reviewSelected.length} câu`}</button>
            </div>
          </>
        )}
      </Modal>

      <Modal
        open={questionModal}
        onClose={() => {
          if (!saving) {
            setQuestionModal(false);
            setEditingQuestion(null);
          }
        }}
        title={editingQuestion ? "Chỉnh sửa câu hỏi" : "Thêm câu hỏi"}
        description="Câu hỏi được lưu thật trong ngân hàng. Đề đã tạo dùng bản chụp riêng để giữ lịch sử."
      >
        <QuestionForm
          question={editingQuestion}
          onSubmit={saveQuestion}
          busy={saving}
          error={formError}
        />
      </Modal>

      <Modal
        open={examModal}
        onClose={() => !saving && setExamModal(false)}
        title="Tạo bài thi độc lập"
        description="Kỳ thi được quản lý riêng và có thể giao cho học viên hoặc đơn vị."
      >
        <form className="form-stack" onSubmit={createExam}>
          <label>
            Tên bài thi <b>*</b>
            <input name="title" required />
            <small>Bài thi độc lập không thuộc khóa học. Bài kiểm tra khóa học được tạo trong trang chi tiết khóa học.</small>
          </label>
          <div className="form-grid four">
            <label>
              Thời lượng (phút)
              <NumberStepper
                name="durationMinutes"
                defaultValue={30}
                min={1}
                max={480}
                step={5}
                ariaLabel="Thời lượng bài thi"
              />
            </label>
            <label>
              Số lần làm
              <NumberStepper
                name="maxAttempts"
                defaultValue={1}
                min={1}
                max={20}
                ariaLabel="Số lần làm"
              />
            </label>
            <label>
              Điểm đạt (%)
              <NumberStepper
                name="passingScore"
                defaultValue={70}
                min={0}
                max={100}
                step={5}
                ariaLabel="Điểm đạt"
              />
            </label>
            <label>
              Trạng thái
              <select name="status" defaultValue="ACTIVE">
                <option value="ACTIVE">Hoạt động</option>
                <option value="DRAFT">Bản nháp</option>
                <option value="INACTIVE">Tạm đóng</option>
              </select>
            </label>
          </div>
          <div className="form-grid two">
            <label>
              Chờ giữa các lần làm (phút)
              <input
                name="waitMinutesBetweenAttempts"
                type="number"
                min="0"
                defaultValue="0"
              />
            </label>
            <label>
              Cách lấy điểm
              <select name="scoreStrategy" defaultValue="HIGHEST">
                <option value="HIGHEST">Cao nhất</option>
                <option value="LATEST">Lần gần nhất</option>
                <option value="AVERAGE">Trung bình</option>
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
                <strong>Trộn thứ tự câu hỏi</strong>
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
                <h3>Chọn câu hỏi</h3>
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
                Chưa có câu hỏi. Hãy tạo câu hỏi trước.
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
              onClick={() => setExamModal(false)}
            >
              Hủy
            </button>
            <button
              className="button primary"
              disabled={
                saving || selectedQuestions.length === 0
              }
            >
              {saving ? "Đang tạo..." : "Tạo bài thi"}
            </button>
          </div>
        </form>
      </Modal>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </>
  );
}

function QuestionForm({
  question,
  onSubmit,
  busy,
  error,
}: {
  question: Question | null;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  error: string;
}) {
  return (
    <form className="form-stack" onSubmit={onSubmit}>
      <div className="form-grid two">
        <label>
          Loại câu hỏi
          <select name="type" defaultValue={question?.type ?? "SINGLE_CHOICE"}>
            <option value="SINGLE_CHOICE">Một đáp án</option>
            <option value="MULTIPLE_CHOICE">Nhiều đáp án</option>
            <option value="TRUE_FALSE">Đúng / Sai</option>
            <option value="SHORT_TEXT">Trả lời ngắn</option>
            <option value="ESSAY">Tự luận</option>
          </select>
        </label>
        <label>
          Điểm mặc định
          <NumberStepper
            name="defaultPoints"
            defaultValue={question?.defaultPoints ?? 1}
            min={0.1}
            max={100}
            step={0.5}
            ariaLabel="Điểm mặc định"
          />
        </label>
      </div>
      <label>
        Nội dung câu hỏi <b>*</b>
        <textarea
          name="prompt"
          required
          rows={4}
          defaultValue={question?.prompt ?? ""}
        />
      </label>
      <label>
        Các phương án trả lời
        <small>
          Mỗi ô là một phương án. Dùng nút + hoặc − để thêm và xóa.
        </small>
        <RepeatableField
          name="options"
          initialValues={question?.options.length ? question.options : ["", ""]}
          addLabel="Thêm phương án"
          placeholder="Nhập phương án trả lời"
          minItems={2}
        />
      </label>
      <label>
        Đáp án đúng
        <small>Mỗi ô là một đáp án đúng; nội dung phải khớp phương án.</small>
        <RepeatableField
          name="correctAnswers"
          initialValues={question?.correctAnswers.length ? question.correctAnswers : [""]}
          addLabel="Thêm đáp án đúng"
          placeholder="Nhập đáp án đúng"
        />
      </label>
      <div className="form-grid two">
        <label>
          Độ khó
          <select
            name="difficulty"
            defaultValue={String(question?.difficulty ?? 1)}
          >
            <option value="1">Dễ</option>
            <option value="2">Trung bình</option>
            <option value="3">Khá</option>
            <option value="4">Khó</option>
            <option value="5">Rất khó</option>
          </select>
        </label>
        <label>
          Thẻ phân loại
          <RepeatableField
            name="tags"
            initialValues={question?.tags.length ? question.tags : [""]}
            addLabel="Thêm thẻ"
            placeholder="VD: an toàn thông tin"
          />
        </label>
      </div>
      <label>
        Giải thích đáp án
        <input name="explanation" defaultValue={question?.explanation ?? ""} />
      </label>
      {error && (
        <div className="form-alert error">
          <Icon name="warning" />
          {error}
        </div>
      )}
      <div className="modal-actions">
        <button className="button primary" disabled={busy}>
          {busy ? "Đang lưu..." : question ? "Lưu thay đổi" : "Lưu câu hỏi"}
        </button>
      </div>
    </form>
  );
}
