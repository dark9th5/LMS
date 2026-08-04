"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { StatusBadge } from "./StatusBadge";

type Row = Record<string, any>;

function has(user: PortalUser, ...permissions: string[]) {
  return user.accountType === "SYSTEM_ADMIN" || permissions.some((permission) => user.permissions.includes(permission));
}
function when(value?: string | null) {
  if (!value) return "—";
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(parsed);
}
function useData<T>(loader: () => Promise<T>, dependencies: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const refresh = useCallback(async () => {
    setLoading(true); setError("");
    try { setData(await loader()); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể tải dữ liệu"); }
    finally { setLoading(false); }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);
  useEffect(() => { void refresh(); }, [refresh]);
  return { data, setData, loading, error, refresh };
}

export function AdvancedSection({ section, user }: { section: string; user: PortalUser }) {
  if (section === "reports") return <ReportAutomationCenter user={user}/>;
  if (section === "certificates") return <CertificateCenter user={user}/>;
  if (section === "operations") return <OperationsCenter user={user}/>;
  if (section === "notification-automation") return <NotificationAutomationCenter user={user}/>;
  return null;
}

export const advancedSections = new Set(["reports", "certificates", "operations", "notification-automation"]);

function Header({ eyebrow, title, description, icon, action }: { eyebrow: string; title: string; description: string; icon: "report" | "certificate" | "operations" | "bell"; action?: React.ReactNode }) {
  return <div className="page-head advanced-head"><div><span className="page-icon"><Icon name={icon}/></span><div><small>{eyebrow}</small><h1>{title}</h1><p>{description}</p></div></div>{action && <div className="page-head-actions">{action}</div>}</div>;
}
function Panel({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return <section className="section-card advanced-panel"><div className="section-title"><div><h2>{title}</h2>{hint && <p>{hint}</p>}</div></div>{children}</section>;
}

// Reports --------------------------------------------------------------------
type ReportExport = { id: string; scope: string; status: string; rowCount?: number | null; errorMessage?: string | null; createdAt: string; completedAt?: string | null; expiresAt: string };
type ReportSchedule = { id: string; name: string; scope: string; frequency: string; dayOfWeek?: number | null; hourUtc: number; enabled: boolean; nextRunAt: string; updatedAt: string };
type LearningKpi = { scope: string; totalEnrollments: number; notStarted: number; inProgress: number; completed: number; overdue: number; dueSoon: number; passed: number; failed: number; activeLast30Days: number; completionRate: number; passRate: number; averageProgress: number; averageScore?: number | null; generatedAt: string };
type CourseKpi = { courseId: string; totalEnrollments: number; completed: number; overdue: number; completionRate: number; passRate: number; averageProgress: number; averageScore?: number | null; lastActivityAt?: string | null };

function ReportAutomationCenter({ user }: { user: PortalUser }) {
  const canExport = has(user, "reports:export");
  const canSchedule = has(user, "reports:schedule");
  const canScope = has(user, "reports:read:scope");
  const [scope, setScope] = useState(user.accountType === "SYSTEM_ADMIN" ? "SYSTEM" : canScope ? "ASSIGNED" : "SELF");
  const { data, loading, error, refresh } = useData(async () => {
    const [rows, exports, schedules, kpi, courseKpis] = await Promise.all([
      apiRequest<Row[] | { items?: Row[] }>("/api/v1/reports/learning"),
      canExport ? apiRequest<ReportExport[]>("/api/v1/reports/exports") : Promise.resolve([]),
      canSchedule ? apiRequest<ReportSchedule[]>("/api/v1/reports/schedules") : Promise.resolve([]),
      apiRequest<LearningKpi>(`/api/v1/reports/kpis?scope=${scope}`),
      scope !== "SELF" ? apiRequest<CourseKpi[]>(`/api/v1/reports/kpis/courses?scope=${scope}`) : Promise.resolve([]),
    ]);
    return { rows: unwrapItems(rows), exports, schedules, kpi, courseKpis };
  }, [canExport, canSchedule, scope]);
  const [schedule, setSchedule] = useState({ name: "Báo cáo học tập định kỳ", frequency: "DAILY", dayOfWeek: "1", hourUtc: "0", enabled: true });
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState("");
  const rows = data?.rows ?? [];
  const completed = rows.filter((row) => row.completed).length;
  const overdue = rows.filter((row) => !row.completed && row.dueAt && new Date(row.dueAt).getTime() < Date.now()).length;

  async function createExport() {
    setBusy(true);
    try { await apiRequest("/api/v1/reports/exports", { method: "POST", body: JSON.stringify({ scope }) }); setToast("Đã đưa báo cáo vào hàng đợi xử lý nền."); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể tạo báo cáo"); }
    finally { setBusy(false); }
  }
  async function createSchedule(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    try {
      await apiRequest("/api/v1/reports/schedules", { method: "POST", body: JSON.stringify({ name: schedule.name, scope, frequency: schedule.frequency, dayOfWeek: schedule.frequency === "WEEKLY" ? Number(schedule.dayOfWeek) : null, hourUtc: Number(schedule.hourUtc), enabled: schedule.enabled }) });
      setToast("Đã tạo lịch xuất báo cáo."); await refresh();
    } catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể tạo lịch"); }
    finally { setBusy(false); }
  }
  async function removeSchedule(id: string) {
    if (!window.confirm("Xóa lịch báo cáo này?")) return;
    await apiRequest(`/api/v1/reports/schedules/${id}`, { method: "DELETE" });
    setToast("Đã xóa lịch báo cáo."); await refresh();
  }

  if (loading) return <LoadingState label="Đang dựng tinh đồ báo cáo..."/>;
  if (error) return <ErrorState message={error} onRetry={() => void refresh()}/>;
  return <>
    <Header eyebrow="READ MODEL & EXPORT NỀN" title="Báo cáo, KPI và lịch xuất" description="Theo dõi tiến độ theo phạm vi quyền; file lớn được tạo trong hàng đợi và tự hết hạn thay vì khóa giao diện." icon="report" action={<button className="soft-button" onClick={() => void refresh()}><Icon name="refresh"/>Làm mới</button>}/>
    <div className="report-scope-strip"><label>Phạm vi KPI<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SELF">Cá nhân</option>{canScope && <option value="ASSIGNED">Lớp được giao</option>}{user.accountType === "SYSTEM_ADMIN" && <option value="SYSTEM">Toàn hệ thống</option>}</select></label><small>Cập nhật từ read model lúc {when(data?.kpi.generatedAt)}</small></div>
    <div className="stat-grid advanced-stats kpi-stats"><article><strong>{data?.kpi.totalEnrollments ?? rows.length}</strong><span>Lượt học</span></article><article><strong>{data?.kpi.completionRate ?? 0}%</strong><span>Tỷ lệ hoàn thành</span></article><article><strong>{data?.kpi.passRate ?? 0}%</strong><span>Tỷ lệ đạt</span></article><article><strong>{data?.kpi.overdue ?? overdue}</strong><span>Quá hạn</span></article><article><strong>{data?.kpi.dueSoon ?? 0}</strong><span>Sắp đến hạn 7 ngày</span></article><article><strong>{data?.kpi.averageProgress ?? 0}%</strong><span>Tiến độ trung bình</span></article></div>
    <div className="advanced-grid">
      {scope !== "SELF" && <section className="section-card advanced-panel kpi-course-panel"><div className="section-title"><div><h2>Hiệu quả theo khóa học</h2><p>Ưu tiên khóa có quá hạn cao để quản trị can thiệp sớm.</p></div></div><div className="kpi-course-table"><div className="kpi-course-head"><span>Khóa học</span><span>Học viên</span><span>Hoàn thành</span><span>Đạt</span><span>Quá hạn</span></div>{data?.courseKpis.length ? data.courseKpis.slice(0,30).map((item) => <div key={item.courseId}><strong>{item.courseId}</strong><span>{item.totalEnrollments}</span><span>{item.completionRate}%</span><span>{item.passRate}%</span><span className={item.overdue ? "danger-text" : ""}>{item.overdue}</span></div>) : <EmptyState title="Chưa có KPI theo khóa" description="Read model sẽ cập nhật khi có ghi danh và hoạt động học tập."/>}</div></section>}
      <Panel title="Xuất báo cáo theo yêu cầu" hint="Chọn phạm vi và để Reporting Service xử lý ở nền.">
        {canExport ? <div className="advanced-form-row"><label>Phạm vi xuất<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SELF">Cá nhân</option>{canScope && <option value="ASSIGNED">Lớp được giao</option>}{user.accountType === "SYSTEM_ADMIN" && <option value="SYSTEM">Toàn hệ thống</option>}</select></label><button className="button primary" disabled={busy} onClick={() => void createExport()}><Icon name="download"/>{busy ? "Đang tạo..." : "Tạo file CSV"}</button></div> : <p className="muted-copy">Tài khoản chưa có quyền xuất báo cáo.</p>}
        <div className="compact-list">{data?.exports.length ? data.exports.slice(0, 20).map((item) => <article key={item.id}><div><strong>{item.scope} · {item.rowCount ?? 0} dòng</strong><small>Tạo {when(item.createdAt)} · hết hạn {when(item.expiresAt)}</small>{item.errorMessage && <em>{item.errorMessage}</em>}</div><StatusBadge value={item.status}/>{item.status === "COMPLETED" && <a className="button secondary compact" href={`/api/gateway/api/v1/reports/exports/${item.id}/download`}><Icon name="download"/>Tải</a>}</article>) : <EmptyState title="Chưa có file xuất" description="Tạo báo cáo đầu tiên từ phạm vi phù hợp."/>}</div>
      </Panel>
      <Panel title="Lịch báo cáo" hint="Lịch dùng UTC để hoạt động thống nhất trong cụm on-premise.">
        {canSchedule && <form className="form-stack" onSubmit={createSchedule}><label>Tên lịch<input required value={schedule.name} onChange={(event) => setSchedule({ ...schedule, name: event.target.value })}/></label><div className="form-grid three"><label>Tần suất<select value={schedule.frequency} onChange={(event) => setSchedule({ ...schedule, frequency: event.target.value })}><option value="DAILY">Hằng ngày</option><option value="WEEKLY">Hằng tuần</option></select></label>{schedule.frequency === "WEEKLY" && <label>Ngày<select value={schedule.dayOfWeek} onChange={(event) => setSchedule({ ...schedule, dayOfWeek: event.target.value })}>{[1,2,3,4,5,6,7].map((day) => <option key={day} value={day}>Thứ {day === 7 ? "CN" : day + 1}</option>)}</select></label>}<label>Giờ UTC<input type="number" min="0" max="23" value={schedule.hourUtc} onChange={(event) => setSchedule({ ...schedule, hourUtc: event.target.value })}/></label></div><button className="button primary" disabled={busy}><Icon name="plus"/>Tạo lịch</button></form>}
        <div className="compact-list">{data?.schedules.length ? data.schedules.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.frequency} · {item.hourUtc}:00 UTC · lần tới {when(item.nextRunAt)}</small></div><StatusBadge value={item.enabled ? "ACTIVE" : "INACTIVE"}/><button className="icon-button danger" onClick={() => void removeSchedule(item.id)} aria-label="Xóa lịch"><Icon name="trash"/></button></article>) : <EmptyState title="Chưa có lịch" description="Lịch tự động phù hợp với báo cáo định kỳ cho quản trị và giảng viên."/>}</div>
      </Panel>
    </div>{toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}

// Certificates ---------------------------------------------------------------
type Certificate = { id: string; courseId: string; userId: string; verificationCode: string; generation: number; status: string; issuedAt: string; revokedAt?: string | null; revokeReason?: string | null };
type CertificateTemplate = { id: string; name: string; courseId?: string | null; title: string; issuerName: string; bodyText: string; primaryColor: string; secondaryColor: string; logoUrl?: string | null; signatureName?: string | null; active: boolean; updatedAt: string };

function CertificateCenter({ user }: { user: PortalUser }) {
  const canManage = has(user, "certificates:manage");
  const canTemplates = has(user, "certificate-templates:manage");
  const { data, loading, error, refresh } = useData(async () => ({
    certificates: await apiRequest<Certificate[]>(canManage ? "/api/v1/certificates" : "/api/v1/certificates/me"),
    templates: canTemplates ? await apiRequest<CertificateTemplate[]>("/api/v1/certificates/templates") : [],
  }), [canManage, canTemplates]);
  const [draft, setDraft] = useState({ name: "Mẫu chứng chỉ mặc định", courseId: "", title: "CHỨNG CHỈ HOÀN THÀNH", issuerName: "LMSPilot", bodyText: "Xác nhận người học đã hoàn thành chương trình đào tạo.", primaryColor: "#173b65", secondaryColor: "#b99044", logoUrl: "", signatureName: "", active: true });
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState("");

  async function createTemplate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    try { await apiRequest("/api/v1/certificates/templates", { method: "POST", body: JSON.stringify({ ...draft, courseId: draft.courseId || null, logoUrl: draft.logoUrl || null, signatureName: draft.signatureName || null }) }); setToast("Đã lưu mẫu chứng chỉ mới."); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể lưu mẫu"); }
    finally { setBusy(false); }
  }
  async function revoke(item: Certificate) {
    const reason = window.prompt("Lý do thu hồi chứng chỉ:", "Thông tin cấp không còn hợp lệ");
    if (!reason) return;
    await apiRequest(`/api/v1/certificates/${item.id}/revoke`, { method: "PUT", body: JSON.stringify({ reason }) }); setToast("Đã thu hồi chứng chỉ."); await refresh();
  }
  async function reissue(item: Certificate) {
    const reason = window.prompt("Lý do cấp lại:", "Cấp lại theo yêu cầu");
    if (!reason) return;
    await apiRequest(`/api/v1/certificates/${item.id}/reissue`, { method: "POST", body: JSON.stringify({ reason }) }); setToast("Đã cấp lại chứng chỉ với mã xác minh mới."); await refresh();
  }
  async function disableTemplate(id: string) {
    await apiRequest(`/api/v1/certificates/templates/${id}`, { method: "DELETE" }); setToast("Đã ngừng sử dụng mẫu."); await refresh();
  }

  if (loading) return <LoadingState label="Đang mở kho chứng chỉ..."/>;
  if (error) return <ErrorState message={error} onRetry={() => void refresh()}/>;
  return <>
    <Header eyebrow="THÀNH TỰU BẤT BIẾN" title="Chứng chỉ và mẫu phát hành" description="Mỗi chứng chỉ giữ bản chụp mẫu tại thời điểm cấp, có mã xác minh riêng và lịch sử thu hồi/cấp lại." icon="certificate" action={<button className="soft-button" onClick={() => void refresh()}><Icon name="refresh"/>Làm mới</button>}/>
    <div className="advanced-grid">
      <Panel title="Chứng chỉ đã phát hành" hint={`${data?.certificates.length ?? 0} bản ghi trong phạm vi`}>
        <div className="compact-list">{data?.certificates.length ? data.certificates.map((item) => <article key={item.id}><div><strong>{item.verificationCode}</strong><small>Khóa {item.courseId.slice(0, 8)} · lần cấp {item.generation} · {when(item.issuedAt)}</small>{item.revokeReason && <em>{item.revokeReason}</em>}</div><StatusBadge value={item.status}/><a className="button secondary compact" target="_blank" rel="noreferrer" href={`/api/gateway/api/v1/certificates/${item.id}/print`}><Icon name="eye"/>Xem/in</a>{canManage && item.status === "ACTIVE" && <button className="button danger compact" onClick={() => void revoke(item)}>Thu hồi</button>}{canManage && item.status !== "ACTIVE" && <button className="button secondary compact" onClick={() => void reissue(item)}>Cấp lại</button>}</article>) : <EmptyState title="Chưa có chứng chỉ" description="Chứng chỉ sẽ được cấp khi hoàn thành khóa học theo chính sách."/>}</div>
      </Panel>
      {canTemplates && <Panel title="Thiết kế mẫu" hint="Logo phải là URL tài nguyên nội bộ; màu dùng định dạng #RRGGBB."><form className="form-stack" onSubmit={createTemplate}><div className="form-grid two"><label>Tên mẫu<input required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })}/></label><label>Course ID (để trống = mặc định)<input value={draft.courseId} onChange={(e) => setDraft({ ...draft, courseId: e.target.value })}/></label></div><label>Tiêu đề<input required value={draft.title} onChange={(e) => setDraft({ ...draft, title: e.target.value })}/></label><div className="form-grid two"><label>Đơn vị cấp<input required value={draft.issuerName} onChange={(e) => setDraft({ ...draft, issuerName: e.target.value })}/></label><label>Người ký<input value={draft.signatureName} onChange={(e) => setDraft({ ...draft, signatureName: e.target.value })}/></label></div><label>Nội dung<textarea required value={draft.bodyText} onChange={(e) => setDraft({ ...draft, bodyText: e.target.value })}/></label><div className="form-grid three"><label>Màu chính<input type="color" value={draft.primaryColor} onChange={(e) => setDraft({ ...draft, primaryColor: e.target.value })}/></label><label>Màu phụ<input type="color" value={draft.secondaryColor} onChange={(e) => setDraft({ ...draft, secondaryColor: e.target.value })}/></label><label>Logo nội bộ<input placeholder="/api/v1/files/..." value={draft.logoUrl} onChange={(e) => setDraft({ ...draft, logoUrl: e.target.value })}/></label></div><button className="button primary" disabled={busy}><Icon name="save"/>{busy ? "Đang lưu..." : "Lưu mẫu"}</button></form><div className="template-preview" style={{ borderColor: draft.primaryColor, boxShadow: `inset 0 0 0 2px ${draft.secondaryColor}` }}><small>{draft.issuerName}</small><strong style={{ color: draft.primaryColor }}>{draft.title}</strong><p>{draft.bodyText}</p><em>{draft.signatureName}</em></div><div className="compact-list">{data?.templates.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.courseId ? `Khóa ${item.courseId}` : "Mẫu mặc định"} · {when(item.updatedAt)}</small></div><StatusBadge value={item.active ? "ACTIVE" : "INACTIVE"}/>{item.active && <button className="icon-button danger" onClick={() => void disableTemplate(item.id)} aria-label="Ngừng mẫu"><Icon name="trash"/></button>}</article>)}</div></Panel>}
    </div>{toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}

