"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Course, Exam, Question } from "@/lib/models";
import { formatDate, formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Modal } from "./Modal";
import { StatusBadge } from "./StatusBadge";

export function ExamsPage({ user }: { user: PortalUser }) {
  const canManage = user.permissions.includes("assessment:manage");
  const [exams, setExams] = useState<Exam[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [examModal, setExamModal] = useState(false);
  const [questionModal, setQuestionModal] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState<Question | null>(null);
  const [selectedQuestions, setSelectedQuestions] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const examData = await apiRequest<unknown>("/api/v1/exams");
      setExams(unwrapItems<Exam>(examData as never));
      const courseData = await apiRequest<unknown>("/api/v1/courses?size=100");
      setCourses(unwrapItems<Course>(courseData as never));
      if (canManage) {
        const questionData = await apiRequest<unknown>("/api/v1/questions");
        setQuestions(unwrapItems<Question>(questionData as never));
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể tải bài kiểm tra");
    } finally {
      setLoading(false);
    }
  }, [canManage]);

  useEffect(() => { void load(); }, [load]);

  const courseMap = useMemo(() => new Map(courses.map((course) => [course.id, course])), [courses]);
  const normalizedQuery = query.trim().toLowerCase();
  const filtered = exams.filter((exam) => `${exam.title} ${courseMap.get(exam.courseId ?? "")?.name ?? ""}`.toLowerCase().includes(normalizedQuery));

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
    const rawOptions = String(data.get("options") ?? "").split("\n").map((value) => value.trim()).filter(Boolean);
    const options = type === "TRUE_FALSE" ? ["Đúng", "Sai"] : rawOptions;
    const correctAnswers = String(data.get("correctAnswers") ?? "").split("\n").map((value) => value.trim()).filter(Boolean);
    const payload = {
      type,
      prompt: String(data.get("prompt") ?? ""),
      options,
      correctAnswers,
      explanation: String(data.get("explanation") ?? "") || null,
      difficulty: Number(data.get("difficulty") || 1),
      tags: String(data.get("tags") ?? "").split(",").map((value) => value.trim()).filter(Boolean),
      defaultPoints: Number(data.get("defaultPoints") || 1),
    };
    try {
      await apiRequest(editingQuestion ? `/api/v1/questions/${editingQuestion.id}` : "/api/v1/questions", {
        method: editingQuestion ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      setQuestionModal(false);
      setEditingQuestion(null);
      setToast(editingQuestion ? "Đã cập nhật câu hỏi. Các đề cũ vẫn giữ bản chụp trước đó." : "Đã thêm câu hỏi vào ngân hàng.");
      await load();
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Không thể lưu câu hỏi");
    } finally {
      setSaving(false);
    }
  }

  async function archiveQuestion(question: Question) {
    if (!window.confirm(`Xóa câu hỏi “${question.prompt}” khỏi ngân hàng? Các đề đã tạo vẫn giữ bản chụp câu hỏi này.`)) return;
    setSaving(true);
    try {
      await apiRequest(`/api/v1/questions/${question.id}`, { method: "DELETE" });
      setToast("Đã lưu trữ câu hỏi. Các bài kiểm tra cũ không bị thay đổi.");
      await load();
    } catch (caught) {
      setToast(caught instanceof Error ? caught.message : "Không thể lưu trữ câu hỏi");
    } finally {
      setSaving(false);
    }
  }

  async function createExam(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    try {
      const created = await apiRequest<Exam>("/api/v1/exams", {
        method: "POST",
        body: JSON.stringify({
          title: String(data.get("title") ?? ""),
          courseId: String(data.get("courseId") ?? ""),
          lessonId: null,
          durationMinutes: Number(data.get("durationMinutes") || 30),
          opensAt: null,
          closesAt: null,
          maxAttempts: Number(data.get("maxAttempts") || 1),
          waitMinutesBetweenAttempts: Number(data.get("waitMinutesBetweenAttempts") || 0),
          passingScore: Number(data.get("passingScore") || 70),
          shuffleQuestions: data.get("shuffleQuestions") === "on",
          shuffleAnswers: data.get("shuffleAnswers") === "on",
          scoreStrategy: String(data.get("scoreStrategy") ?? "HIGHEST"),
          status: String(data.get("status") ?? "DRAFT"),
          questions: selectedQuestions.map((id, index) => ({
            questionId: id,
            points: questions.find((item) => item.id === id)?.defaultPoints ?? 1,
            sortOrder: index + 1,
          })),
        }),
      });
      setExamModal(false);
      setSelectedQuestions([]);
      setToast("Đã tạo bài kiểm tra và lưu cấu hình thật.");
      await load();
      window.setTimeout(() => window.location.assign(`/exams/${created.id}`), 250);
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Không thể tạo bài kiểm tra");
    } finally {
      setSaving(false);
    }
  }

  return <>
    <PageHeader
      eyebrow={canManage ? "ĐÁNH GIÁ & KIỂM TRA" : "BÀI KIỂM TRA CỦA TÔI"}
      title="Bài kiểm tra"
      description={canManage ? "Tạo, sửa, lưu trữ câu hỏi và bài kiểm tra. Mỗi đề giữ bản chụp nội dung để bảo toàn lịch sử." : "Làm bài, tự lưu đáp án và nhận kết quả trong các khóa học được giao."}
      icon="exam"
      actions={canManage ? <><button className="button secondary" onClick={() => openQuestion()}><Icon name="plus"/>Thêm câu hỏi</button><button className="button primary" onClick={() => { setFormError(""); setExamModal(true); }}><Icon name="plus"/>Tạo bài kiểm tra</button></> : undefined}
    />

    <section className="toolbar-card"><label className="search-field"><Icon name="search"/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm bài kiểm tra"/></label><button className="button secondary" onClick={() => void load()}><Icon name="refresh"/>Làm mới</button></section>

    {loading ? <LoadingState/> : error ? <ErrorState message={error} onRetry={() => void load()}/> : filtered.length === 0 ? <EmptyState title="Chưa có bài kiểm tra" description={canManage ? "Tạo câu hỏi trong ngân hàng, sau đó tạo bài kiểm tra và gắn với khóa học." : "Bài kiểm tra sẽ xuất hiện khi khóa học được giao có bài thi đang hoạt động."}/> : <section className="exam-grid">{filtered.map((exam) => <Link className="exam-card" href={`/exams/${exam.id}`} key={exam.id}><div className="exam-card-icon"><Icon name="exam" size={27}/></div><div className="exam-card-body"><div><StatusBadge value={exam.status}/><span>v{exam.version}</span></div><h2>{exam.title}</h2><p>{courseMap.get(exam.courseId ?? "")?.name ?? "Khóa học"}</p><dl><div><dt>Thời lượng</dt><dd>{formatDuration(exam.durationMinutes)}</dd></div><div><dt>Số câu hỏi</dt><dd>{exam.questions.length}</dd></div><div><dt>Điểm đạt</dt><dd>{exam.passingScore}%</dd></div><div><dt>Mở đến</dt><dd>{formatDate(exam.closesAt)}</dd></div></dl><span className="exam-open">{canManage ? "Xem và chỉnh sửa" : "Mở bài kiểm tra"}<Icon name="arrow"/></span></div></Link>)}</section>}

    {canManage && !loading && !error && <section className="section-card" style={{ marginTop: 22 }}>
      <div className="section-title"><div><h2>Ngân hàng câu hỏi</h2><p>{questions.length} câu đang sử dụng · sửa câu hỏi không làm thay đổi đề đã chụp trước đó</p></div><button className="button secondary compact" onClick={() => openQuestion()}><Icon name="plus"/>Thêm câu</button></div>
      {questions.length === 0 ? <EmptyState title="Ngân hàng đang trống" description="Tạo câu hỏi đầu tiên để xây dựng bài kiểm tra."/> : <div className="question-list">{questions.map((question, index) => <article className="question-preview" key={question.id}><span className="question-number">{index + 1}</span><div><div className="question-heading"><strong>{question.prompt}</strong><span>{question.defaultPoints} điểm</span></div><p>{question.type.replaceAll("_", " ")} · phiên bản {question.version} · độ khó {question.difficulty}</p>{question.options.length > 0 && <ol>{question.options.map((option) => <li key={option}>{option}</li>)}</ol>}<div className="page-actions" style={{ justifyContent: "flex-start", marginTop: 10 }}><button className="button secondary compact" onClick={() => openQuestion(question)}><Icon name="edit"/>Sửa</button><button className="button danger compact" disabled={saving} onClick={() => void archiveQuestion(question)}><Icon name="trash"/>Xóa</button></div></div></article>)}</div>}
    </section>}

    <Modal open={questionModal} onClose={() => { if (!saving) { setQuestionModal(false); setEditingQuestion(null); } }} title={editingQuestion ? "Chỉnh sửa câu hỏi" : "Thêm câu hỏi"} description="Câu hỏi được lưu thật trong ngân hàng. Đề đã tạo dùng bản chụp riêng để giữ lịch sử.">
      <QuestionForm question={editingQuestion} onSubmit={saveQuestion} busy={saving} error={formError}/>
    </Modal>

    <Modal open={examModal} onClose={() => !saving && setExamModal(false)} title="Tạo bài kiểm tra" description="Chọn khóa học, quy tắc làm bài và câu hỏi từ ngân hàng.">
      <form className="form-stack" onSubmit={createExam}>
        <div className="form-grid two"><label>Tên bài kiểm tra <b>*</b><input name="title" required/></label><label>Khóa học <b>*</b><select name="courseId" required defaultValue=""><option value="" disabled>Chọn khóa học</option>{courses.map((course) => <option key={course.id} value={course.id}>{course.name} ({course.code})</option>)}</select></label></div>
        <div className="form-grid four"><label>Thời lượng (phút)<input name="durationMinutes" type="number" min="1" max="480" defaultValue="30"/></label><label>Số lần làm<input name="maxAttempts" type="number" min="1" max="20" defaultValue="1"/></label><label>Điểm đạt (%)<input name="passingScore" type="number" min="0" max="100" defaultValue="70"/></label><label>Trạng thái<select name="status" defaultValue="ACTIVE"><option value="ACTIVE">Hoạt động</option><option value="DRAFT">Bản nháp</option><option value="INACTIVE">Tạm đóng</option></select></label></div>
        <div className="form-grid two"><label>Chờ giữa các lần làm (phút)<input name="waitMinutesBetweenAttempts" type="number" min="0" defaultValue="0"/></label><label>Cách lấy điểm<select name="scoreStrategy" defaultValue="HIGHEST"><option value="HIGHEST">Cao nhất</option><option value="LATEST">Lần gần nhất</option><option value="AVERAGE">Trung bình</option></select></label></div>
        <div className="check-group"><label className="check-row"><input type="checkbox" name="shuffleQuestions"/><span><strong>Trộn thứ tự câu hỏi</strong></span></label><label className="check-row"><input type="checkbox" name="shuffleAnswers"/><span><strong>Trộn phương án</strong></span></label></div>
        <div className="question-picker"><div className="section-title"><div><h3>Chọn câu hỏi</h3><p>{selectedQuestions.length} câu đã chọn</p></div></div>{questions.length ? questions.map((question) => <label className="question-option" key={question.id}><input type="checkbox" checked={selectedQuestions.includes(question.id)} onChange={(event) => setSelectedQuestions((current) => event.target.checked ? [...current, question.id] : current.filter((id) => id !== question.id))}/><span><strong>{question.prompt}</strong><small>{question.type} · {question.defaultPoints} điểm</small></span></label>) : <p className="selection-empty">Chưa có câu hỏi. Hãy tạo câu hỏi trước.</p>}</div>
        {formError && <div className="form-alert error"><Icon name="warning"/>{formError}</div>}
        <div className="modal-actions"><button type="button" className="button secondary" onClick={() => setExamModal(false)}>Hủy</button><button className="button primary" disabled={saving || selectedQuestions.length === 0}>{saving ? "Đang tạo..." : "Tạo bài kiểm tra"}</button></div>
      </form>
    </Modal>
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}

