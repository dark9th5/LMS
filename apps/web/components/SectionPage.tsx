"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, ApiRequestError, unwrapItems } from "@/lib/api";
import type { IconName, PortalUser } from "@/lib/types";
import { EntityDialog, type FormField } from "./EntityDialog";
import { Icon } from "./Icon";

type UnknownRecord = Record<string, unknown>;
type TableRow = { id: string; cells: string[] };
type SectionDefinition = {
  title: string;
  description: string;
  icon: IconName;
  columns: string[];
  endpoint: (user: PortalUser) => string;
  permission?: string;
  actionLabel?: string;
  roles?: string[];
};
type References = {
  courses: UnknownRecord[];
  units: UnknownRecord[];
  instructors: UnknownRecord[];
};

const definitions: Record<string, SectionDefinition> = {
  courses: {
    title: "Khóa học & nội dung",
    description: "Xây dựng, xuất bản và quản lý thư viện đào tạo trong đúng phạm vi được phân công.",
    icon: "book",
    columns: ["Khóa học", "Mã", "Thời lượng", "Phiên bản", "Trạng thái"],
    endpoint: () => "/api/v1/courses?size=100",
    permission: "courses:write",
    actionLabel: "Tạo khóa học",
    roles: ["ADMIN", "INSTRUCTOR"],
  },
  users: {
    title: "Người dùng",
    description: "Quản lý vòng đời tài khoản, trạng thái và vai trò truy cập.",
    icon: "users",
    columns: ["Họ và tên", "Tài khoản", "Mã người dùng", "Vai trò", "Trạng thái"],
    endpoint: () => "/api/v1/users?size=100",
    permission: "users:write",
    actionLabel: "Thêm người dùng",
    roles: ["ADMIN"],
  },
  organization: {
    title: "Cơ cấu tổ chức",
    description: "Quản lý cấu trúc đơn vị nhiều cấp phục vụ ghi danh và báo cáo.",
    icon: "building",
    columns: ["Đơn vị", "Mã", "Loại", "Đơn vị cha", "Trạng thái"],
    endpoint: () => "/api/v1/organization/units",
    permission: "organization:write",
    actionLabel: "Thêm đơn vị",
    roles: ["ADMIN"],
  },
  classes: {
    title: "Lớp & ghi danh",
    description: "Mở đợt đào tạo, phân công giảng viên và quản lý học viên theo lớp.",
    icon: "class",
    columns: ["Lớp đào tạo", "Mã lớp", "Khóa học", "Hạn hoàn thành", "Trạng thái"],
    endpoint: () => "/api/v1/classes",
    permission: "classes:write",
    actionLabel: "Mở lớp mới",
    roles: ["ADMIN", "INSTRUCTOR"],
  },
  learning: {
    title: "Học tập của tôi",
    description: "Theo dõi tiến độ, thời gian học và tiếp tục từ vị trí gần nhất.",
    icon: "learn",
    columns: ["Khóa học", "Tiến độ", "Thời gian học", "Truy cập gần nhất", "Trạng thái"],
    endpoint: () => "/api/v1/learning/me",
    roles: ["STUDENT"],
  },
  exams: {
    title: "Bài kiểm tra",
    description: "Xem các bài kiểm tra trong phạm vi quản lý hoặc được phép thực hiện.",
    icon: "exam",
    columns: ["Bài kiểm tra", "Khóa học", "Thời lượng", "Số lượt", "Trạng thái"],
    endpoint: () => "/api/v1/exams",
    roles: ["ADMIN", "INSTRUCTOR", "STUDENT"],
  },
  grading: {
    title: "Chấm điểm",
    description: "Xử lý hàng chờ tự luận và theo dõi kết quả chấm trong phạm vi được giao.",
    icon: "grade",
    columns: ["Kết quả", "Bài thi", "Điểm", "Tỷ lệ", "Trạng thái"],
    endpoint: (user) => user.permissions.includes("grading:manage") ? "/api/v1/grades/queue" : "/api/v1/grades/me",
    roles: ["ADMIN", "INSTRUCTOR"],
  },
  reports: {
    title: "Dashboard & báo cáo",
    description: "Dữ liệu tổng hợp theo phạm vi quyền, có thể xuất CSV mà không làm treo giao diện.",
    icon: "report",
    columns: ["Ghi danh", "Lớp", "Khóa học", "Tiến độ", "Kết quả"],
    endpoint: () => "/api/v1/reports/learning",
    permission: "reports:export",
    actionLabel: "Xuất CSV",
    roles: ["ADMIN", "INSTRUCTOR"],
  },
  certificates: {
    title: "Chứng chỉ",
    description: "Tra cứu chứng chỉ hiệu lực, thu hồi hoặc cấp lại theo đúng quyền.",
    icon: "certificate",
    columns: ["Mã xác minh", "Khóa học", "Lần cấp", "Ngày cấp", "Trạng thái"],
    endpoint: (user) => user.permissions.includes("certificates:manage") ? "/api/v1/certificates" : "/api/v1/certificates/me",
    roles: ["ADMIN", "STUDENT"],
  },
  operations: {
    title: "Vận hành hệ thống",
    description: "Theo dõi health check và tạo yêu cầu sao lưu trong môi trường On-Premise.",
    icon: "operations",
    columns: ["Thành phần", "Phiên bản", "Trạng thái", "Chi tiết", "Kiểm tra"],
    endpoint: () => "/api/v1/operations/health",
    permission: "operations:manage",
    actionLabel: "Yêu cầu sao lưu",
    roles: ["ADMIN"],
  },
  settings: {
    title: "Cấu hình sản phẩm",
    description: "Nhận diện, ngôn ngữ và feature flag được áp dụng bằng cấu hình, không cần build lại.",
    icon: "settings",
    columns: ["Nhóm cấu hình", "Giá trị", "Nguồn", "Cập nhật", "Trạng thái"],
    endpoint: () => "/api/v1/configuration",
    roles: ["ADMIN"],
  },
};