// Notification automation ----------------------------------------------------
type NotificationTemplate = { id: string; code: string; name: string; eventType?: string | null; titleTemplate: string; bodyTemplate: string; inAppEnabled: boolean; emailEnabled: boolean; active: boolean; updatedAt: string };
type ReminderRule = { id: string; name: string; ruleType: string; templateId: string; relativeDays: number; hourUtc: number; enabled: boolean; nextRunAt: string; dispatchedCount: number; updatedAt: string };
type ReminderRun = { ruleId: string; matched: number; dispatched: number; duplicate: number; targetDate: string };

const blankNotificationTemplate = { code: "COURSE_DUE_REMINDER", name: "Nhắc hạn hoàn thành khóa học", eventType: "", titleTemplate: "Khóa học sắp đến hạn", bodyTemplate: "Hạn hoàn thành: {{dueDate}}. Tiến độ hiện tại: {{progressPercent}}%.", inAppEnabled: true, emailEnabled: false, active: true };
const blankReminderRule = { name: "Nhắc trước hạn 7 ngày", templateId: "", relativeDays: "7", hourUtc: "0", enabled: true };

function previewTemplate(value: string) {
  const variables: Record<string, string> = { dueDate: "15/08/2026", progressPercent: "62", courseId: "COURSE-001", classId: "CLASS-001", score: "8.5", maxScore: "10", passed: "true" };
  return value.replace(/\{\{\s*([A-Za-z0-9_.-]+)\s*}}/g, (token, key: string) => variables[key] ?? token);
}

