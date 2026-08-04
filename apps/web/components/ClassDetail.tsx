"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, createIdempotencyKey, unwrapItems } from "@/lib/api";
import type { Course, Enrollment, PageResponse, TrainingClass, UserAccount } from "@/lib/models";
import { formatDate } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Modal } from "./Modal";
import { StatusBadge } from "./StatusBadge";

export function ClassDetail({ classId, user }: { classId: string; user: PortalUser }) {
  const [trainingClass, setTrainingClass] = useState<TrainingClass | null>(null);
  const [course, setCourse] = useState<Course | null>(null);
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState("");
  const canEnroll = user.permissions.includes("enrollments:write");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const item = await apiRequest<TrainingClass>(`/api/v1/classes/${classId}`);
      const [courseData, enrollmentData] = await Promise.all([apiRequest<Course>(`/api/v1/courses/${item.courseId}`), apiRequest<unknown>(`/api/v1/classes/${classId}/enrollments`)]);
      setTrainingClass(item); setCourse(courseData); setEnrollments(unwrapItems<Enrollment>(enrollmentData as any));
      if (user.permissions.includes("users:read")) {
        const userData = await apiRequest<unknown>("/api/v1/users?size=1000");
        setUsers(
          unwrapItems<UserAccount>(userData as any).filter(
            (account) =>
              account.status === "ACTIVE" &&
              (account.permissions ?? []).includes("courses:learn"),
          ),
        );
      }
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể tải lớp đào tạo"); }
    finally { setLoading(false); }
  }, [classId, user.permissions]);
  useEffect(() => { void load(); }, [load]);

  const userList = useMemo(() => Array.isArray(users) ? users : unwrapItems<UserAccount>(users as any), [users]);
  const enrollmentList = useMemo(() => Array.isArray(enrollments) ? enrollments : unwrapItems<Enrollment>(enrollments as any), [enrollments]);
  const userMap = useMemo(() => new Map(userList.map((item) => [item.id, item])), [userList]);
  const enrolledIds = useMemo(() => new Set(enrollmentList.map((item) => item.userId)), [enrollmentList]);
  const available = userList.filter((item) => !enrolledIds.has(item.id) && `${item.fullName} ${item.username} ${item.code}`.toLowerCase().includes(query.trim().toLowerCase()));

  async function enroll() {
    if (!selected.length) return;
    setSaving(true);
    try {
      const result = await apiRequest<{ created: Enrollment[]; existing: Enrollment[]; errors: Record<string, string> }>(`/api/v1/classes/${classId}/enrollments`, { method: "POST", headers: { "Idempotency-Key": createIdempotencyKey() }, body: JSON.stringify({ userIds: selected, dueAt: trainingClass?.dueAt ?? null }) });
      const failures = Object.keys(result.errors ?? {}).length;
      setToast(failures ? `Đã ghi danh ${result.created.length} học viên, ${failures} bản ghi lỗi.` : `Đã ghi danh ${result.created.length} học viên thành công.`);
      setOpen(false); setSelected([]); await load();
    } catch (caught) { setToast(caught instanceof Error ? caught.message : "Không thể ghi danh"); }
    finally { setSaving(false); }
  }

  if (loading) return <LoadingState label="Đang tải thông tin lớp và danh sách ghi danh..."/>;
  if (error || !trainingClass) return <ErrorState message={error || "Không tìm thấy lớp"} onRetry={() => void load()}/>;
  return <>
    <PageHeader backHref="/classes" eyebrow={`${trainingClass.code} · PHIÊN BẢN KHÓA HỌC ${trainingClass.courseVersion}`} title={trainingClass.name} description={course?.name ?? "Lớp đào tạo"} actions={<><StatusBadge value={trainingClass.status}/>{canEnroll && <button className="button primary" onClick={() => setOpen(true)}><Icon name="plus"/>Ghi danh học viên</button>}</>}/>
    <section className="detail-grid">
      <article className="info-panel"><h2>Thông tin lớp</h2><dl><div><dt>Khóa học</dt><dd>{course?.name ?? trainingClass.courseId}</dd></div><div><dt>Ngày bắt đầu</dt><dd>{formatDate(trainingClass.startsAt)}</dd></div><div><dt>Ngày kết thúc</dt><dd>{formatDate(trainingClass.endsAt)}</dd></div><div><dt>Hạn hoàn thành</dt><dd>{formatDate(trainingClass.dueAt)}</dd></div></dl></article>
      <article className="metric-panel"><span>Học viên đã ghi danh</span><strong>{enrollments.length}</strong><small>Dữ liệu trực tiếp từ Enrollment Service</small></article>
    </section>
    <section className="section-card">
      <div className="section-title"><div><h2>Danh sách học viên</h2><p>Theo dõi các tài khoản đã được đưa vào lớp.</p></div><button className="button secondary compact" onClick={() => void load()}><Icon name="refresh"/>Làm mới</button></div>
      {enrollments.length === 0 ? <EmptyState title="Chưa có học viên" description="Ghi danh học viên để khóa học xuất hiện trong không gian học tập của họ." action={canEnroll ? <button className="button primary" onClick={() => setOpen(true)}><Icon name="plus"/>Ghi danh</button> : undefined}/> : <div className="responsive-table"><table><thead><tr><th>Học viên</th><th>Tài khoản</th><th>Ngày ghi danh</th><th>Hạn hoàn thành</th><th>Trạng thái</th></tr></thead><tbody>{enrollments.map((item) => { const account = userMap.get(item.userId); return <tr key={item.id}><td><div className="person-cell"><span className="avatar small">{(account?.fullName ?? "HV").slice(0,2).toUpperCase()}</span><strong>{account?.fullName ?? item.userId}</strong></div></td><td>{account?.username ?? "—"}</td><td>{formatDate(item.enrolledAt)}</td><td>{formatDate(item.dueAt)}</td><td><StatusBadge value={item.status}/></td></tr>; })}</tbody></table></div>}
    </section>
    <Modal open={open} onClose={() => !saving && setOpen(false)} title="Ghi danh học viên" description="Chọn một hoặc nhiều học viên. Yêu cầu được xử lý idempotent để không tạo bản ghi trùng." wide>
      <div className="selection-toolbar"><label className="search-field"><Icon name="search"/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên, tài khoản hoặc mã"/></label><span>{selected.length} đã chọn</span></div>
      <div className="selection-list">{available.length ? available.map((item) => <label className="select-person" key={item.id}><input type="checkbox" checked={selected.includes(item.id)} onChange={(event) => setSelected((current) => event.target.checked ? [...current, item.id] : current.filter((id) => id !== item.id))}/><span className="avatar small">{item.fullName.slice(0,2).toUpperCase()}</span><span><strong>{item.fullName}</strong><small>{item.username} · {item.code}</small></span></label>) : <p className="selection-empty">Không còn học viên phù hợp để ghi danh.</p>}</div>
      <div className="modal-actions"><button className="button secondary" onClick={() => setOpen(false)}>Hủy</button><button className="button primary" onClick={() => void enroll()} disabled={saving || selected.length === 0}>{saving ? "Đang ghi danh..." : `Ghi danh ${selected.length} học viên`}</button></div>
    </Modal>
    {toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}