const statusLabels: Record<string, string> = {
  ACTIVE: "Hoạt động",
  INACTIVE: "Ngừng hoạt động",
  LOCKED: "Đã khóa",
  DRAFT: "Bản nháp",
  PUBLISHED: "Đã xuất bản",
  HIDDEN: "Tạm ẩn",
  ARCHIVED: "Lưu trữ",
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
  NOT_STARTED: "Chưa bắt đầu",
  IN_PROGRESS: "Đang học",
  COMPLETED: "Đã hoàn thành",
  OVERDUE: "Quá hạn",
  SUBMITTED: "Đã nộp",
  EXPIRED: "Hết giờ",
  GRADED: "Đã chấm",
  PENDING_MANUAL: "Chờ chấm",
  REVOKED: "Thu hồi",
  REISSUED: "Cấp lại",
  DEVELOPMENT: "Development",
  HEALTHY: "Hoạt động",
  UP: "Hoạt động",
  DOWN: "Gián đoạn",
};

const roleLabels: Record<string, string> = {
  ADMIN: "Quản trị viên",
  INSTRUCTOR: "Giảng viên",
  STUDENT: "Học viên",
};

function record(value: unknown): UnknownRecord {
  return typeof value === "object" && value !== null ? value as UnknownRecord : {};
}
function text(value: unknown, fallback = "—"): string {
  if (value === null || value === undefined || value === "") return fallback;
  return String(value);
}
function status(value: unknown): string {
  const raw = text(value, "UNKNOWN");
  return statusLabels[raw] ?? raw;
}
function date(value: unknown): string {
  if (!value) return "—";
  const parsed = new Date(String(value));
  if (Number.isNaN(parsed.getTime())) return text(value);
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(parsed);
}
function minutes(value: unknown): string {
  const total = Number(value ?? 0);
  if (!Number.isFinite(total) || total <= 0) return "—";
  if (total < 60) return `${total} phút`;
  return `${Math.floor(total / 60)} giờ ${total % 60} phút`;
}
function seconds(value: unknown): string {
  const total = Math.max(0, Number(value ?? 0));
  if (!Number.isFinite(total) || total === 0) return "0 phút";
  return minutes(Math.round(total / 60));
}
function percentage(value: unknown): string {
  const number = Number(value ?? 0);
  return `${Number.isFinite(number) ? Math.round(number) : 0}%`;
}
function idName(items: UnknownRecord[], id: unknown, primary: string, secondary?: string): string {
  const found = items.find((item) => text(item.id, "") === text(id, ""));
  if (!found) return text(id);
  return secondary && found[secondary] ? `${text(found[primary])} (${text(found[secondary])})` : text(found[primary]);
}