function NotificationAutomationCenter({ user }: { user: PortalUser }) {
  const canTemplates = has(user, "notification-templates:manage");
  const canReminders = has(user, "notification-reminders:manage");
  const { data, loading, error, refresh } = useData(async () => {
    const [templates, rules] = await Promise.all([
      canTemplates || canReminders ? apiRequest<NotificationTemplate[]>("/api/v1/notifications/templates") : Promise.resolve([]),
      canReminders ? apiRequest<ReminderRule[]>("/api/v1/notifications/reminder-rules") : Promise.resolve([]),
    ]);
    return { templates, rules };
  }, [canTemplates, canReminders]);
  const [template, setTemplate] = useState(blankNotificationTemplate);
  const [templateId, setTemplateId] = useState("");
  const [rule, setRule] = useState(blankReminderRule);
  const [ruleId, setRuleId] = useState("");
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState("");

  useEffect(() => {
    if (!rule.templateId && data?.templates[0]) setRule((current) => ({ ...current, templateId: data.templates[0].id }));
  }, [data?.templates, rule.templateId]);

  function editTemplate(item: NotificationTemplate) {
    setTemplateId(item.id);
    setTemplate({ code: item.code, name: item.name, eventType: item.eventType ?? "", titleTemplate: item.titleTemplate, bodyTemplate: item.bodyTemplate, inAppEnabled: item.inAppEnabled, emailEnabled: item.emailEnabled, active: item.active });
  }
  function resetTemplate() { setTemplateId(""); setTemplate(blankNotificationTemplate); }
  function editRule(item: ReminderRule) {
    setRuleId(item.id);
    setRule({ name: item.name, templateId: item.templateId, relativeDays: String(item.relativeDays), hourUtc: String(item.hourUtc), enabled: item.enabled });
  }
  function resetRule() { setRuleId(""); setRule({ ...blankReminderRule, templateId: data?.templates[0]?.id ?? "" }); }

  async function saveTemplate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    try {
      await apiRequest(templateId ? `/api/v1/notifications/templates/${templateId}` : "/api/v1/notifications/templates", { method: templateId ? "PUT" : "POST", body: JSON.stringify({ ...template, eventType: template.eventType || null }) });
      setToast(templateId ? "Đã cập nhật mẫu thông báo." : "Đã tạo mẫu thông báo."); resetTemplate(); await refresh();
    } catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể lưu mẫu thông báo"); }
    finally { setBusy(false); }
  }
  async function deleteTemplate(id: string) {
    if (!window.confirm("Xóa mẫu thông báo này? Mẫu đang được quy tắc sử dụng sẽ được bảo vệ.")) return;
    try { await apiRequest(`/api/v1/notifications/templates/${id}`, { method: "DELETE" }); setToast("Đã xóa mẫu thông báo."); resetTemplate(); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể xóa mẫu"); }
  }
  async function saveRule(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    try {
      await apiRequest(ruleId ? `/api/v1/notifications/reminder-rules/${ruleId}` : "/api/v1/notifications/reminder-rules", { method: ruleId ? "PUT" : "POST", body: JSON.stringify({ name: rule.name, ruleType: "COURSE_DUE", templateId: rule.templateId, relativeDays: Number(rule.relativeDays), hourUtc: Number(rule.hourUtc), enabled: rule.enabled }) });
      setToast(ruleId ? "Đã cập nhật quy tắc nhắc hạn." : "Đã tạo quy tắc nhắc hạn."); resetRule(); await refresh();
    } catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể lưu quy tắc"); }
    finally { setBusy(false); }
  }
  async function deleteRule(id: string) {
    if (!window.confirm("Xóa quy tắc và lịch sử chống gửi trùng liên quan?")) return;
    setBusy(true);
    try { await apiRequest(`/api/v1/notifications/reminder-rules/${id}`, { method: "DELETE" }); setToast("Đã xóa quy tắc."); resetRule(); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể xóa quy tắc"); }
    finally { setBusy(false); }
  }
  async function runRule(id: string) {
    setBusy(true);
    try { const result = await apiRequest<ReminderRun>(`/api/v1/notifications/reminder-rules/${id}/run`, { method: "POST" }); setToast(`Ngày đích ${result.targetDate}: khớp ${result.matched}, gửi mới ${result.dispatched}, bỏ trùng ${result.duplicate}.`); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể chạy thử quy tắc"); }
    finally { setBusy(false); }
  }

  if (loading) return <LoadingState label="Đang mở xưởng thông báo..."/>;
  if (error) return <ErrorState message={error} onRetry={() => void refresh()}/>;
  return <>
    <Header eyebrow="OUTBOX & NHẮC HẠN" title="Mẫu thông báo và tự động nhắc học" description="Nội dung được cấu hình không cần build lại; quy tắc đọc hạn từ Reporting read model, claim từng lượt và không gửi trùng khi worker chạy lại." icon="bell" action={<button className="soft-button" onClick={() => void refresh()}><Icon name="refresh"/>Làm mới</button>}/>
    <div className="stat-grid advanced-stats"><article><strong>{data?.templates.filter((item) => item.active).length ?? 0}</strong><span>Mẫu hoạt động</span></article><article><strong>{data?.rules.filter((item) => item.enabled).length ?? 0}</strong><span>Quy tắc đang chạy</span></article><article><strong>{data?.rules.reduce((sum, item) => sum + item.dispatchedCount, 0) ?? 0}</strong><span>Lượt đã claim</span></article><article><strong>UTC</strong><span>Múi giờ lịch chạy</span></article></div>
    <div className="advanced-grid notification-automation-grid">
      {canTemplates && <Panel title={templateId ? "Sửa mẫu thông báo" : "Tạo mẫu thông báo"} hint="Biến hỗ trợ gồm {{dueDate}}, {{progressPercent}}, {{courseId}}, {{classId}}, {{score}}, {{maxScore}}."><form className="form-stack" onSubmit={saveTemplate}><div className="form-grid two"><label>Mã mẫu<input required maxLength={80} value={template.code} onChange={(event) => setTemplate({ ...template, code: event.target.value })}/></label><label>Tên mẫu<input required maxLength={180} value={template.name} onChange={(event) => setTemplate({ ...template, name: event.target.value })}/></label></div><label>Sự kiện nghiệp vụ<select value={template.eventType} onChange={(event) => setTemplate({ ...template, eventType: event.target.value })}><option value="">Chỉ dùng cho quy tắc thủ công</option><option value="enrollment.learner.enrolled.v1">Ghi danh</option><option value="grading.exam.graded.v1">Có kết quả thi</option><option value="learning.course.completed.v1">Hoàn thành khóa học</option><option value="grading.appeal.resolved.v1">Xử lý phúc khảo</option><option value="certificate.issued.v1">Cấp chứng chỉ</option></select></label><label>Tiêu đề<input required maxLength={240} value={template.titleTemplate} onChange={(event) => setTemplate({ ...template, titleTemplate: event.target.value })}/></label><label>Nội dung<textarea required rows={5} value={template.bodyTemplate} onChange={(event) => setTemplate({ ...template, bodyTemplate: event.target.value })}/></label><div className="check-grid"><label><input type="checkbox" checked={template.inAppEnabled} onChange={(event) => setTemplate({ ...template, inAppEnabled: event.target.checked })}/>Trong ứng dụng</label><label><input type="checkbox" checked={template.emailEnabled} onChange={(event) => setTemplate({ ...template, emailEnabled: event.target.checked })}/>Email</label><label><input type="checkbox" checked={template.active} onChange={(event) => setTemplate({ ...template, active: event.target.checked })}/>Đang hoạt động</label></div><div className="inline-actions"><button className="button primary" disabled={busy}><Icon name="save"/>{templateId ? "Cập nhật mẫu" : "Lưu mẫu"}</button>{templateId && <button type="button" className="button secondary" onClick={resetTemplate}>Hủy sửa</button>}</div></form><div className="notification-template-preview"><small>XEM TRƯỚC</small><strong>{previewTemplate(template.titleTemplate)}</strong><p>{previewTemplate(template.bodyTemplate)}</p><span>{template.inAppEnabled ? "IN-APP" : ""}{template.inAppEnabled && template.emailEnabled ? " · " : ""}{template.emailEnabled ? "EMAIL" : ""}</span></div></Panel>}
      {canReminders && <Panel title={ruleId ? "Sửa quy tắc nhắc hạn" : "Tạo quy tắc nhắc hạn"} hint="Số ngày dương = trước hạn; số âm = sau hạn. Quy tắc mới chỉ gửi cho lượt học chưa hoàn thành."><form className="form-stack" onSubmit={saveRule}><label>Tên quy tắc<input required value={rule.name} onChange={(event) => setRule({ ...rule, name: event.target.value })}/></label><label>Mẫu<select required value={rule.templateId} onChange={(event) => setRule({ ...rule, templateId: event.target.value })}><option value="">Chọn mẫu</option>{data?.templates.map((item) => <option key={item.id} value={item.id}>{item.name}{item.active ? "" : " (đang tắt)"}</option>)}</select></label><div className="form-grid two"><label>Ngày tương đối<input type="number" min="-365" max="365" value={rule.relativeDays} onChange={(event) => setRule({ ...rule, relativeDays: event.target.value })}/></label><label>Giờ chạy UTC<input type="number" min="0" max="23" value={rule.hourUtc} onChange={(event) => setRule({ ...rule, hourUtc: event.target.value })}/></label></div><label className="check-row"><input type="checkbox" checked={rule.enabled} onChange={(event) => setRule({ ...rule, enabled: event.target.checked })}/>Kích hoạt sau khi lưu</label><div className="inline-actions"><button className="button primary" disabled={busy || !rule.templateId}><Icon name="plus"/>{ruleId ? "Cập nhật quy tắc" : "Tạo quy tắc"}</button>{ruleId && <button type="button" className="button secondary" onClick={resetRule}>Hủy sửa</button>}</div></form><div className="reminder-semantics"><Icon name="clock"/><p>{Number(rule.relativeDays) >= 0 ? `Nhắc trước hạn ${Number(rule.relativeDays)} ngày` : `Nhắc sau hạn ${Math.abs(Number(rule.relativeDays))} ngày`} · chạy lúc {rule.hourUtc || 0}:00 UTC</p></div></Panel>}
      <section className="section-card advanced-panel notification-list-panel"><div className="section-title"><div><h2>Thư viện mẫu</h2><p>Mẫu có event type sẽ thay thế nội dung mặc định của sự kiện tương ứng.</p></div></div><div className="compact-list">{data?.templates.length ? data.templates.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.code} · {item.eventType ?? "REMINDER"} · {item.inAppEnabled ? "IN-APP" : ""}{item.emailEnabled ? " EMAIL" : ""}</small></div><StatusBadge value={item.active ? "ACTIVE" : "INACTIVE"}/>{canTemplates && <><button className="icon-button" onClick={() => editTemplate(item)} aria-label="Sửa mẫu"><Icon name="edit"/></button><button className="icon-button danger" onClick={() => void deleteTemplate(item.id)} aria-label="Xóa mẫu"><Icon name="trash"/></button></>}</article>) : <EmptyState title="Chưa có mẫu" description="Tạo mẫu đầu tiên để dùng cho nhắc hạn hoặc sự kiện nghiệp vụ."/>}</div></section>
      <section className="section-card advanced-panel notification-list-panel"><div className="section-title"><div><h2>Lịch nhắc hạn</h2><p>Chạy idempotent theo quy tắc + ghi danh + hạn hoàn thành.</p></div></div><div className="compact-list">{data?.rules.length ? data.rules.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.relativeDays >= 0 ? `trước ${item.relativeDays} ngày` : `sau ${Math.abs(item.relativeDays)} ngày`} · {item.hourUtc}:00 UTC · lần tới {when(item.nextRunAt)}</small></div><StatusBadge value={item.enabled ? "ACTIVE" : "INACTIVE"}/><button className="button secondary compact" disabled={busy} onClick={() => void runRule(item.id)}><Icon name="play"/>Chạy thử</button>{canReminders && <><button className="icon-button" onClick={() => editRule(item)} aria-label="Sửa quy tắc"><Icon name="edit"/></button><button className="icon-button danger" onClick={() => void deleteRule(item.id)} aria-label="Xóa quy tắc"><Icon name="trash"/></button></>}</article>) : <EmptyState title="Chưa có quy tắc" description="Mẫu mặc định được tạo sẵn nhưng lịch ban đầu được tắt để quản trị viên kiểm tra trước."/>}</div></section>
    </div>{toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}

