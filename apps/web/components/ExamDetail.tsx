"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, createIdempotencyKey } from "@/lib/api";
import type { Course, Exam, ExamQuestion, ExamSession, Grade } from "@/lib/models";
import { formatDate, formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { ProgressBar } from "./ProgressBar";
import { StatusBadge } from "./StatusBadge";

function secondsLeft(expiresAt: string): number {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 1000));
}
function timerLabel(value: number): string {
  const hours = Math.floor(value / 3600);
  const minutes = Math.floor((value % 3600) / 60);
  const seconds = value % 60;
  return [hours, minutes, seconds].filter((_, index) => index > 0 || hours > 0).map((part) => String(part).padStart(2, "0")).join(":");
}
function answerCount(answers?: Record<string, unknown> | null): number {
  if (!answers) return 0;
  return Object.values(answers).filter((answer) => Array.isArray(answer) ? answer.length > 0 : String(answer ?? "").trim().length > 0).length;
}

export function ExamDetail({ examId, user }: { examId: string; user: PortalUser }) {
  const canManage = user.permissions.includes("assessment:manage");
  const [exam, setExam] = useState<Exam | null>(null);
  const [course, setCourse] = useState<Course | null>(null);
  const [session, setSession] = useState<ExamSession | null>(null);
  const [answers, setAnswers] = useState<Record<string, unknown>>({});
  const [current, setCurrent] = useState(0);
  const [remaining, setRemaining] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [result, setResult] = useState<Grade | null>(null);
  const lastSaved = useRef("");
  const autoSubmitted = useRef(false);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const data = await apiRequest<Exam>(`/api/v1/exams/${examId}`);
      setExam(data);
      if (data.courseId) {
        try { setCourse(await apiRequest<Course>(`/api/v1/courses/${data.courseId}`)); } catch { setCourse(null); }
      }
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể tải bài kiểm tra"); }
    finally { setLoading(false); }
  }, [examId]);
  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!session || session.status !== "IN_PROGRESS") return;
    setRemaining(secondsLeft(session.expiresAt));
    const timer = window.setInterval(() => setRemaining(secondsLeft(session.expiresAt)), 1000);
    return () => window.clearInterval(timer);
  }, [session]);

  const questions = session?.questions ?? exam?.questions ?? [];
  const answered = answerCount(answers);
  const question = questions[current];
  const progress = questions.length ? Math.round(answered * 100 / questions.length) : 0;

  async function startExam() {
    setBusy(true); setError("");
    try {
      const started = await apiRequest<ExamSession>("/api/v1/exams/start", { method: "POST", body: JSON.stringify({ examId }) });
      setSession(started); setAnswers(started.answers ?? {}); setCurrent(0); lastSaved.current = JSON.stringify(started.answers ?? {});
      setToast(started.attemptNo > 1 ? `Đã bắt đầu lần thi thứ ${started.attemptNo}.` : "Đã bắt đầu bài kiểm tra.");
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể bắt đầu bài kiểm tra"); }
    finally { setBusy(false); }
  }

  function setAnswer(questionId: string, value: unknown) {
    setAnswers((previous) => ({ ...previous, [questionId]: value }));
  }

  async function save(showMessage = true) {
    if (!session || session.status !== "IN_PROGRESS") return;
    const serialized = JSON.stringify(answers);
    if (serialized === lastSaved.current) { if (showMessage) setToast("Đáp án đã được lưu."); return; }
    setBusy(true);
    try {
      const updated = await apiRequest<ExamSession>(`/api/v1/exam-sessions/${session.id}/answers`, { method: "PUT", body: JSON.stringify({ answers }) });
      setSession(updated); lastSaved.current = serialized;
      if (showMessage) setToast("Đã lưu đáp án lên hệ thống.");
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể lưu đáp án"); }
    finally { setBusy(false); }
  }

  async function submitExam(automatic = false) {
    if (!session) return;
    const unanswered = questions.length - answered;
    if (!automatic && unanswered > 0 && !window.confirm(`Bạn còn ${unanswered} câu chưa trả lời. Vẫn nộp bài?`)) return;
    if (!automatic && !window.confirm("Sau khi nộp, bạn không thể sửa đáp án. Xác nhận nộp bài?")) return;
    setBusy(true); setError("");
    try {
      if (JSON.stringify(answers) !== lastSaved.current) {
        await apiRequest(`/api/v1/exam-sessions/${session.id}/answers`, { method: "PUT", body: JSON.stringify({ answers }) });
      }
      const submitted = await apiRequest<ExamSession>(`/api/v1/exam-sessions/${session.id}/submit`, { method: "POST", headers: { "Idempotency-Key": createIdempotencyKey() } });
      setSession(submitted);
      setToast("Đã nộp bài thành công. Hệ thống đang chấm điểm.");
      window.setTimeout(async () => {
        try {
          const grades = await apiRequest<Grade[]>("/api/v1/grades/me");
          setResult(grades.find((grade) => grade.sessionId === submitted.id) ?? null);
        } catch { /* grading may still be processing */ }
      }, 1200);
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể nộp bài"); }
    finally { setBusy(false); }
  }

  useEffect(() => {
    if (!session || session.status !== "IN_PROGRESS") return;
    const timer = window.setInterval(() => { if (!busy && JSON.stringify(answers) !== lastSaved.current) void save(false); }, 20000);
    return () => window.clearInterval(timer);
  }, [session, answers, busy]);

  useEffect(() => {
    if (!session || session.status !== "IN_PROGRESS" || remaining > 0 || autoSubmitted.current) return;
    autoSubmitted.current = true;
    void submitExam(true);
  }, [remaining, session]);

  if (loading) return <LoadingState label="Đang tải cấu hình bài kiểm tra..."/>;
  if (error && !exam) return <ErrorState message={error} onRetry={() => void load()}/>;
  if (!exam) return <EmptyState title="Không tìm thấy bài kiểm tra" description="Bài kiểm tra có thể đã bị đóng hoặc không thuộc phạm vi của bạn."/>;

  if (canManage) return <>
    <PageHeader backHref="/exams" eyebrow="CẤU HÌNH BÀI KIỂM TRA" title={exam.title} description={course?.name ?? "Bài kiểm tra chưa gắn thông tin khóa học"} actions={<StatusBadge value={exam.status}/>}/>
    <section className="detail-grid">
      <article className="section-card">
        <div className="section-title"><div><h2>Nội dung đề thi</h2><p>Phiên bản {exam.version} · {(exam.questions ?? []).length} câu hỏi</p></div></div>
        <div className="question-list">{(exam.questions ?? []).map((item, index) => <article className="question-preview" key={item.id}><span className="question-number">{index + 1}</span><div><div className="question-heading"><strong>{item.prompt}</strong><span>{item.points} điểm</span></div><p>{(item.type ?? "").replaceAll("_", " ")} · {(item.options ?? []).length ? `${(item.options ?? []).length} phương án` : "Câu trả lời tự do"}</p>{(item.options ?? []).length > 0 && <ol>{(item.options ?? []).map((option) => <li key={option}>{option}</li>)}</ol>}</div></article>)}</div>
      </article>
      <aside className="settings-panel">
        <h2>Thiết lập</h2>
        <dl className="summary-list"><div><dt>Trạng thái</dt><dd><StatusBadge value={exam.status}/></dd></div><div><dt>Thời lượng</dt><dd>{formatDuration(exam.durationMinutes)}</dd></div><div><dt>Số lần làm</dt><dd>{exam.maxAttempts}</dd></div><div><dt>Điểm đạt</dt><dd>{exam.passingScore}%</dd></div><div><dt>Thời gian mở</dt><dd>{formatDate(exam.opensAt, true)}</dd></div><div><dt>Thời gian đóng</dt><dd>{formatDate(exam.closesAt, true)}</dd></div></dl>
        {course && <Link className="button secondary full" href={`/courses/${course.id}`}><Icon name="book"/>Mở khóa học</Link>}
      </aside>
    </section>
  </>;

  if (session?.status === "SUBMITTED") return <>
    <PageHeader backHref="/exams" eyebrow="ĐÃ NỘP BÀI" title={exam.title} description={`Lần thi ${session.attemptNo} đã được ghi nhận lúc ${formatDate(session.submittedAt, true)}.`}/>
    <section className="result-panel"><span className="result-icon"><Icon name="check" size={34}/></span><h2>Nộp bài thành công</h2>{result ? <><strong className="result-score">{Math.round(result.percentage)}%</strong><StatusBadge value={result.status}/><p>{result.status === "PENDING_MANUAL" ? "Bài có câu tự luận và đang chờ giảng viên chấm." : result.passed ? "Bạn đã đạt yêu cầu bài kiểm tra." : "Kết quả chưa đạt mức yêu cầu."}</p></> : <p>Hệ thống đang xử lý kết quả. Bạn có thể quay lại trang bài kiểm tra để xem sau.</p>}<Link className="button primary" href="/exams">Về danh sách bài kiểm tra</Link></section>
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;

  if (!session) return <>
    <PageHeader backHref="/exams" eyebrow="BÀI KIỂM TRA ĐƯỢC GIAO" title={exam.title} description={course?.name ?? "Khóa học của tôi"}/>
    <section className="exam-intro"><div className="exam-intro-main"><span className="exam-hero-icon"><Icon name="exam" size={36}/></span><div><StatusBadge value={exam.status}/><h2>Sẵn sàng bắt đầu?</h2><p>Khi bắt đầu, đồng hồ sẽ chạy liên tục. Đáp án được lưu trên máy chủ khi bạn bấm lưu hoặc nộp bài.</p></div></div><div className="exam-rules"><div><Icon name="clock"/><span><strong>{formatDuration(exam.durationMinutes)}</strong>Thời gian làm bài</span></div><div><Icon name="question"/><span><strong>{(exam.questions ?? []).length} câu</strong>Tổng số câu hỏi</span></div><div><Icon name="target"/><span><strong>{exam.passingScore}%</strong>Điểm đạt</span></div><div><Icon name="refresh"/><span><strong>{exam.maxAttempts} lần</strong>Số lượt tối đa</span></div></div>{error && <div className="form-alert error"><Icon name="warning"/>{error}</div>}<button className="button primary large-action" disabled={busy || exam.status !== "ACTIVE"} onClick={() => void startExam()}>{busy ? "Đang tạo phiên thi..." : "Bắt đầu làm bài"}<Icon name="arrow"/></button></section>
  </>;

  return <div className="exam-taking">
    <header className="exam-taking-header"><div><Link href="/exams" className="icon-button"><Icon name="close"/></Link><div><small>BÀI KIỂM TRA ĐANG DIỄN RA</small><h1>{exam.title}</h1></div></div><div className={`exam-timer ${remaining < 300 ? "urgent" : ""}`}><Icon name="clock"/><span><small>Thời gian còn lại</small><strong>{timerLabel(remaining)}</strong></span></div></header>
    <div className="exam-progress-row"><ProgressBar value={progress}/><span>{answered}/{(questions ?? []).length} câu đã trả lời</span></div>
    <div className="exam-taking-layout">
      <aside className="exam-navigator"><h2>Danh sách câu hỏi</h2><div>{(questions ?? []).map((item, index) => <button key={item.id} className={`${index === current ? "active" : ""} ${answers[item.id] !== undefined && String(answers[item.id]).length ? "answered" : ""}`} onClick={() => setCurrent(index)}>{index + 1}</button>)}</div><div className="exam-legend"><span><i className="answered"/>Đã trả lời</span><span><i/>Chưa trả lời</span></div></aside>
      <main className="exam-question-panel">{question && <QuestionEditor question={question} value={answers[question.id]} onChange={(value) => setAnswer(question.id, value)} index={current}/>}<footer className="exam-actions"><button className="button secondary" disabled={current === 0} onClick={() => { void save(false); setCurrent((value) => Math.max(0, value - 1)); }}><Icon name="back"/>Câu trước</button><div><button className="button secondary" disabled={busy} onClick={() => void save()}><Icon name="save"/>Lưu bài</button>{current < (questions ?? []).length - 1 ? <button className="button primary" onClick={() => { void save(false); setCurrent((value) => Math.min((questions ?? []).length - 1, value + 1)); }}>Câu tiếp theo<Icon name="arrow"/></button> : <button className="button primary" disabled={busy} onClick={() => void submitExam(false)}>Nộp bài<Icon name="check"/></button>}</div></footer></main>
    </div>
    {error && <div className="floating-error"><Icon name="warning"/>{error}<button onClick={() => setError("")}><Icon name="close"/></button></div>}
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </div>;
}

function QuestionEditor({ question, value, onChange, index }: { question: ExamQuestion; value: unknown; onChange: (value: unknown) => void; index: number }) {
  const selected = Array.isArray(value) ? value.map(String) : [];
  const options = question.type === "TRUE_FALSE" && question.options.length === 0 ? ["Đúng", "Sai"] : question.options;
  return <article className="question-editor"><div className="question-editor-head"><span>Câu {index + 1}</span><strong>{question.points} điểm</strong></div><h2>{question.prompt}</h2>
    {question.type === "MULTIPLE_CHOICE" ? <div className="answer-options">{options.map((option) => <label key={option}><input type="checkbox" checked={selected.includes(option)} onChange={(event) => onChange(event.target.checked ? [...selected, option] : selected.filter((item) => item !== option))}/><span>{option}</span></label>)}</div>
    : question.type === "SINGLE_CHOICE" || question.type === "TRUE_FALSE" ? <div className="answer-options">{options.map((option) => <label key={option}><input type="radio" name={question.id} checked={String(value ?? "") === option} onChange={() => onChange(option)}/><span>{option}</span></label>)}</div>
    : <label className="essay-answer">Câu trả lời<textarea rows={question.type === "ESSAY" ? 10 : 4} value={String(value ?? "")} onChange={(event) => onChange(event.target.value)} placeholder="Nhập câu trả lời của bạn..."/></label>}
  </article>;
}