function normalize(section: string, payload: unknown, references: References): TableRow[] {
  if (section === "settings") {
    const item = record(payload);
    return [
      { id: "product", cells: ["Tên hệ thống", text(item.productName), "Cấu hình", date(item.updatedAt), "Đã áp dụng"] },
      { id: "locale", cells: ["Ngôn ngữ mặc định", text(item.defaultLocale), "Cấu hình", date(item.updatedAt), "Đã áp dụng"] },
      { id: "theme", cells: ["Màu nhận diện", `${text(item.primaryColor)} · ${text(item.accentColor)}`, "Cấu hình", date(item.updatedAt), "Đã áp dụng"] },
      { id: "features", cells: ["Feature flag", `${Object.keys(record(item.featureFlags)).length} cấu hình`, "Cấu hình", date(item.updatedAt), "Hợp lệ"] },
    ];
  }

  const values = unwrapItems<UnknownRecord>(payload as UnknownRecord[] | { items?: UnknownRecord[] });
  return values.map((item, index) => {
    const id = text(item.id, `${section}-${index}`);
    switch (section) {
      case "users": {
        const roles = Array.isArray(item.roles) ? item.roles.map((role) => roleLabels[text(role)] ?? text(role)).join(", ") : "—";
        return { id, cells: [text(item.fullName), text(item.username), text(item.code), roles, status(item.status)] };
      }
      case "organization":
        return { id, cells: [text(item.name), text(item.code), status(item.type), idName(references.units, item.parentId, "name"), status(item.status)] };
      case "courses":
        return { id, cells: [text(item.name), text(item.code), minutes(item.durationMinutes), `v${text(item.contentVersion, "1")}`, status(item.status)] };
      case "classes":
        return { id, cells: [text(item.name), text(item.code), idName(references.courses, item.courseId, "name", "code"), date(item.dueAt), status(item.status)] };
      case "learning":
        return { id, cells: [idName(references.courses, item.courseId, "name", "code"), percentage(item.progressPercent), seconds(item.totalLearningSeconds), date(item.lastAccessedAt), status(item.status)] };
      case "exams":
        return { id, cells: [text(item.title), idName(references.courses, item.courseId, "name", "code"), minutes(item.durationMinutes), text(item.maxAttempts), status(item.status)] };
      case "grading":
        return { id, cells: [`#${id.slice(0, 8)}`, text(item.examId), `${text(item.score, "0")}/${text(item.maxScore, "0")}`, percentage(item.percentage), status(item.status)] };
      case "reports":
        return { id, cells: [text(item.enrollmentId), text(item.classId), idName(references.courses, item.courseId, "name", "code"), percentage(item.progressPercent), item.passed === true ? "Đạt" : item.passed === false ? "Chưa đạt" : status(item.completed ? "COMPLETED" : "IN_PROGRESS")] };
      case "certificates":
        return { id, cells: [text(item.verificationCode), idName(references.courses, item.courseId, "name", "code"), text(item.generation), date(item.issuedAt), status(item.status)] };
      case "operations":
        return { id: text(item.name, id), cells: [text(item.name), text(item.version), status(item.status), item.details ? "Có dữ liệu health check" : "—", new Intl.DateTimeFormat("vi-VN", { timeStyle: "medium" }).format(new Date())] };
      default:
        return { id, cells: Object.values(item).slice(0, 5).map((value) => text(value)) };
    }
  });
}