function QuestionForm({ question, onSubmit, busy, error }: { question: Question | null; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; busy: boolean; error: string }) {
  return <form className="form-stack" onSubmit={onSubmit}>
    <div className="form-grid two"><label>Loại câu hỏi<select name="type" defaultValue={question?.type ?? "SINGLE_CHOICE"}><option value="SINGLE_CHOICE">Một đáp án</option><option value="MULTIPLE_CHOICE">Nhiều đáp án</option><option value="TRUE_FALSE">Đúng / Sai</option><option value="SHORT_TEXT">Trả lời ngắn</option><option value="ESSAY">Tự luận</option></select></label><label>Điểm mặc định<input name="defaultPoints" type="number" min="0.1" step="0.1" defaultValue={question?.defaultPoints ?? 1}/></label></div>
    <label>Nội dung câu hỏi <b>*</b><textarea name="prompt" required rows={4} defaultValue={question?.prompt ?? ""}/></label>
    <label>Các phương án trả lời<small>Mỗi phương án một dòng. Câu Đúng/Sai được hệ thống chuẩn hóa tự động.</small><textarea name="options" rows={5} defaultValue={question?.options.join("\n") ?? ""} placeholder={'Phương án A\nPhương án B'}/></label>
    <label>Đáp án đúng<small>Mỗi đáp án một dòng, nhập đúng nội dung phương án.</small><textarea name="correctAnswers" rows={3} defaultValue={question?.correctAnswers.join("\n") ?? ""}/></label>
    <div className="form-grid two"><label>Độ khó<select name="difficulty" defaultValue={String(question?.difficulty ?? 1)}><option value="1">Dễ</option><option value="2">Trung bình</option><option value="3">Khá</option><option value="4">Khó</option><option value="5">Rất khó</option></select></label><label>Thẻ phân loại<input name="tags" defaultValue={question?.tags.join(", ") ?? ""} placeholder="VD: lmspilot, lan"/></label></div>
    <label>Giải thích đáp án<input name="explanation" defaultValue={question?.explanation ?? ""}/></label>
    {error && <div className="form-alert error"><Icon name="warning"/>{error}</div>}
    <div className="modal-actions"><button className="button primary" disabled={busy}>{busy ? "Đang lưu..." : question ? "Lưu thay đổi" : "Lưu câu hỏi"}</button></div>
  </form>;
}
