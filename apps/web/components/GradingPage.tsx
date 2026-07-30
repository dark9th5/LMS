"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Exam, Grade, UserAccount } from "@/lib/models";
import { formatDate } from "@/lib/models";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Icon } from "./Icon";
import { Modal } from "./Modal";
import { PageHeader } from "./PageHeader";
import { StatusBadge } from "./StatusBadge";

export function GradingPage() {
  const [grades, setGrades] = useState<Grade[]>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [selected, setSelected] = useState<Grade | null>(null);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [queue, examList] = await Promise.all([apiRequest<unknown>("/api/v1/grades/queue"), apiRequest<unknown>("/api/v1/exams")]);
      setGrades(unwrapItems<Grade>(queue as any));
      setExams(unwrapItems<Exam>(examList as any));
      try {
        const payload = await apiRequest<unknown>("/api/v1/users?size=200");
        setUsers(unwrapItems<UserAccount>(payload as any));
      } catch { setUsers([]); }
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể tải hàng chờ chấm"); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const examListItems = useMemo(() => Array.isArray(exams) ? exams : unwrapItems<Exam>(exams as any), [exams]);
  const userListItems = useMemo(() => Array.isArray(users) ? users : unwrapItems<UserAccount>(users as any), [users]);
  const gradeListItems = useMemo(() => Array.isArray(grades) ? grades : unwrapItems<Grade>(grades as any), [grades]);
  const examMap = useMemo(() => new Map(examListItems.map((item) => [item.id, item])), [examListItems]);
  const userMap = useMemo(() => new Map(userListItems.map((item) => [item.id, item])), [userListItems]);
  const filtered = gradeListItems.filter((grade) => `${examMap.get(grade.examId)?.title ?? ""} ${userMap.get(grade.userId)?.fullName ?? grade.userId}`.toLowerCase().includes(query.trim().toLowerCase()));

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected) return;
    const data = new FormData(event.currentTarget);
    setBusy(true); setFormError("");
    try {
      await apiRequest(`/api/v1/grades/${selected.id}`, { method: "PUT", body: JSON.stringify({ score: Number(data.get("score")), feedback: String(data.get("feedback") ?? "") || null }) });
      setSelected(null); setToast("Đã lưu điểm và hoàn tất chấm bài."); await load();
    } catch (caught) { setFormError(caught instanceof Error ? caught.message : "Không thể lưu điểm"); }
    finally { setBusy(false); }
  }

  return <>
    <PageHeader eyebrow="ĐÁNH GIÁ THỦ CÔNG" title="Hàng chờ chấm điểm" description="Chấm các bài tự luận và câu trả lời ngắn trong đúng phạm vi được phân công." icon="grade"/>
    <section className="toolbar-card"><label className="search-field"><Icon name="search"/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo bài thi hoặc học viên"/></label><span className="result-count">{filtered.length} bài đang chờ</span><button className="button secondary" onClick={() => void load()}><Icon name="refresh"/>Làm mới</button></section>
    {loading ? <LoadingState/> : error ? <ErrorState message={error} onRetry={() => void load()}/> : filtered.length === 0 ? <EmptyState title="Không còn bài chờ chấm" description="Các bài tự luận mới sẽ tự động xuất hiện tại đây sau khi học viên nộp bài."/> : <section className="grading-list">{filtered.map((grade) => {
      const exam = examMap.get(grade.examId); const learner = userMap.get(grade.userId);
      return <article className="grading-card" key={grade.id}><div className="grading-card-main"><span className="list-icon"><Icon name="grade"/></span><div><div className="grading-heading"><h2>{exam?.title ?? "Bài kiểm tra"}</h2><StatusBadge value={grade.status}/></div><p>{learner?.fullName ?? `Học viên ${grade.userId.slice(0, 8)}`} · Nộp bài cần chấm thủ công</p><div className="grading-meta"><span>{grade.details.filter((item) => item.requiresManual).length} câu cần chấm</span><span>Điểm tự động: {grade.score}/{grade.maxScore}</span><span>Cập nhật {formatDate(grade.updatedAt, true)}</span></div></div></div><button className="button primary" onClick={() => { setFormError(""); setSelected(grade); }}><Icon name="edit"/>Chấm bài</button></article>;
    })}</section>}

    <Modal open={Boolean(selected)} onClose={() => !busy && setSelected(null)} title="Chấm bài thủ công" description={selected ? `${examMap.get(selected.examId)?.title ?? "Bài kiểm tra"} · ${userMap.get(selected.userId)?.fullName ?? "Học viên"}` : undefined} wide>
      {selected && <form className="form-stack" onSubmit={submit}><section className="grade-summary"><div><span>Điểm tự động</span><strong>{selected.score}</strong></div><div><span>Điểm tối đa</span><strong>{selected.maxScore}</strong></div><div><span>Câu cần chấm</span><strong>{selected.details.filter((item) => item.requiresManual).length}</strong></div></section><div className="manual-question-list">{selected.details.filter((item) => item.requiresManual).map((item, index) => <div key={item.questionId}><span>Câu tự luận {index + 1}</span><strong>Tối đa {item.maximum} điểm</strong></div>)}</div><label>Tổng điểm sau chấm <b>*</b><input name="score" type="number" min="0" max={selected.maxScore} step="0.01" defaultValue={selected.score} required/></label><label>Nhận xét cho học viên<textarea name="feedback" rows={5} placeholder="Nêu điểm tốt và nội dung cần cải thiện..."/></label>{formError && <div className="form-alert error"><Icon name="warning"/>{formError}</div>}<div className="modal-actions"><button type="button" className="button secondary" onClick={() => setSelected(null)}>Hủy</button><button className="button primary" disabled={busy}>{busy ? "Đang lưu..." : "Hoàn tất chấm điểm"}</button></div></form>}
    </Modal>
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}
