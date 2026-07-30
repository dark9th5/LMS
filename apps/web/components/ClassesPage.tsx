"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Course, PageResponse, TrainingClass, UserAccount } from "@/lib/models";
import { formatDate } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Modal } from "./Modal";
import { StatusBadge } from "./StatusBadge";

export function ClassesPage({ user }: { user: PortalUser }) {
  const [classes, setClasses] = useState<TrainingClass[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [instructors, setInstructors] = useState<UserAccount[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");
  const canCreate = user.permissions.includes("classes:write");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [classData, courseData] = await Promise.all([
        apiRequest<unknown>("/api/v1/classes"),
        apiRequest<unknown>("/api/v1/courses?size=100"),
      ]);
      setClasses(unwrapItems<TrainingClass>(classData as any));
      setCourses(unwrapItems<Course>(courseData as any));
      if (canCreate && user.permissions.includes("users:read")) {
        const users = await apiRequest<unknown>("/api/v1/users?role=INSTRUCTOR&size=100");
        setInstructors(unwrapItems<UserAccount>(users as any));
      }
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể tải lớp đào tạo"); }
    finally { setLoading(false); }
  }, [canCreate, user.permissions]);
  useEffect(() => { void load(); }, [load]);

  const courseList = useMemo(() => Array.isArray(courses) ? courses : unwrapItems<Course>(courses as any), [courses]);
  const classList = useMemo(() => Array.isArray(classes) ? classes : unwrapItems<TrainingClass>(classes as any), [classes]);
  const courseMap = useMemo(() => new Map(courseList.map((course) => [course.id, course])), [courseList]);
  const filtered = useMemo(() => classList.filter((item) => `${item.name} ${item.code} ${courseMap.get(item.courseId)?.name ?? ""}`.toLowerCase().includes(query.trim().toLowerCase())), [classList, courseMap, query]);

  async function createClass(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setFormError("");
    const data = new FormData(event.currentTarget);
    try {
      const dateValue = (name: string) => data.get(name) ? new Date(`${String(data.get(name))}T00:00:00`).toISOString() : null;
      const created = await apiRequest<TrainingClass>("/api/v1/classes", { method: "POST", body: JSON.stringify({
        code: String(data.get("code") ?? ""), name: String(data.get("name") ?? ""), courseId: String(data.get("courseId") ?? ""),
        startsAt: dateValue("startsAt"), endsAt: dateValue("endsAt"), dueAt: data.get("dueAt") ? new Date(`${String(data.get("dueAt"))}T23:59:59`).toISOString() : null,
        instructorIds: [String(data.get("instructorId") ?? "")],
      }) });
      setOpen(false); setToast("Đã mở lớp đào tạo. Tiếp theo hãy ghi danh học viên."); await load();
      window.setTimeout(() => window.location.assign(`/classes/${created.id}`), 450);
    } catch (caught) { setFormError(caught instanceof Error ? caught.message : "Không thể tạo lớp"); }
    finally { setSaving(false); }
  }

  const publishedCourses = courses.filter((course) => course.status === "PUBLISHED");
  return <>
    <PageHeader eyebrow="TỔ CHỨC ĐÀO TẠO" title="Lớp đào tạo" description="Mở lớp từ khóa học đã xuất bản, phân công giảng viên và ghi danh học viên." icon="class" actions={canCreate ? <button className="button primary" onClick={() => setOpen(true)}><Icon name="plus"/>Mở lớp mới</button> : undefined}/>
    <section className="summary-row"><div className="summary-card"><span>Tổng số lớp</span><strong>{classes.length}</strong><small>Trong phạm vi được phân công</small></div><div className="summary-card"><span>Đang mở</span><strong>{classes.filter((item) => item.status === "OPEN").length}</strong><small>Cho phép ghi danh và học tập</small></div><div className="summary-card"><span>Khóa học sẵn sàng</span><strong>{publishedCourses.length}</strong><small>Đã xuất bản</small></div></section>
    <section className="toolbar-card"><label className="search-field"><Icon name="search"/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm lớp, mã lớp hoặc khóa học"/></label><button className="button secondary" onClick={() => void load()}><Icon name="refresh"/>Làm mới</button></section>
    {loading ? <LoadingState/> : error ? <ErrorState message={error} onRetry={() => void load()}/> : filtered.length === 0 ? <EmptyState title="Chưa có lớp đào tạo" description="Xuất bản ít nhất một khóa học, sau đó mở lớp và ghi danh học viên." action={canCreate ? <button className="button primary" onClick={() => setOpen(true)}><Icon name="plus"/>Mở lớp mới</button> : undefined}/> : <section className="data-list">{filtered.map((item) => {
      const course = courseMap.get(item.courseId);
      return <Link className="data-list-item" href={`/classes/${item.id}`} key={item.id}><span className="list-icon"><Icon name="class"/></span><div className="list-main"><div><strong>{item.name}</strong><StatusBadge value={item.status}/></div><p>{item.code} · {course?.name ?? "Khóa học"}</p><div className="list-meta"><span><Icon name="calendar"/>Bắt đầu: {formatDate(item.startsAt)}</span><span><Icon name="clock"/>Hạn: {formatDate(item.dueAt)}</span></div></div><Icon name="chevron"/></Link>;
    })}</section>}
    <Modal open={open} onClose={() => !saving && setOpen(false)} title="Mở lớp đào tạo" description="Chỉ khóa học đã xuất bản mới có thể dùng để mở lớp.">
      <form className="form-stack" onSubmit={createClass}>
        <div className="form-grid two"><label>Mã lớp <b>*</b><input name="code" required placeholder="CLASS-2026-01"/></label><label>Giảng viên <b>*</b><select name="instructorId" required defaultValue=""><option value="" disabled>Chọn giảng viên</option>{instructors.map((item) => <option key={item.id} value={item.id}>{item.fullName} ({item.username})</option>)}</select></label></div>
        <label>Tên lớp <b>*</b><input name="name" required placeholder="Tên đợt đào tạo"/></label>
        <label>Khóa học đã xuất bản <b>*</b><select name="courseId" required defaultValue=""><option value="" disabled>Chọn khóa học</option>{publishedCourses.map((course) => <option key={course.id} value={course.id}>{course.name} ({course.code})</option>)}</select></label>
        <div className="form-grid three"><label>Ngày bắt đầu<input type="date" name="startsAt"/></label><label>Ngày kết thúc<input type="date" name="endsAt"/></label><label>Hạn hoàn thành<input type="date" name="dueAt"/></label></div>
        {publishedCourses.length === 0 && <div className="form-alert info"><Icon name="warning"/>Chưa có khóa học đã xuất bản.</div>}
        {instructors.length === 0 && <div className="form-alert info"><Icon name="warning"/>Chưa có tài khoản giảng viên khả dụng.</div>}
        {formError && <div className="form-alert error"><Icon name="warning"/>{formError}</div>}
        <div className="modal-actions"><button type="button" className="button secondary" onClick={() => setOpen(false)}>Hủy</button><button className="button primary" disabled={saving || publishedCourses.length === 0 || instructors.length === 0}>{saving ? "Đang tạo..." : "Mở lớp"}</button></div>
      </form>
    </Modal>
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}