function actionFields(section: string, references: References): FormField[] {
  switch (section) {
    case "users":
      return [
        { name: "code", label: "Mã người dùng", required: true, placeholder: "NV001" },
        { name: "username", label: "Tên đăng nhập", required: true, placeholder: "nguyenvana" },
        { name: "fullName", label: "Họ và tên", required: true },
        { name: "email", label: "Email", type: "email" },
        { name: "password", label: "Mật khẩu tạm", type: "password", required: true, placeholder: "Tối thiểu 12 ký tự" },
        { name: "organizationUnitId", label: "Đơn vị", type: "select", options: references.units.map((unit) => ({ label: `${text(unit.name)} (${text(unit.code)})`, value: text(unit.id, "") })) },
        { name: "role", label: "Vai trò", type: "select", required: true, defaultValue: "STUDENT", options: [
          { label: "Học viên", value: "STUDENT" },
          { label: "Giảng viên", value: "INSTRUCTOR" },
          { label: "Quản trị viên", value: "ADMIN" },
        ] },
      ];
    case "organization":
      return [
        { name: "code", label: "Mã đơn vị", required: true, placeholder: "P.KT" },
        { name: "name", label: "Tên đơn vị", required: true },
        { name: "type", label: "Loại đơn vị", type: "select", required: true, defaultValue: "DEPARTMENT", options: [
          { label: "Tổ chức", value: "ORGANIZATION" }, { label: "Khối", value: "DIVISION" },
          { label: "Phòng ban", value: "DEPARTMENT" }, { label: "Nhóm", value: "TEAM" },
          { label: "Lớp", value: "CLASS" }, { label: "Nhóm học", value: "GROUP" },
        ] },
        { name: "parentId", label: "Đơn vị cha", type: "select", options: references.units.map((unit) => ({ label: text(unit.name), value: text(unit.id, "") })) },
      ];
    case "courses":
      return [
        { name: "code", label: "Mã khóa học", required: true, placeholder: "COURSE-001" },
        { name: "name", label: "Tên khóa học", required: true },
        { name: "durationMinutes", label: "Thời lượng (phút)", type: "number", min: 0, defaultValue: "30" },
        { name: "passingScore", label: "Điểm đạt (%)", type: "number", min: 0, max: 100, defaultValue: "70" },
        { name: "description", label: "Mô tả", type: "textarea", placeholder: "Mục tiêu và nội dung chính của khóa học" },
      ];
    case "classes":
      return [
        { name: "code", label: "Mã lớp", required: true, placeholder: "CLASS-2026-01" },
        { name: "name", label: "Tên lớp", required: true },
        { name: "courseId", label: "Khóa học đã xuất bản", type: "select", required: true, options: references.courses.filter((course) => course.status === "PUBLISHED").map((course) => ({ label: `${text(course.name)} (${text(course.code)})`, value: text(course.id, "") })) },
        { name: "instructorId", label: "Giảng viên phụ trách", type: "select", required: true, options: references.instructors.map((instructor) => ({ label: `${text(instructor.fullName)} (${text(instructor.username)})`, value: text(instructor.id, "") })) },
        { name: "dueAt", label: "Hạn hoàn thành", type: "date" },
      ];
    default:
      return [];
  }
}

function requestForAction(section: string, values: Record<string, string>): { path: string; body: UnknownRecord } {
  switch (section) {
    case "users":
      return { path: "/api/v1/users", body: { code: values.code, username: values.username, password: values.password, fullName: values.fullName, email: values.email || null, organizationUnitId: values.organizationUnitId || null, roleCodes: [values.role] } };
    case "organization":
      return { path: "/api/v1/organization/units", body: { code: values.code, name: values.name, type: values.type, parentId: values.parentId || null, status: "ACTIVE" } };
    case "courses":
      return { path: "/api/v1/courses", body: { code: values.code, name: values.name, description: values.description || null, durationMinutes: Number(values.durationMinutes || 0), passingScore: Number(values.passingScore || 70), completionPolicyJson: "{\"requiredLessonPercent\":100}" } };
    case "classes":
      return { path: "/api/v1/classes", body: { code: values.code, name: values.name, courseId: values.courseId, dueAt: values.dueAt ? new Date(`${values.dueAt}T23:59:59`).toISOString() : null, instructorIds: values.instructorId ? [values.instructorId] : [] } };
    default:
      throw new Error("Thao tác chưa được cấu hình");
  }
}

