"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, createIdempotencyKey } from "@/lib/api";
import type { Course, CourseProgress, Lesson, StoredFile } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { ProgressBar } from "./ProgressBar";
import { LessonResource } from "./LessonResource";

const typeLabel: Record<string, string> = { TEXT: "Bài đọc", PDF: "PDF", VIDEO: "Video", AUDIO: "Âm thanh", FILE: "Tài liệu", ASSIGNMENT: "Bài thực hành", EXAM: "Bài kiểm tra" };

export function LearningPlayer({ enrollmentId, user }: { enrollmentId: string; user: PortalUser }) {
  const [course, setCourse] = useState<Course | null>(null);
  const [progress, setProgress] = useState<CourseProgress | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [outlineOpen, setOutlineOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [assignmentFile, setAssignmentFile] = useState<File | null>(null);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const openedAt = useRef(Date.now());

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const detail = await apiRequest<CourseProgress>(`/api/v1/learning/${enrollmentId}`);
      const value = await apiRequest<Course>(`/api/v1/courses/${detail.courseId}`);
      const sorted = { ...value, lessons: [...(value.lessons ?? [])].sort((a, b) => a.sortOrder - b.sortOrder) };
      setProgress(detail); setCourse(sorted);
      setSelectedId((current) => current && sorted.lessons?.some((item) => item.id === current) ? current : detail.lastLessonId ?? sorted.lessons?.[0]?.id ?? null);
      openedAt.current = Date.now();
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể mở khóa học"); }
    finally { setLoading(false); }
  }, [enrollmentId]);
  useEffect(() => { void load(); }, [load]);

  const selected = useMemo(() => course?.lessons?.find((lesson) => lesson.id === selectedId) ?? null, [course, selectedId]);
  const completedIds = useMemo(() => new Set(progress?.lessons?.filter((item) => item.completed).map((item) => item.lessonId) ?? []), [progress]);
  const index = course?.lessons?.findIndex((item) => item.id === selectedId) ?? -1;
  const previous = index > 0 ? course?.lessons?.[index - 1] : null;
  const next = index >= 0 && index < (course?.lessons?.length ?? 0) - 1 ? course?.lessons?.[index + 1] : null;

  async function updateLesson(lesson: Lesson, completed: boolean, moveNext = false, position?: string) {
    if (!course || !progress) return;
    setUpdating(true);
    try {
      const elapsed = Math.max(1, Math.min(3600, Math.round((Date.now() - openedAt.current) / 1000)));
      const updated = await apiRequest<CourseProgress>("/api/v1/learning/progress", {
        method: "PUT",
        headers: { "Idempotency-Key": createIdempotencyKey() },
        body: JSON.stringify({ enrollmentId, courseId: course.id, lessonId: lesson.id, completed, learningSecondsDelta: elapsed, position: position ?? (completed ? "completed" : "opened") }),
      });
      setProgress(updated); openedAt.current = Date.now();
      setToast(completed ? "Đã hoàn thành bài học và lưu tiến độ." : "Đã cập nhật tiến độ bài học.");
      if (moveNext && next) { setSelectedId(next.id); setOutlineOpen(false); }
    } catch (caught) { setToast(caught instanceof Error ? caught.message : "Không thể lưu tiến độ"); }
    finally { setUpdating(false); }
  }

  async function submitAssignment(lesson: Lesson) {
    if (!assignmentFile) { setToast("Vui lòng chọn tệp bài làm trước khi nộp."); return; }
    setUpdating(true);
    try {
      const form = new FormData(); form.append("file", assignmentFile);
      const stored = await apiRequest<StoredFile>(`/api/v1/files?purpose=ASSIGNMENT_SUBMISSION`, { method: "POST", body: form });
      await updateLesson(lesson, true, false, `submission:${stored.id}:${encodeURIComponent(stored.originalName)}`);
      setAssignmentFile(null);
      setToast("Đã tải bài làm lên hệ thống và ghi nhận hoàn thành.");
    } catch (caught) { setToast(caught instanceof Error ? caught.message : "Không thể nộp bài thực hành"); }
    finally { setUpdating(false); }
  }

  if (loading) return <LoadingState label="Đang mở nội dung và tiến độ học tập..."/>;
  if (error || !course || !progress) return <ErrorState message={error || "Không tìm thấy khóa học"} onRetry={() => void load()}/>;

  return <div className="learning-player">
    <header className="player-header">
      <div className="player-title"><Link className="icon-button" href="/learning" aria-label="Quay lại"><Icon name="back"/></Link><div><small>{course.code}</small><strong>{course.name}</strong></div></div>
      <div className="player-progress"><ProgressBar value={progress.progressPercent}/><span>{progress.progressPercent}% hoàn thành</span></div>
      <button className="button secondary compact player-outline-toggle" onClick={() => setOutlineOpen((value) => !value)}><Icon name="list"/>Mục lục</button>
    </header>
    <div className="player-layout">
      {outlineOpen && <button className="player-overlay" onClick={() => setOutlineOpen(false)} aria-label="Đóng mục lục"/>}
      <aside className={`player-outline ${outlineOpen ? "open" : ""}`}>
        <div className="player-outline-header"><div><strong>Nội dung khóa học</strong><small>{completedIds.size}/{course.lessons?.length ?? 0} bài đã hoàn thành</small></div><button className="icon-button outline-close" onClick={() => setOutlineOpen(false)}><Icon name="close"/></button></div>
        <div className="player-lesson-list">{course.lessons?.map((lesson, lessonIndex) => <button key={lesson.id} className={`player-lesson ${lesson.id === selectedId ? "active" : ""}`} onClick={() => { setSelectedId(lesson.id); setOutlineOpen(false); openedAt.current = Date.now(); }}>
          <span className={`completion-dot ${completedIds.has(lesson.id) ? "done" : ""}`}>{completedIds.has(lesson.id) ? <Icon name="check" size={14}/> : lessonIndex + 1}</span><span><strong>{lesson.title}</strong><small>{typeLabel[lesson.type] ?? lesson.type} · {lesson.estimatedMinutes || 0} phút</small></span>
        </button>)}</div>
        <div className="outline-progress"><ProgressBar value={progress.progressPercent} label="Tiến độ khóa học"/></div>
      </aside>
      <main className="player-content">
        {selected ? <article className="learning-lesson">
          <header><span className="content-type">{typeLabel[selected.type] ?? selected.type}</span><h1>{selected.title}</h1><p>{selected.required ? "Nội dung bắt buộc" : "Nội dung tự chọn"} · {selected.estimatedMinutes || 0} phút</p></header>
          <section className="lesson-body">
            {selected.type === "TEXT" ? <div className="learning-rich-text"><p>{selected.textContent || "Bài học chưa có nội dung."}</p></div> : selected.fileId ? <LessonResource fileId={selected.fileId} type={selected.type}/> : selected.type === "EXAM" ? <div className="learning-callout"><Icon name="exam" size={36}/><div><h2>Bài kiểm tra của khóa học</h2><p>Mở chức năng Bài kiểm tra để bắt đầu phiên thi và lưu đáp án.</p><Link className="button primary" href="/exams">Mở bài kiểm tra</Link></div></div> : selected.type === "ASSIGNMENT" ? <AssignmentSubmission lesson={selected} position={progress.lessons?.find((item) => item.lessonId === selected.id)?.position} file={assignmentFile} busy={updating} onFile={setAssignmentFile} onSubmit={() => void submitAssignment(selected)}/> : <EmptyState title="Nội dung chưa sẵn sàng" description="Giảng viên cần bổ sung tài nguyên cho bài học này."/>}
          </section>
          <footer className="lesson-navigation">
            <button className="button secondary" disabled={!previous} onClick={() => previous && setSelectedId(previous.id)}><Icon name="back"/>Bài trước</button>
            <div>{completedIds.has(selected.id) ? <button className="button secondary" disabled={updating} onClick={() => void updateLesson(selected, false)}><Icon name="refresh"/>Đánh dấu học lại</button> : <button className="button primary" disabled={updating} onClick={() => void updateLesson(selected, true, true)}><Icon name="check"/>{updating ? "Đang lưu..." : next ? "Hoàn thành & tiếp tục" : "Hoàn thành khóa học"}</button>}</div>
          </footer>
        </article> : <EmptyState title="Khóa học chưa có bài học" description="Vui lòng liên hệ giảng viên phụ trách."/>}
      </main>
    </div>
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </div>;
}
function AssignmentSubmission({ lesson, position, file, busy, onFile, onSubmit }: { lesson: Lesson; position?: string | null; file: File | null; busy: boolean; onFile: (file: File | null) => void; onSubmit: () => void }) {
  const parts = position?.startsWith("submission:") ? position.split(":") : [];
  const fileId = parts[1];
  const fileName = parts.length > 2 ? decodeURIComponent(parts.slice(2).join(":")) : "Bài làm đã nộp";
  return <div className="assignment-box"><div className="assignment-head"><span><Icon name="upload" size={30}/></span><div><h2>{lesson.title}</h2><p>Tải bài làm lên kho tệp nội bộ. Tệp được gắn với tiến độ của bài học này.</p></div></div>{fileId && <div className="assignment-submitted"><Icon name="check"/><div><strong>Đã nộp bài</strong><span>{fileName}</span></div><a className="button secondary compact" href={`/api/gateway/api/v1/files/${fileId}/content`}><Icon name="download"/>Tải lại</a></div>}<label className="assignment-drop"><input type="file" onChange={(event) => onFile(event.target.files?.[0] ?? null)}/><Icon name="file" size={28}/><span><strong>{file ? file.name : fileId ? "Chọn tệp khác để nộp lại" : "Chọn tệp bài làm"}</strong><small>Tệp sẽ được kiểm tra định dạng và dung lượng bởi File Storage Service.</small></span></label><button className="button primary" disabled={!file || busy} onClick={onSubmit}>{busy ? "Đang tải và lưu..." : fileId ? "Nộp lại bài" : "Nộp bài thực hành"}</button></div>;
}