// Operations -----------------------------------------------------------------
type Health = { name: string; status: string; version?: string | null; details?: Row };
type OperationJob = { id: string; type: string; status: string; requestedBy: string; requestedAt: string; startedAt?: string | null; finishedAt?: string | null; errorMessage?: string | null; claimedBy?: string | null; heartbeatAt?: string | null; attemptCount: number };
type OperationSchedule = { id: string; name: string; operationType: string; frequency: string; dayOfWeek?: number | null; hourUtc: number; parameters: Row; enabled: boolean; nextRunAt: string; updatedAt: string };

function OperationsCenter({ user }: { user: PortalUser }) {
  const allowed = has(user, "operations:manage");
  const { data, loading, error, refresh } = useData(async () => {
    if (!allowed) throw new Error("Bạn không có quyền vận hành hệ thống.");
    const [health, jobs, schedules] = await Promise.all([apiRequest<Health[]>("/api/v1/operations/health"), apiRequest<OperationJob[]>("/api/v1/operations/jobs"), apiRequest<OperationSchedule[]>("/api/v1/operations/schedules")]);
    return { health, jobs, schedules };
  }, [allowed]);
  const [draft, setDraft] = useState({ name: "Sao lưu hằng ngày", frequency: "DAILY", dayOfWeek: "1", hourUtc: "18", enabled: true });
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState("");
  const healthy = data?.health.filter((item) => item.status === "UP" || item.status === "HEALTHY").length ?? 0;

  async function request(type: "BACKUP" | "MAINTENANCE", parameters: Row = {}) {
    setBusy(true);
    try { await apiRequest(`/api/v1/operations/jobs/${type}`, { method: "POST", body: JSON.stringify({ parameters }) }); setToast(`Đã tạo yêu cầu ${type}. Operations Agent sẽ nhận việc qua lease an toàn.`); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể tạo tác vụ"); }
    finally { setBusy(false); }
  }
  async function createSchedule(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true);
    try { await apiRequest("/api/v1/operations/schedules", { method: "POST", body: JSON.stringify({ name: draft.name, operationType: "BACKUP", frequency: draft.frequency, dayOfWeek: draft.frequency === "WEEKLY" ? Number(draft.dayOfWeek) : null, hourUtc: Number(draft.hourUtc), parameters: {}, enabled: draft.enabled }) }); setToast("Đã tạo lịch sao lưu."); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể tạo lịch sao lưu"); }
    finally { setBusy(false); }
  }
  async function removeSchedule(id: string) {
    if (!window.confirm("Xóa lịch vận hành này?")) return;
    setBusy(true);
    try { await apiRequest(`/api/v1/operations/schedules/${id}`, { method: "DELETE" }); setToast("Đã xóa lịch."); await refresh(); }
    catch (cause) { setToast(cause instanceof Error ? cause.message : "Không thể xóa lịch vận hành"); }
    finally { setBusy(false); }
  }

  if (loading) return <LoadingState label="Đang thăm dò cụm dịch vụ..."/>;
  if (error) return <ErrorState message={error} onRetry={() => void refresh()}/>;
  return <>
    <Header eyebrow="ON-PREMISE CONTROL PLANE" title="Vận hành, sao lưu và bảo trì" description="Web chỉ tạo công việc có kiểu cố định; Operations Agent trên host mới được thực thi script trong allowlist bằng claim token và lease." icon="operations" action={<><button className="soft-button" onClick={() => void refresh()}><Icon name="refresh"/>Thăm dò lại</button><button className="primary-button" disabled={busy} onClick={() => void request("BACKUP")}><Icon name="save"/>Sao lưu ngay</button></>}/>
    <div className="stat-grid advanced-stats"><article><strong>{healthy}/{data?.health.length ?? 0}</strong><span>Service khỏe</span></article><article><strong>{data?.jobs.filter((item) => item.status === "RUNNING").length ?? 0}</strong><span>Tác vụ đang chạy</span></article><article><strong>{data?.jobs.filter((item) => item.status === "FAILED").length ?? 0}</strong><span>Tác vụ lỗi</span></article><article><strong>{data?.schedules.filter((item) => item.enabled).length ?? 0}</strong><span>Lịch hoạt động</span></article></div>
    <div className="advanced-grid">
      <Panel title="Sức khỏe dịch vụ" hint="Probe có timeout; một service hỏng không làm treo toàn bộ bảng vận hành."><div className="service-health-grid">{data?.health.map((item) => <article key={item.name}><span className={`health-orb ${item.status.toLowerCase()}`}/><div><strong>{item.name}</strong><small>{item.version ?? "Không công bố phiên bản"}</small></div><StatusBadge value={item.status}/></article>)}</div></Panel>
      <Panel title="Lịch sao lưu" hint="Giờ chạy theo UTC; lịch chỉ tạo job BACKUP hoặc MAINTENANCE đã được kiểm tra tham số."><form className="form-stack" onSubmit={createSchedule}><label>Tên lịch<input required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })}/></label><div className="form-grid three"><label>Tần suất<select value={draft.frequency} onChange={(e) => setDraft({ ...draft, frequency: e.target.value })}><option value="DAILY">Hằng ngày</option><option value="WEEKLY">Hằng tuần</option></select></label>{draft.frequency === "WEEKLY" && <label>Ngày<select value={draft.dayOfWeek} onChange={(e) => setDraft({ ...draft, dayOfWeek: e.target.value })}>{[1,2,3,4,5,6,7].map((day) => <option key={day} value={day}>{day}</option>)}</select></label>}<label>Giờ UTC<input type="number" min="0" max="23" value={draft.hourUtc} onChange={(e) => setDraft({ ...draft, hourUtc: e.target.value })}/></label></div><button className="button primary" disabled={busy}><Icon name="plus"/>Tạo lịch sao lưu</button></form><div className="compact-list">{data?.schedules.map((item) => <article key={item.id}><div><strong>{item.name}</strong><small>{item.frequency} · {item.hourUtc}:00 UTC · {when(item.nextRunAt)}</small></div><StatusBadge value={item.enabled ? "ACTIVE" : "INACTIVE"}/><button className="icon-button danger" onClick={() => void removeSchedule(item.id)} aria-label="Xóa lịch"><Icon name="trash"/></button></article>)}</div></Panel>
      <Panel title="Nhật ký công việc" hint="UPDATE/ROLLBACK cố ý fail-closed cho tới khi có adapter gói phát hành ký số."><div className="compact-list">{data?.jobs.length ? data.jobs.slice(0, 50).map((item) => <article key={item.id}><div><strong>{item.type} · lần thử {item.attemptCount}</strong><small>Yêu cầu {when(item.requestedAt)}{item.claimedBy ? ` · agent ${item.claimedBy}` : ""}</small>{item.errorMessage && <em>{item.errorMessage}</em>}</div><StatusBadge value={item.status}/></article>) : <EmptyState title="Chưa có tác vụ" description="Tạo sao lưu hoặc lịch đầu tiên để kiểm tra Operations Agent."/>}</div></Panel>
      <Panel title="Chế độ bảo trì" hint="Chặn thao tác ghi trong thời gian restore hoặc cập nhật; không xóa dữ liệu."><div className="maintenance-actions"><button className="button danger" disabled={busy} onClick={() => void request("MAINTENANCE", { mode: "ON" })}><Icon name="lock"/>Bật bảo trì</button><button className="button secondary" disabled={busy} onClick={() => void request("MAINTENANCE", { mode: "OFF" })}><Icon name="check"/>Tắt bảo trì</button></div></Panel>
    </div>{toast && <Toast message={toast} onClose={() => setToast("")}/>} 
  </>;
}