function statusClass(value: string): string {
  if (/Hoạt động|Đã xuất bản|Đã hoàn thành|Hiệu lực|Đạt|Đã áp dụng|Hợp lệ|Đang mở/.test(value)) return "success";
  if (/Chờ|Bản nháp|Sắp|Đang học|Chưa bắt đầu|Development/.test(value)) return "pending";
  if (/Khóa|Gián đoạn|Quá hạn|Thu hồi|Chưa đạt|Hết giờ/.test(value)) return "danger";
  return "muted";
}

export function SectionPage({ section, user }: { section: string; user: PortalUser }) {
  const definition = definitions[section];
  const canAccess = Boolean(definition && (!definition.roles || definition.roles.some((role) => user.roles.includes(role))));
  const [payload, setPayload] = useState<unknown>(null);
  const [references, setReferences] = useState<References>({ courses: [], units: [], instructors: [] });
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  const canUseAction = Boolean(canAccess && definition?.actionLabel && (!definition.permission || user.permissions.includes(definition.permission)));

  const load = useCallback(async () => {
    if (!definition || !canAccess) {
      setLoading(false);
      setError("Bạn không có quyền truy cập chức năng này.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const needsCourses = ["classes", "learning", "exams", "reports", "certificates"].includes(section);
      const needsUnits = ["organization", "users"].includes(section) && user.permissions.includes("organization:read");
      const needsInstructors = section === "classes" && user.permissions.includes("users:read");
      const [coursePayload, unitPayload, instructorPayload] = await Promise.all([
        needsCourses ? apiRequest("/api/v1/courses?size=100").catch(() => []) : Promise.resolve([]),
        needsUnits ? apiRequest("/api/v1/organization/units").catch(() => []) : Promise.resolve([]),
        needsInstructors ? apiRequest("/api/v1/users?role=INSTRUCTOR&size=100").catch(() => []) : Promise.resolve([]),
      ]);
      setReferences({
        courses: unwrapItems<UnknownRecord>(coursePayload as { items?: UnknownRecord[] } | UnknownRecord[]),
        units: unwrapItems<UnknownRecord>(unitPayload as UnknownRecord[]),
        instructors: unwrapItems<UnknownRecord>(instructorPayload as { items?: UnknownRecord[] } | UnknownRecord[]),
      });
      setPayload(await apiRequest(definition.endpoint(user)));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể tải dữ liệu");
    } finally {
      setLoading(false);
    }
  }, [canAccess, definition, section, user]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(""), 2600);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const rows = useMemo(() => normalize(section, payload, references), [section, payload, references]);
  const filteredRows = useMemo(() => {
    const keyword = query.trim().toLocaleLowerCase("vi");
    return keyword ? rows.filter((row) => row.cells.join(" ").toLocaleLowerCase("vi").includes(keyword)) : rows;
  }, [query, rows]);
  const fields = useMemo(() => canAccess ? actionFields(section, references) : [], [canAccess, section, references]);

  async function runPrimaryAction() {
    setFormError("");
    try {
      if (section === "reports") {
        const response = await fetch("/api/gateway/api/v1/reports/learning/export.csv", { cache: "no-store" });
        if (!response.ok) throw new Error("Không thể xuất báo cáo");
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = "learning-report.csv";
        anchor.click();
        URL.revokeObjectURL(url);
        setToast("Đã tạo file báo cáo CSV.");
        return;
      }
      if (section === "operations") {
        await apiRequest("/api/v1/operations/jobs/BACKUP", { method: "POST", body: JSON.stringify({ parameters: {} }) });
        setToast("Đã tạo yêu cầu sao lưu.");
        return;
      }
      if (section === "classes" && !references.courses.some((course) => course.status === "PUBLISHED")) {
        setToast("Cần xuất bản ít nhất một khóa học trước khi mở lớp.");
        return;
      }
      if (section === "classes" && references.instructors.length === 0) {
        setToast("Cần tạo ít nhất một tài khoản giảng viên trước khi mở lớp.");
        return;
      }
      if (fields.length > 0) setDialogOpen(true);
    } catch (caught) {
      setToast(caught instanceof Error ? caught.message : "Không thể thực hiện thao tác");
    }
  }

  async function submit(values: Record<string, string>) {
    setSaving(true);
    setFormError("");
    try {
      const request = requestForAction(section, values);
      await apiRequest(request.path, { method: "POST", body: JSON.stringify(request.body) });
      setDialogOpen(false);
      setToast("Đã lưu dữ liệu thành công.");
      await load();
    } catch (caught) {
      const message = caught instanceof ApiRequestError ? caught.message : caught instanceof Error ? caught.message : "Không thể lưu dữ liệu";
      setFormError(message);
    } finally {
      setSaving(false);
    }
  }

  if (!definition || !canAccess) {
    return <div className="data-card standalone-state"><div className="state-card error-state"><b>Không có quyền truy cập</b><p>Chức năng này không thuộc vai trò hiện tại của bạn.</p></div></div>;
  }

  return (
    <>
      <div className="page-head">
        <div>
          <span className="page-icon"><Icon name={definition.icon} /></span>
          <div><h1>{definition.title}</h1><p>{definition.description}</p></div>
        </div>
        {canUseAction && (
          <button className="primary-button" onClick={() => void runPrimaryAction()}>
            <Icon name="plus" size={18} />{definition.actionLabel}
          </button>
        )}
      </div>

      <div className="filter-card">
        <div className="table-search"><Icon name="search" size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm kiếm trong danh sách..." /></div>
        <button className="soft-button" onClick={() => void load()} disabled={loading}>Làm mới</button>
        <span className="result-count">{filteredRows.length} kết quả</span>
      </div>

      <div className="data-card">
        {loading ? (
          <div className="state-card"><span className="state-spinner" /><b>Đang tải dữ liệu</b><p>Hệ thống đang lấy dữ liệu từ service phụ trách.</p></div>
        ) : error ? (
          <div className="state-card error-state"><b>Không thể hiển thị dữ liệu</b><p>{error}</p><button className="soft-button" onClick={() => void load()}>Thử lại</button></div>
        ) : filteredRows.length === 0 ? (
          <div className="state-card"><b>Chưa có dữ liệu phù hợp</b><p>Hãy thay đổi từ khóa hoặc tạo bản ghi đầu tiên nếu bạn có quyền.</p></div>
        ) : (
          <>
            <div className="table-wrap"><table><thead><tr>{definition.columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>
              {filteredRows.map((row) => <tr key={row.id}>{row.cells.map((cell, index) => <td key={`${row.id}-${index}`}>{index === 0 ? <div className="primary-cell"><span className={`mini-avatar m${Math.abs(row.id.length + index) % 4}`}>{cell.slice(0, 2).toUpperCase()}</span><b>{cell}</b></div> : index === row.cells.length - 1 ? <span className={`status-pill ${statusClass(cell)}`}>{cell}</span> : cell}</td>)}</tr>)}
            </tbody></table></div>
            <div className="pagination"><span>Hiển thị {filteredRows.length} bản ghi</span><div><button disabled>‹</button><button className="selected">1</button><button disabled>›</button></div></div>
          </>
        )}
      </div>

      <EntityDialog
        open={dialogOpen}
        title={definition.actionLabel ?? "Thêm dữ liệu"}
        description="Chỉ nhập các thông tin cốt lõi. Các cấu hình chi tiết có thể bổ sung sau khi tạo."
        fields={fields}
        busy={saving}
        error={formError}
        onClose={() => !saving && setDialogOpen(false)}
        onSubmit={submit}
      />
      {toast && <div className="toast"><span>✓</span>{toast}</div>}
    </>
  );
}
