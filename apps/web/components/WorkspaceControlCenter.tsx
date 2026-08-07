"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type {
  FormEvent,
  MouseEventHandler,
  ReactNode,
} from "react";
import { apiRequest, createIdempotencyKey, unwrapItems } from "@/lib/api";
import { readableText } from "@/lib/color";
import { normalizeThemeKey, type ThemeKey } from "@/lib/themes";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { AiConnectionCenter } from "./AiConnectionCenter";
import { RepeatableField } from "./RepeatableField";

type Row = Record<string, any>;
type Notice = { tone: "success" | "error" | "info"; message: string } | null;

const centerPermissions: Record<string, string[]> = {
  users: ["users:read", "roles:read"],
  organization: ["organization:read"],
  settings: ["branding:manage", "configuration:manage", "integrations:manage"],
  results: ["grades:read:self", "grade-appeals:create", "grade-appeals:manage"],
};

export const workspaceSections = new Set(Object.keys(centerPermissions));

export function WorkspaceControlCenter({
  section,
  user,
}: {
  section: string;
  user: PortalUser;
}) {
  const requiredPermissions = centerPermissions[section] ?? [];
  const allowed =
    requiredPermissions.length === 0 ||
    requiredPermissions.some((permission) =>
      user.permissions.includes(permission),
    );
  if (!allowed) return <WorkspaceDenied />;
  switch (section) {
    case "users":
      return <UserAccessConsole user={user} />;
    case "organization":
      return <OrganizationConsole user={user} />;
    case "settings":
      return <WorldSettingsConsole user={user} />;
    case "results":
      return <GradeResultsConsole user={user} />;
    default:
      return <WorkspaceDenied />;
  }
}

function WorkspaceDenied() {
  return (
    <section className="workspace-empty-state">
      <span className="workspace-empty-mark">!</span>
      <h1>Khu vực chưa khả dụng</h1>
      <p>Tài khoản hiện tại chưa được trao quyền cho khu vực này.</p>
    </section>
  );
}

function useLoad<T>(loader: () => Promise<T>, dependencies: unknown[] = []) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const requestId = useRef(0);
  const mounted = useRef(true);
  const hasData = useRef(false);

  useEffect(() => () => {
    mounted.current = false;
  }, []);

  const refresh = useCallback(async () => {
    const currentRequest = ++requestId.current;
    // Preserve already-rendered content during background refresh to avoid page flicker.
    if (!hasData.current) setLoading(true);
    setError("");
    try {
      const next = await loader();
      if (mounted.current && currentRequest === requestId.current) {
        hasData.current = true;
        setData(next);
      }
    } catch (cause) {
      if (mounted.current && currentRequest === requestId.current) {
        setError(cause instanceof Error ? cause.message : "Không thể tải dữ liệu");
      }
    } finally {
      if (mounted.current && currentRequest === requestId.current) setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);

  useEffect(() => {
    void refresh();
  }, [refresh]);
  return { data, setData, loading, error, refresh };
}

function WorkspaceHero({
  eyebrow,
  title,
  description,
  icon,
  actions,
  stats,
}: {
  eyebrow: string;
  title: string;
  description: string;
  icon: ReactNode;
  actions?: ReactNode;
  stats?: Array<{ value: string | number; label: string; tone?: string }>;
}) {
  return (
    <header className="workspace-hero">
      <div className="workspace-hero-glyph" aria-hidden>
        {icon}
        <i />
        <b />
      </div>
      <div className="workspace-hero-copy">
        <span>{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
        {actions && <div className="workspace-hero-actions">{actions}</div>}
      </div>
      {stats && (
        <div className="workspace-hero-stats">
          {stats.map((stat) => (
            <div key={stat.label} className={stat.tone ?? ""}>
              <strong>{stat.value}</strong>
              <span>{stat.label}</span>
            </div>
          ))}
        </div>
      )}
    </header>
  );
}

function Panel({
  title,
  subtitle,
  action,
  children,
  className = "",
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <article className={`workspace-panel ${className}`}>
      <header>
        <div>
          <h2>{title}</h2>
          {subtitle && <p>{subtitle}</p>}
        </div>
        {action}
      </header>
      <div className="workspace-panel-body">{children}</div>
    </article>
  );
}

function NoticeBar({
  notice,
  onClose,
}: {
  notice: Notice;
  onClose: () => void;
}) {
  if (!notice) return null;
  return (
    <div className={`workspace-notice ${notice.tone}`} role="status">
      <span>
        {notice.tone === "success" ? "✦" : notice.tone === "error" ? "!" : "◇"}
      </span>
      <p>{notice.message}</p>
      <button onClick={onClose} aria-label="Đóng">
        <Icon name="close" size={16} />
      </button>
    </div>
  );
}

function Busy({ label = "Đang tải dữ liệu…" }: { label?: string }) {
  return (
    <div className="workspace-busy">
      <span />
      <p>{label}</p>
    </div>
  );
}
function Empty({ text }: { text: string }) {
  return (
    <div className="workspace-inline-empty">
      <span>✧</span>
      <p>{text}</p>
    </div>
  );
}
function Field({
  label,
  children,
  wide = false,
  hint,
}: {
  label: string;
  children: ReactNode;
  wide?: boolean;
  hint?: string;
}) {
  return (
    <label className={`workspace-field ${wide ? "wide" : ""}`}>
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
function Tag({ children, tone = "" }: { children: ReactNode; tone?: string }) {
  return <span className={`workspace-tag ${tone}`}>{children}</span>;
}
function Button({
  children,
  onClick,
  type = "button",
  disabled,
  tone = "primary",
  title,
}: {
  children: ReactNode;
  onClick?: MouseEventHandler<HTMLButtonElement>;
  type?: "button" | "submit";
  disabled?: boolean;
  tone?: "primary" | "secondary" | "danger" | "ghost";
  title?: string;
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`workspace-button ${tone}`}
      title={title}
    >
      {children}
    </button>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Users, roles and scoped permissions
// ─────────────────────────────────────────────────────────────────────────────

type UserRow = {
  id: string;
  code: string;
  username: string;
  fullName: string;
  email?: string;
  status: string;
  accountType?: string;
  protectedAccount?: boolean;
  roles: string[];
  permissions?: string[];
};
type RoleRow = {
  id: string;
  code: string;
  name: string;
  permissions: string[];
  systemRole: boolean;
};
type PermissionDefinitionRow = {
  code: string;
  group: string;
  label: string;
  description: string;
  allowedScopes: string[];
  risk: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  legacy?: boolean;
};
type AccessProfileRow = {
  code: string;
  name: string;
  description: string;
  permissions: string[];
  recommendedScopes: string[];
  risk: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
};
type PermissionCatalog = {
  permissions: string[];
  groups: Record<string, string[]>;
  definitions: PermissionDefinitionRow[];
  profiles: AccessProfileRow[];
  defaultRoles: Record<string, string[]>;
};
type BulkGrantPreview = {
  affectedUsers: number;
  assignmentsToCreate: number;
  duplicateAssignments: number;
  criticalPermissions: string[];
  users: Array<{
    userId: string;
    fullName: string;
    addedPermissions: string[];
    alreadyAllowed: string[];
    deniedPermissions: string[];
    excludedByScope: string[];
  }>;
};
type PermissionGrantRow = {
  id: string;
  permissionCode: string;
  scopeType: string;
  scopeId?: string | null;
  effect: string;
  validFrom?: string | null;
  validUntil?: string | null;
};
type RoleAssignmentRow = {
  id: string;
  roleCode: string;
  scopeType: string;
  scopeId?: string | null;
  effect: string;
  validFrom?: string | null;
  validUntil?: string | null;
};
type AssignmentBundle = {
  permissionGrants: PermissionGrantRow[];
  roleAssignments: RoleAssignmentRow[];
};
type AuthorizationExplanation = {
  effective: {
    userId: string;
    scopeType: string;
    scopeId?: string | null;
    allowed: string[];
    denied: string[];
  };
  sources: Array<{
    permissionCode: string;
    sourceType: string;
    sourceId?: string | null;
    sourceLabel: string;
    effect: string;
    scopeType: string;
    scopeId?: string | null;
    active: boolean;
    applicable: boolean;
  }>;
};

function UserAccessConsole({ user }: { user: PortalUser }) {
  const canReadUsers = user.permissions.includes("users:read");
  const canReadRoles = user.permissions.some((permission) =>
    ["roles:read", "users:read"].includes(permission),
  );
  // The product uses three exclusive roles. Ad-hoc cross-role grants are disabled.
  const canGrant = false;
  const canRevoke = false;
  const { data, loading, error, refresh } = useLoad(async () => {
    const [users, roles, catalog, units, courses, exams] = await Promise.all([
      canReadUsers
        ? apiRequest<UserRow[] | { items: UserRow[] }>("/api/v1/users?size=1000")
        : Promise.resolve([] as UserRow[]),
      canReadRoles
        ? apiRequest<RoleRow[]>("/api/v1/roles")
        : Promise.resolve([] as RoleRow[]),
      apiRequest<PermissionCatalog>("/api/v1/authorization/catalog"),
      canGrant
        ? apiRequest<Row[]>("/api/v1/organization/units/tree").catch(() => [])
        : Promise.resolve([] as Row[]),
      canGrant
        ? apiRequest<Row[] | { items: Row[] }>("/api/v1/courses?size=500").catch(() => [])
        : Promise.resolve([] as Row[]),
      canGrant
        ? apiRequest<Row[] | { items: Row[] }>("/api/v1/exams?size=500").catch(() => [])
        : Promise.resolve([] as Row[]),
    ]);
    return {
      users: unwrapItems(users),
      roles,
      catalog,
      units,
      courses: unwrapItems(courses),
      exams: unwrapItems(exams),
    };
  }, [canReadUsers, canReadRoles, canGrant]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [tab, setTab] = useState<
    "accounts" | "grant" | "inspect" | "bulk"
  >("accounts");
  const [query, setQuery] = useState("");
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [create, setCreate] = useState({
    code: "",
    username: "",
    password: "",
    fullName: "",
    email: "",
    roleCode: "STUDENT",
  });
  const [grant, setGrant] = useState({
    kind: "ROLE",
    value: "STUDENT",
    scopeType: "SYSTEM",
    scopeId: "",
    effect: "ALLOW",
    validFrom: "",
    validUntil: "",
  });
  const [preview, setPreview] = useState<BulkGrantPreview | null>(null);
  const [inspectedUserId, setInspectedUserId] = useState("");
  const [assignments, setAssignments] = useState<AssignmentBundle | null>(null);
  const [explanation, setExplanation] = useState<AuthorizationExplanation | null>(null);
  const [inspectionLoading, setInspectionLoading] = useState(false);

  const users = data?.users ?? [];
  const roles = data?.roles ?? [];
  const catalog = data?.catalog;
  const scopeOptions = useMemo(() => {
    const flattenedUnits: Row[] = [];
    const visit = (items: Row[]) => {
      items.forEach((item) => {
        flattenedUnits.push(item);
        if (Array.isArray(item.children)) visit(item.children);
      });
    };
    visit(data?.units ?? []);
    const unitOptions = flattenedUnits.map((item) => ({
      id: String(item.id),
      type: String(item.type ?? "GROUP"),
      label: `${item.name ?? item.code ?? "Đơn vị"}${item.code ? ` · ${item.code}` : ""}`,
    }));
    const courseOptions = (data?.courses ?? []).map((item) => ({
      id: String(item.id),
      type: "COURSE",
      label: `${item.title ?? item.name ?? item.code ?? "Khóa học"}${item.code ? ` · ${item.code}` : ""}`,
    }));
    const examOptions = (data?.exams ?? []).map((item) => ({
      id: String(item.id),
      type: "EXAM",
      label: `${item.title ?? item.name ?? item.code ?? "Kỳ thi"}${item.code ? ` · ${item.code}` : ""}`,
    }));
    return [...unitOptions, ...courseOptions, ...examOptions];
  }, [data?.units, data?.courses, data?.exams]);
  const permissionByCode = useMemo(
    () => new Map((catalog?.definitions ?? []).map((item) => [item.code, item])),
    [catalog],
  );
  const profileByCode = useMemo(
    () => new Map((catalog?.profiles ?? []).map((item) => [item.code, item])),
    [catalog],
  );
  const availableRoleCodes = useMemo(
    () => ["ADMIN", "INSTRUCTOR", "STUDENT"].filter((code) =>
      roles.some((item) => item.code === code) ||
      (catalog?.profiles ?? []).some((item) => item.code === code),
    ),
    [catalog, roles],
  );
  const filtered = users.filter((item) =>
    `${item.code} ${item.username} ${item.fullName} ${item.email ?? ""}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );
  const inspectedUser = users.find((item) => item.id === inspectedUserId);
  const canWrite = user.permissions.some((value) =>
    ["users:create", "users:write", "users:bulk-manage"].includes(value),
  );
  const tabs: Array<{
    key: "accounts" | "grant" | "inspect" | "bulk";
    label: string;
  }> = [
    ...(canReadUsers ? [{ key: "accounts" as const, label: "Tài khoản" }] : []),
    ...(canWrite ? [{ key: "bulk" as const, label: "Nhập từ tệp" }] : []),
  ];

  useEffect(() => {
    if (!tabs.some((item) => item.key === tab) && tabs[0]) setTab(tabs[0].key);
  }, [tab, canReadUsers, canReadRoles, canGrant, canWrite]);

  function permissionLabel(code: string) {
    const definition = permissionByCode.get(code);
    return definition ? `${definition.label} (${code})` : code;
  }
  function roleLabel(code: string) {
    return profileByCode.get(code)?.name ?? roles.find((item) => item.code === code)?.name ?? code;
  }
  function riskTone(risk?: string) {
    if (risk === "CRITICAL") return "danger";
    if (risk === "HIGH") return "gold";
    if (risk === "MEDIUM") return "violet";
    return "teal";
  }
  function grantPayload() {
    return {
      roleCode: grant.kind === "ROLE" ? grant.value : null,
      permissionCode: grant.kind === "PERMISSION" ? grant.value : null,
      scopeType: grant.scopeType,
      scopeId: grant.scopeType === "SYSTEM" ? null : grant.scopeId,
      effect: grant.effect,
      validFrom: grant.validFrom ? new Date(grant.validFrom).toISOString() : null,
      validUntil: grant.validUntil ? new Date(grant.validUntil).toISOString() : null,
    };
  }

  async function createAccount(event: FormEvent) {
    event.preventDefault();
    if (!canWrite) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/users", {
        method: "POST",
        body: JSON.stringify({
          code: create.code,
          username: create.username,
          password: create.password,
          fullName: create.fullName,
          email: create.email || null,
          organizationUnitId: null,
          roleCodes: [create.roleCode],
        }),
      });
      setNotice({ tone: "success", message: `Đã tạo tài khoản ${create.username}.` });
      setCreate({
        code: "",
        username: "",
        password: "",
        fullName: "",
        email: "",
        roleCode: "STUDENT",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể tạo tài khoản",
      });
    } finally {
      setWorking(false);
    }
  }

  async function previewAccess(event: FormEvent) {
    event.preventDefault();
    if (!canGrant) return;
    if (!selected.size) {
      setNotice({ tone: "error", message: "Hãy chọn ít nhất một tài khoản." });
      return;
    }
    if (grant.scopeType !== "SYSTEM" && !grant.scopeId) {
      setNotice({ tone: "error", message: "Cần nhập ID của đối tượng phạm vi." });
      return;
    }
    if (grant.validFrom && grant.validUntil && grant.validFrom >= grant.validUntil) {
      setNotice({ tone: "error", message: "Thời điểm kết thúc phải sau thời điểm bắt đầu." });
      return;
    }
    setWorking(true);
    setNotice(null);
    try {
      const result = await apiRequest<BulkGrantPreview>(
        "/api/v1/authorization/grants/preview",
        {
          method: "POST",
          body: JSON.stringify({
            userIds: Array.from(selected),
            grants: [grantPayload()],
          }),
        },
      );
      setPreview(result);
      setNotice({ tone: "info", message: "Đã tính toán tác động. Hãy kiểm tra trước khi xác nhận." });
    } catch (cause) {
      setPreview(null);
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể xem trước quyền",
      });
    } finally {
      setWorking(false);
    }
  }

  async function grantAccess() {
    if (!canGrant || !preview) return;
    setWorking(true);
    setNotice(null);
    try {
      const result = await apiRequest<{ duplicateAssignments?: number }>(
        "/api/v1/authorization/grants/bulk",
        {
          method: "POST",
          body: JSON.stringify({
            operationId: createIdempotencyKey("grant"),
            userIds: Array.from(selected),
            grants: [grantPayload()],
          }),
        },
      );
      setNotice({
        tone: "success",
        message: `Đã áp dụng cho ${selected.size} tài khoản${result.duplicateAssignments ? `; bỏ qua ${result.duplicateAssignments} lần cấp trùng` : ""}.`,
      });
      setPreview(null);
      setSelected(new Set());
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể cấp quyền",
      });
    } finally {
      setWorking(false);
    }
  }

  async function loadInspection(userId: string, openTab = true) {
    if (!userId) return;
    setInspectionLoading(true);
    setInspectedUserId(userId);
    if (openTab) setTab("inspect");
    setNotice(null);
    try {
      const [bundle, explained] = await Promise.all([
        apiRequest<AssignmentBundle>(`/api/v1/authorization/users/${userId}/assignments`),
        apiRequest<AuthorizationExplanation>(
          `/api/v1/authorization/explain?userId=${encodeURIComponent(userId)}&scopeType=SYSTEM`,
        ),
      ]);
      setAssignments(bundle);
      setExplanation(explained);
    } catch (cause) {
      setAssignments(null);
      setExplanation(null);
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể kiểm tra quyền",
      });
    } finally {
      setInspectionLoading(false);
    }
  }

  async function revokeAssignment(kind: "permission" | "role", id: string) {
    if (!canRevoke) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/authorization/grants/bulk", {
        method: "DELETE",
        body: JSON.stringify({
          operationId: createIdempotencyKey("revoke"),
          grantIds: kind === "permission" ? [id] : [],
          roleAssignmentIds: kind === "role" ? [id] : [],
        }),
      });
      setNotice({ tone: "success", message: "Đã thu hồi lần cấp quyền đã chọn." });
      await loadInspection(inspectedUserId, false);
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể thu hồi quyền",
      });
    } finally {
      setWorking(false);
    }
  }


  return (
    <>
      <WorkspaceHero
        eyebrow="QUẢN LÝ NGƯỜI DÙNG"
        title="Tài khoản và vai trò"
        description="Mỗi tài khoản có đúng một vai trò: Quản trị viên, Giảng viên hoặc Học viên. Muốn dùng chức năng của vai trò khác phải đăng nhập bằng tài khoản tương ứng."
        icon={<Icon name="users" size={31} />}
        stats={[
          { value: users.length, label: "Tài khoản" },
          { value: 3, label: "Vai trò cố định" },
          { value: users.filter((item) => item.status === "ACTIVE").length, label: "Đang hoạt động", tone: "violet" },
        ]}
        actions={
          <>
            <Button tone="secondary" onClick={() => void refresh()}>
              <Icon name="refresh" size={16} /> Đồng bộ
            </Button>
            {canWrite && (
              <Button onClick={() => setTab("accounts")}>
                <Icon name="plus" size={16} /> Tạo tài khoản
              </Button>
            )}
          </>
        }
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      <nav className="workspace-tabs">
        {tabs.map(({ key, label }) => (
          <button key={key} className={tab === key ? "active" : ""} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </nav>
      {loading ? (
        <Busy />
      ) : error ? (
        <NoticeBar notice={{ tone: "error", message: error }} onClose={() => void refresh()} />
      ) : (
        <>
          {tab === "accounts" && (
            <div className="workspace-two-column wide-left">
              <Panel
                title="Danh sách tài khoản"
                subtitle="Vai trò được hiển thị trực tiếp. Tài khoản quản trị gốc được bảo vệ khỏi các thao tác thông thường."
                action={
                  <div className="workspace-search">
                    <Icon name="search" size={16} />
                    <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm tên, mã, tài khoản…" />
                  </div>
                }
              >
                <div className="workspace-table-wrap">
                  <table className="workspace-table">
                    <thead>
                      <tr>
                        <th>Danh tính</th>
                        <th>Vai trò</th>
                        <th>Bảo vệ</th>
                        <th>Trạng thái</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filtered.map((item) => (
                        <tr key={item.id}>
                          <td>
                            <div className="workspace-primary-cell">
                              <span className="mini-avatar">{item.fullName.slice(0, 1).toUpperCase()}</span>
                              <div>
                                <strong>{item.fullName}</strong>
                                <small>
                                  {item.code} · @{item.username}
                                  {item.email ? ` · ${item.email}` : ""}
                                </small>
                              </div>
                            </div>
                          </td>
                          <td>
                            <div className="workspace-tags">
                              {item.roles.map((value) => (
                                <Tag key={value} tone={riskTone(profileByCode.get(value)?.risk)}>
                                  {roleLabel(value)}
                                </Tag>
                              ))}
                            </div>
                          </td>
                          <td>{item.protectedAccount ? <Tag tone="gold">Tài khoản gốc</Tag> : <Tag>Tiêu chuẩn</Tag>}</td>
                          <td><Tag tone={item.status === "ACTIVE" ? "teal" : "danger"}>{item.status}</Tag></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
              {canWrite && (
                <Panel title="Tạo tài khoản" subtitle="Không có đăng ký công khai. Quản trị tạo tài khoản và chọn đúng một vai trò." className="sticky-panel">
                  <form className="workspace-form" onSubmit={createAccount}>
                    <Field label="Mã người dùng">
                      <input required maxLength={80} value={create.code} onChange={(event) => setCreate({ ...create, code: event.target.value.toUpperCase() })} placeholder="NV001" />
                    </Field>
                    <Field label="Tên đăng nhập">
                      <input required minLength={3} value={create.username} onChange={(event) => setCreate({ ...create, username: event.target.value })} placeholder="nguyen.an" />
                    </Field>
                    <Field label="Họ và tên" wide>
                      <input required value={create.fullName} onChange={(event) => setCreate({ ...create, fullName: event.target.value })} />
                    </Field>
                    <Field label="Email" wide>
                      <input type="email" value={create.email} onChange={(event) => setCreate({ ...create, email: event.target.value })} />
                    </Field>
                    <Field label="Mật khẩu tạm" wide hint="Tối thiểu 12 ký tự; người dùng đổi ở lần đăng nhập đầu.">
                      <input required minLength={12} type="password" value={create.password} onChange={(event) => setCreate({ ...create, password: event.target.value })} />
                    </Field>
                    <Field label="Vai trò" wide hint="Một tài khoản chỉ có một vai trò và không được ghép chức năng chéo.">
                      <select value={create.roleCode} onChange={(event) => setCreate({ ...create, roleCode: event.target.value })}>
                        {availableRoleCodes.map((code) => <option key={code} value={code}>{roleLabel(code)}</option>)}
                      </select>
                    </Field>
                    <Button type="submit" disabled={working || !canWrite}>
                      <Icon name="plus" size={16} /> {working ? "Đang tạo…" : "Tạo tài khoản"}
                    </Button>
                  </form>
                </Panel>
              )}
            </div>
          )}

          {tab === "grant" && (
            <div className="workspace-two-column">
              <Panel title={`Tài khoản nhận quyền (${selected.size})`} subtitle="Chọn nhiều tài khoản ở thẻ Tài khoản. Một lần xác nhận có thể áp dụng cho tối đa 1.000 người.">
                {selected.size ? (
                  <div className="selection-cloud">
                    {users.filter((item) => selected.has(item.id)).map((item) => (
                      <button key={item.id} onClick={() => setSelected((current) => {
                        const next = new Set(current);
                        next.delete(item.id);
                        setPreview(null);
                        return next;
                      })}>
                        <span>{item.fullName}</span><Icon name="close" size={13} />
                      </button>
                    ))}
                  </div>
                ) : <Empty text="Chưa chọn tài khoản. Quay lại thẻ Tài khoản để chọn người nhận quyền." />}
                <div className="role-card-grid">
                  {(catalog?.profiles ?? []).map((profile) => (
                    <button
                      type="button"
                      className="role-card"
                      key={profile.code}
                      onClick={() => {
                        const scopeType = profile.recommendedScopes.includes("SYSTEM") ? "SYSTEM" : (profile.recommendedScopes[0] ?? "SYSTEM");
                        setGrant({ ...grant, kind: "ROLE", value: profile.code, scopeType, scopeId: "" });
                        setPreview(null);
                      }}
                    >
                      <div>
                        <span className="role-symbol">{profile.name.slice(0, 1)}</span>
                        <div><strong>{profile.name}</strong><small>{profile.code}</small></div>
                        <Tag tone={riskTone(profile.risk)}>{profile.risk}</Tag>
                      </div>
                      <p>{profile.description}</p>
                    </button>
                  ))}
                </div>
              </Panel>
              <div>
                <Panel title="Thiết lập lần cấp" subtitle="Chọn một gói công việc, phạm vi áp dụng và thời hạn nếu cần.">
                  <form className="workspace-form" onSubmit={previewAccess}>
                    <Field label="Gói công việc">
                      <select value={grant.value} onChange={(event) => { setGrant({ ...grant, kind: "ROLE", value: event.target.value }); setPreview(null); }}>
                        {availableRoleCodes.map((code) => <option key={code} value={code}>{roleLabel(code)}</option>)}
                      </select>
                    </Field>
                    <Field label="Phạm vi">
                      <select value={grant.scopeType} onChange={(event) => { setGrant({ ...grant, scopeType: event.target.value, scopeId: event.target.value === "SYSTEM" ? "" : grant.scopeId }); setPreview(null); }}>
                        <option value="SYSTEM">Toàn hệ thống</option>
                        <option value="BRANCH">Chi nhánh</option>
                        <option value="DEPARTMENT">Phòng ban</option>
                        <option value="GROUP">Nhóm</option>
                        <option value="COURSE">Khóa học</option>
                        <option value="EXAM">Kỳ thi</option>
                      </select>
                    </Field>
                    {grant.scopeType !== "SYSTEM" && (
                      <Field label="Đối tượng áp dụng" wide hint="Tìm theo tên; mã kỹ thuật được hệ thống tự xử lý.">
                        <select
                          required
                          value={grant.scopeId}
                          onChange={(event) => {
                            setGrant({ ...grant, scopeId: event.target.value });
                            setPreview(null);
                          }}
                        >
                          <option value="">Chọn đối tượng…</option>
                          {scopeOptions
                            .filter((option) => option.type === grant.scopeType)
                            .map((option) => (
                              <option key={option.id} value={option.id}>{option.label}</option>
                            ))}
                        </select>
                      </Field>
                    )}
                    <Field label="Bắt đầu" hint="Để trống để có hiệu lực ngay.">
                      <input type="datetime-local" value={grant.validFrom} onChange={(event) => { setGrant({ ...grant, validFrom: event.target.value }); setPreview(null); }} />
                    </Field>
                    <Field label="Kết thúc" hint="Để trống nếu không hết hạn.">
                      <input type="datetime-local" value={grant.validUntil} onChange={(event) => { setGrant({ ...grant, validUntil: event.target.value }); setPreview(null); }} />
                    </Field>
                    <Button type="submit" disabled={working || selected.size === 0}>
                      <Icon name="search" size={16} /> {working ? "Đang tính…" : "Xem trước tác động"}
                    </Button>
                  </form>
                </Panel>
                {preview && (
                  <Panel title="Kết quả xem trước" subtitle="Chưa có thay đổi nào được ghi cho tới khi bấm xác nhận.">
                    <div className="workspace-tags">
                      <Tag tone="teal">{preview.assignmentsToCreate} lần cấp mới</Tag>
                      <Tag>{preview.duplicateAssignments} lần cấp trùng</Tag>
                      <Tag tone={preview.criticalPermissions.length ? "danger" : "teal"}>{preview.criticalPermissions.length} quyền cực nhạy cảm</Tag>
                    </div>
                    {preview.criticalPermissions.length > 0 && (
                      <div className="workspace-callout">
                        <Icon name="warning" size={18} />
                        <p>Thao tác chứa quyền mức CRITICAL: {preview.criticalPermissions.map(permissionLabel).join(", ")}.</p>
                      </div>
                    )}
                    <div className="workspace-table-wrap">
                      <table className="workspace-table">
                        <thead><tr><th>Người dùng</th><th>Quyền mới</th><th>Đã có</th><th>Bị từ chối</th><th>Không áp dụng ở phạm vi</th></tr></thead>
                        <tbody>
                          {preview.users.map((item) => (
                            <tr key={item.userId}>
                              <td><strong>{item.fullName}</strong></td>
                              <td>{item.addedPermissions.length}</td>
                              <td>{item.alreadyAllowed.length}</td>
                              <td>{item.deniedPermissions.length}</td>
                              <td>{item.excludedByScope.length}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <Button onClick={() => void grantAccess()} disabled={working || preview.assignmentsToCreate === 0}>
                      <Icon name="lock" size={16} /> {working ? "Đang áp dụng…" : "Xác nhận cấp quyền"}
                    </Button>
                  </Panel>
                )}
              </div>
            </div>
          )}

          {tab === "inspect" && (
            <div className="workspace-two-column wide-left">
              <Panel title="Chọn người cần kiểm tra" subtitle="Hiển thị gói quyền, quyền đơn lẻ và kết quả hiệu lực ở phạm vi SYSTEM.">
                <div className="workspace-search">
                  <Icon name="search" size={16} />
                  <select value={inspectedUserId} onChange={(event) => void loadInspection(event.target.value, false)}>
                    <option value="">Chọn tài khoản…</option>
                    {users.map((item) => <option key={item.id} value={item.id}>{item.fullName} · {item.code}</option>)}
                  </select>
                </div>
                {inspectionLoading ? <Busy label="Đang phân tích nguồn quyền…" /> : !inspectedUser ? <Empty text="Chọn một tài khoản để xem quyền hiệu lực và nguồn phát sinh." /> : (
                  <>
                    <div className="workspace-primary-cell">
                      <span className="mini-avatar">{inspectedUser.fullName.slice(0, 1).toUpperCase()}</span>
                      <div><strong>{inspectedUser.fullName}</strong><small>{inspectedUser.code} · @{inspectedUser.username}</small></div>
                    </div>
                    <div className="workspace-tags">
                      {(explanation?.effective.allowed ?? []).map((code) => <Tag key={code} tone="teal">{permissionByCode.get(code)?.label ?? code}</Tag>)}
                    </div>
                    {(explanation?.effective.denied.length ?? 0) > 0 && (
                      <div className="workspace-callout"><Icon name="warning" size={18} /><p>Quyền bị từ chối ưu tiên: {explanation?.effective.denied.map(permissionLabel).join(", ")}</p></div>
                    )}
                  </>
                )}
              </Panel>
              <div>
                <Panel title="Các lần cấp đang lưu" subtitle="Thu hồi đúng bản ghi, không ảnh hưởng những nguồn quyền khác.">
                  {!assignments || (!assignments.roleAssignments.length && !assignments.permissionGrants.length) ? <Empty text="Tài khoản chưa có lần cấp theo phạm vi." /> : (
                    <div className="workspace-table-wrap">
                      <table className="workspace-table">
                        <thead><tr><th>Nguồn</th><th>Phạm vi</th><th>Hiệu lực</th><th></th></tr></thead>
                        <tbody>
                          {assignments.roleAssignments.map((item) => (
                            <tr key={item.id}>
                              <td><strong>{roleLabel(item.roleCode)}</strong><small className="block">Gói quyền · {item.roleCode}</small></td>
                              <td>{item.scopeType}{item.scopeId ? ` · ${item.scopeId}` : ""}</td>
                              <td><Tag tone={item.effect === "DENY" ? "danger" : "teal"}>{item.effect}</Tag></td>
                              <td>{canRevoke && <Button tone="secondary" disabled={working} onClick={() => void revokeAssignment("role", item.id)}>Thu hồi</Button>}</td>
                            </tr>
                          ))}
                          {assignments.permissionGrants.map((item) => (
                            <tr key={item.id}>
                              <td><strong>{permissionByCode.get(item.permissionCode)?.label ?? item.permissionCode}</strong><small className="block">Quyền đơn lẻ · {item.permissionCode}</small></td>
                              <td>{item.scopeType}{item.scopeId ? ` · ${item.scopeId}` : ""}</td>
                              <td><Tag tone={item.effect === "DENY" ? "danger" : "teal"}>{item.effect}</Tag></td>
                              <td>{canRevoke && <Button tone="secondary" disabled={working} onClick={() => void revokeAssignment("permission", item.id)}>Thu hồi</Button>}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </Panel>
                <Panel title="Giải thích nguồn quyền" subtitle="Mỗi quyền cho biết đến từ gói nào hoặc lần cấp trực tiếp nào.">
                  {!explanation?.sources.length ? <Empty text="Không có nguồn quyền áp dụng ở phạm vi SYSTEM." /> : (
                    <div className="workspace-table-wrap">
                      <table className="workspace-table">
                        <thead><tr><th>Quyền</th><th>Nguồn</th><th>Kết quả</th></tr></thead>
                        <tbody>
                          {explanation.sources.map((source, index) => (
                            <tr key={`${source.permissionCode}-${source.sourceId ?? index}`}>
                              <td>{permissionByCode.get(source.permissionCode)?.label ?? source.permissionCode}<small className="block">{source.permissionCode}</small></td>
                              <td>{source.sourceLabel}<small className="block">{source.sourceType} · {source.scopeType}</small></td>
                              <td><Tag tone={!source.active || !source.applicable ? "" : source.effect === "DENY" ? "danger" : "teal"}>{!source.active ? "Hết hiệu lực" : !source.applicable ? "Ngoài phạm vi" : source.effect}</Tag></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </Panel>
              </div>
            </div>
          )}


          {tab === "bulk" && (
            <Panel
              title="Nhập tài khoản từ tệp"
              subtitle="Dùng trình nhập có hướng dẫn để chọn tệp, ánh xạ từng cột, xem trước lỗi và xác nhận."
            >
              <div className="workspace-import-actions">
                <Link className="workspace-button primary" href="/admin/users/import">
                  <Icon name="upload" size={16} /> Mở trình nhập CSV/XLSX
                </Link>
                <span>Không cần dán dữ liệu nhiều người vào một ô văn bản.</span>
              </div>
            </Panel>
          )}
        </>
      )}
    </>
  );
}


// ─────────────────────────────────────────────────────────────────────────────
// Organization
// ─────────────────────────────────────────────────────────────────────────────

type UnitRow = {
  id: string;
  code: string;
  name: string;
  type: string;
  parentId?: string;
  status: string;
  sortOrder: number;
  path: string;
  children?: UnitRow[];
};
type MembershipRow = {
  id: string;
  userId: string;
  unitId: string;
  membershipType: string;
  primaryMembership: boolean;
  active: boolean;
  createdAt: string;
};

function unitTypeLabel(type: string) {
  return (
    {
      ORGANIZATION: "Tổ chức",
      BRANCH: "Chi nhánh",
      DIVISION: "Khối",
      DEPARTMENT: "Phòng ban",
      TEAM: "Đội nhóm",
      GROUP: "Nhóm",
    }[type] ?? type
  );
}

function OrganizationConsole({ user }: { user: PortalUser }) {
  const canManageUnits =
    user.permissions.some((permission) =>
      ["organization:manage", "organization:write"].includes(permission),
    );
  const canManageMembers =
    canManageUnits || user.permissions.includes("organization:membership:manage");
  const canReadUsers = user.permissions.includes("users:read");
  const { data, loading, error, refresh } = useLoad(async () => {
    const [units, flat, users] = await Promise.all([
      apiRequest<UnitRow[]>("/api/v1/organization/units/tree"),
      apiRequest<UnitRow[]>("/api/v1/organization/units"),
      canReadUsers
        ? apiRequest<UserRow[] | { items: UserRow[] }>("/api/v1/users?size=1000")
            .then(unwrapItems)
            .catch(() => [] as UserRow[])
        : Promise.resolve([] as UserRow[]),
    ]);
    return { units, flat, users };
  }, [canReadUsers]);
  const [selectedUnit, setSelectedUnitState] = useState<string>("");
  const [members, setMembers] = useState<MembershipRow[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [memberQuery, setMemberQuery] = useState("");
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());
  const [unit, setUnit] = useState({
    code: "",
    name: "",
    type: "DEPARTMENT",
    parentId: "",
  });
  const [membership, setMembership] = useState({
    membershipType: "MEMBER",
    primaryMembership: false,
  });
  const [working, setWorking] = useState(false);
  const flat = data?.flat ?? [];
  const users = data?.users ?? [];
  const usersById = useMemo(
    () => new Map(users.map((item) => [item.id, item])),
    [users],
  );
  const currentUnit = flat.find((item) => item.id === selectedUnit);
  const filteredUsers = useMemo(() => {
    const query = memberQuery.trim().toLocaleLowerCase("vi-VN");
    if (!query) return users;
    return users.filter((item) =>
      `${item.fullName} ${item.username} ${item.email ?? ""} ${item.code}`
        .toLocaleLowerCase("vi-VN")
        .includes(query),
    );
  }, [memberQuery, users]);

  const setSelectedUnit = useCallback((id: string) => {
    setSelectedUnitState(id);
    window.localStorage.setItem("lmspilot-organization-unit", id);
  }, []);

  useEffect(() => {
    if (!flat.length) return;
    const remembered = window.localStorage.getItem("lmspilot-organization-unit");
    const next =
      (remembered && flat.some((item) => item.id === remembered) && remembered) ||
      (selectedUnit && flat.some((item) => item.id === selectedUnit) && selectedUnit) ||
      flat[0].id;
    if (next !== selectedUnit) setSelectedUnitState(next);
  }, [flat, selectedUnit]);

  useEffect(() => {
    if (!selectedUnit) {
      setMembers([]);
      return;
    }
    void apiRequest<MembershipRow[]>(
      `/api/v1/organization/memberships?unitId=${selectedUnit}`,
    )
      .then(setMembers)
      .catch(() => setMembers([]));
  }, [selectedUnit]);

  async function createUnit(event: FormEvent) {
    event.preventDefault();
    if (!canManageUnits) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/organization/units", {
        method: "POST",
        body: JSON.stringify({
          code: unit.code,
          name: unit.name,
          type: unit.type,
          parentId: unit.parentId || null,
          status: "ACTIVE",
          sortOrder: 0,
        }),
      });
      setUnit({ code: "", name: "", type: "DEPARTMENT", parentId: "" });
      setNotice({ tone: "success", message: "Đã tạo đơn vị mới." });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể tạo đơn vị",
      });
    } finally {
      setWorking(false);
    }
  }

  async function grantMembers(event: FormEvent) {
    event.preventDefault();
    if (!canManageMembers) return;
    const ids = Array.from(selectedUsers);
    if (!selectedUnit || !ids.length) {
      setNotice({
        tone: "error",
        message: "Chọn đơn vị và ít nhất một người dùng.",
      });
      return;
    }
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/organization/memberships/bulk", {
        method: "POST",
        body: JSON.stringify({
          memberships: ids.map((userId) => ({
            userId,
            unitId: selectedUnit,
            membershipType: membership.membershipType,
            primaryMembership: membership.primaryMembership,
          })),
        }),
      });
      setNotice({
        tone: "success",
        message: `Đã gán ${ids.length} người vào ${currentUnit?.name ?? "đơn vị"}.`,
      });
      setSelectedUsers(new Set());
      setMembers(
        await apiRequest<MembershipRow[]>(
          `/api/v1/organization/memberships?unitId=${selectedUnit}`,
        ),
      );
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể gán thành viên",
      });
    } finally {
      setWorking(false);
    }
  }

  const countTree = (items: UnitRow[]): number =>
    items.reduce((sum, item) => sum + 1 + countTree(item.children ?? []), 0);

  return (
    <>
      <WorkspaceHero
        eyebrow="TỔ CHỨC"
        title="Cơ cấu và thành viên"
        description="Quản lý cây tổ chức, đơn vị trực thuộc và thành viên trong một không gian làm việc gọn hơn."
        icon={<Icon name="building" size={29} />}
        stats={[
          { value: countTree(data?.units ?? []), label: "Đơn vị" },
          { value: members.length, label: "Thành viên", tone: "teal" },
        ]}
        actions={
          <Button tone="secondary" onClick={() => void refresh()}>
            <Icon name="refresh" size={16} /> Làm mới
          </Button>
        }
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      {loading ? (
        <Busy />
      ) : error ? (
        <NoticeBar
          notice={{ tone: "error", message: error }}
          onClose={() => void refresh()}
        />
      ) : (
        <div className="org-layout">
          <Panel
            title="Cây tổ chức"
            subtitle={`${flat.length} đơn vị`}
            className="org-tree-panel"
            action={
              canManageUnits ? (
                <button
                  type="button"
                  className="workspace-button ghost"
                  onClick={() =>
                    document.getElementById("create-organization-unit")?.scrollIntoView({
                      behavior: "smooth",
                    })
                  }
                >
                  <Icon name="plus" size={15} /> Thêm
                </button>
              ) : undefined
            }
          >
            <OrganizationTree
              units={data?.units ?? []}
              selected={selectedUnit}
              onSelect={setSelectedUnit}
            />
          </Panel>

          <div className="org-main-column">
            <Panel
              title={currentUnit?.name ?? "Thành viên trong đơn vị"}
              subtitle={
                currentUnit
                  ? `${unitTypeLabel(currentUnit.type)} · ${currentUnit.code}`
                  : "Chọn một đơn vị trong cây"
              }
              action={currentUnit ? <Tag tone="teal">{members.length} người</Tag> : undefined}
            >
              {members.length ? (
                <div className="member-list">
                  {members.map((item) => {
                    const member = usersById.get(item.userId);
                    return (
                      <div key={item.id}>
                        <span className="mini-avatar">
                          {(member?.fullName || member?.username || "N").slice(0, 1).toUpperCase()}
                        </span>
                        <div>
                          <strong>{member?.fullName || member?.username || item.userId}</strong>
                          <small>
                            {member?.email || item.membershipType}
                            {item.primaryMembership ? " · Đơn vị chính" : ""}
                          </small>
                        </div>
                        <Tag tone={item.active ? "teal" : "danger"}>
                          {item.active ? "Đang hoạt động" : "Ngừng hoạt động"}
                        </Tag>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <Empty
                  text={
                    selectedUnit
                      ? "Đơn vị chưa có thành viên trực tiếp."
                      : "Chọn một đơn vị để bắt đầu."
                  }
                />
              )}
            </Panel>

            {canManageMembers && (
              <Panel
                title="Thêm thành viên"
                subtitle="Chọn trực tiếp từ danh sách tài khoản, không cần nhập mã người dùng."
              >
                {canReadUsers ? (
                  <form className="workspace-form" onSubmit={grantMembers}>
                    <Field label="Tìm người dùng" wide>
                      <input
                        value={memberQuery}
                        onChange={(event) => setMemberQuery(event.target.value)}
                        placeholder="Tên, tài khoản hoặc email"
                      />
                    </Field>
                    <div className="member-picker wide">
                      {filteredUsers.slice(0, 120).map((item) => (
                        <label key={item.id}>
                          <input
                            type="checkbox"
                            checked={selectedUsers.has(item.id)}
                            onChange={(event) => {
                              const next = new Set(selectedUsers);
                              if (event.target.checked) next.add(item.id);
                              else next.delete(item.id);
                              setSelectedUsers(next);
                            }}
                          />
                          <span className="mini-avatar">
                            {(item.fullName || item.username).slice(0, 1).toUpperCase()}
                          </span>
                          <span>
                            <strong>{item.fullName || item.username}</strong>
                            <small>{item.email || item.username}</small>
                          </span>
                        </label>
                      ))}
                      {!filteredUsers.length && <Empty text="Không tìm thấy người dùng phù hợp." />}
                    </div>
                    <Field label="Vai trò trong đơn vị">
                      <select
                        value={membership.membershipType}
                        onChange={(event) =>
                          setMembership({ ...membership, membershipType: event.target.value })
                        }
                      >
                        <option value="MEMBER">Thành viên</option>
                        <option value="MANAGER">Quản lý đơn vị</option>
                        <option value="INSTRUCTOR">Phụ trách đào tạo</option>
                        <option value="LEARNER">Người học</option>
                      </select>
                    </Field>
                    <label className="workspace-check">
                      <input
                        type="checkbox"
                        checked={membership.primaryMembership}
                        onChange={(event) =>
                          setMembership({
                            ...membership,
                            primaryMembership: event.target.checked,
                          })
                        }
                      />
                      <span>Đặt làm đơn vị chính</span>
                    </label>
                    <Button
                      type="submit"
                      disabled={working || !selectedUnit || selectedUsers.size === 0}
                    >
                      <Icon name="users" size={16} />
                      Gán {selectedUsers.size ? `${selectedUsers.size} người` : "thành viên"}
                    </Button>
                  </form>
                ) : (
                  <Empty text="Cần quyền đọc người dùng để chọn thành viên từ danh sách." />
                )}
              </Panel>
            )}

            {canManageUnits && (
              <Panel
                title="Tạo đơn vị"
                subtitle="Chỉ giữ các trường cần thiết; thông tin ít dùng được đặt cuối biểu mẫu."
                className="compact-form-panel"
              >
                <form className="workspace-form" onSubmit={createUnit} id="create-organization-unit">
                  <Field label="Tên đơn vị" wide>
                    <input
                      required
                      value={unit.name}
                      onChange={(event) => setUnit({ ...unit, name: event.target.value })}
                      placeholder="Ví dụ: Phòng Phát triển"
                    />
                  </Field>
                  <Field label="Mã">
                    <input
                      required
                      value={unit.code}
                      onChange={(event) =>
                        setUnit({ ...unit, code: event.target.value.toUpperCase() })
                      }
                      placeholder="DEV"
                    />
                  </Field>
                  <Field label="Loại">
                    <select
                      value={unit.type}
                      onChange={(event) => setUnit({ ...unit, type: event.target.value })}
                    >
                      {["ORGANIZATION", "BRANCH", "DIVISION", "DEPARTMENT", "TEAM", "GROUP"].map(
                        (value) => (
                          <option value={value} key={value}>
                            {unitTypeLabel(value)}
                          </option>
                        ),
                      )}
                    </select>
                  </Field>
                  <Field label="Đơn vị cha" wide>
                    <select
                      value={unit.parentId}
                      onChange={(event) => setUnit({ ...unit, parentId: event.target.value })}
                    >
                      <option value="">Cấp gốc</option>
                      {flat.map((item) => (
                        <option value={item.id} key={item.id}>
                          {item.name}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Button type="submit" disabled={working}>
                    <Icon name="plus" size={16} /> Tạo đơn vị
                  </Button>
                </form>
              </Panel>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function OrganizationTree({
  units,
  selected,
  onSelect,
  level = 0,
}: {
  units: UnitRow[];
  selected: string;
  onSelect: (id: string) => void;
  level?: number;
}) {
  return (
    <div className={`org-tree level-${level}`}>
      {units.map((item) => (
        <div key={item.id} className="org-node-wrap">
          <button
            type="button"
            className={`org-node ${selected === item.id ? "active" : ""}`}
            onClick={() => onSelect(item.id)}
          >
            <span className={`org-type ${item.type.toLowerCase()}`}>
              {item.type === "BRANCH"
                ? "⌘"
                : item.type === "DEPARTMENT"
                  ? "◇"
                  : item.type === "GROUP"
                    ? "✦"
                    : "⬡"}
            </span>
            <strong className="org-node-name">{item.name}</strong>
            <span className="org-node-side">
              <span className="org-node-kind">{unitTypeLabel(item.type)}</span>
              <span
                className={`org-status-dot ${item.status === "ACTIVE" ? "" : "inactive"}`}
                title={item.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng hoạt động"}
              />
            </span>
          </button>
          {item.children?.length ? (
            <OrganizationTree
              units={item.children}
              selected={selected}
              onSelect={onSelect}
              level={level + 1}
            />
          ) : null}
        </div>
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Branding and external services
// ─────────────────────────────────────────────────────────────────────────────

type BrandingRow = {
  systemName: string;
  themeKey: ThemeKey;
  introduction?: string;
  logoFileId?: string;
  faviconFileId?: string;
  backgroundFileId?: string;
  logoUrl?: string;
  faviconUrl?: string;
  backgroundUrl?: string;
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
  customDomain?: string;
  updatedAt: string;
};

type ServiceType =
  | "REDIS"
  | "SMTP"
  | "AI_PROVIDER"
  | "OBJECT_STORAGE"
  | "DOCUMENT_EDITOR"
  | "VIDEO_CONFERENCE";

type ServiceRow = {
  id: string;
  serviceType: ServiceType;
  configKey: string;
  enabled: boolean;
  config: Record<string, any>;
  secretConfigured: boolean;
  healthStatus: string;
  lastCheckedAt?: string;
  lastError?: string;
  updatedAt: string;
};

type ServiceDraft = {
  id?: string;
  serviceType: ServiceType;
  configKey: string;
  enabled: boolean;
  host: string;
  port: string;
  username: string;
  database: string;
  endpoint: string;
  callbackUrl: string;
  model: string;
  bucket: string;
  region: string;
  accessKey: string;
  fromEmail: string;
  fromName: string;
  security: string;
  provider: string;
  pathStyle: boolean;
  secure: boolean;
  secret: string;
  secretConfigured: boolean;
};

const BRAND_COLORS = [
  { name: "Tím", primary: "#5B4BDB", secondary: "#7C6DF2" },
  { name: "Xanh dương", primary: "#2563EB", secondary: "#60A5FA" },
  { name: "Xanh ngọc", primary: "#0F9F8F", secondary: "#34D399" },
  { name: "Xanh lá", primary: "#16A34A", secondary: "#4ADE80" },
  { name: "Cam", primary: "#F97316", secondary: "#FDBA74" },
  { name: "Hồng", primary: "#E84A6F", secondary: "#FB7185" },
];

const SERVICE_META: Record<
  ServiceType,
  { label: string; short: string; icon: string; docs: string; secretLabel: string }
> = {
  REDIS: {
    label: "Redis",
    short: "Bộ nhớ đệm và phiên",
    icon: "R",
    docs: "https://redis.io/docs/latest/develop/clients/",
    secretLabel: "Mật khẩu Redis",
  },
  SMTP: {
    label: "Email SMTP",
    short: "Gửi thông báo hệ thống",
    icon: "@",
    docs: "https://www.rfc-editor.org/rfc/rfc8314",
    secretLabel: "Mật khẩu ứng dụng",
  },
  AI_PROVIDER: {
    label: "Dịch vụ AI",
    short: "API tương thích OpenAI",
    icon: "AI",
    docs: "https://platform.openai.com/docs/api-reference/introduction",
    secretLabel: "API key",
  },
  OBJECT_STORAGE: {
    label: "Lưu trữ S3",
    short: "Tệp và nội dung học tập",
    icon: "S3",
    docs: "https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html",
    secretLabel: "Secret access key",
  },
  DOCUMENT_EDITOR: {
    label: "ONLYOFFICE Docs",
    short: "Chỉnh sửa tài liệu trực tuyến",
    icon: "D",
    docs: "https://api.onlyoffice.com/docs/docs-api/get-started/basic-concepts/",
    secretLabel: "JWT secret",
  },
  VIDEO_CONFERENCE: {
    label: "Họp trực tuyến",
    short: "Họp và đào tạo trực tuyến",
    icon: "V",
    docs: "https://jitsi.github.io/handbook/docs/dev-guide/dev-guide-iframe/",
    secretLabel: "API secret hoặc token",
  },
};

function defaultServiceDraft(serviceType: ServiceType = "REDIS"): ServiceDraft {
  return {
    serviceType,
    configKey: "default",
    enabled: true,
    host: serviceType === "REDIS" ? "redis" : "",
    port: serviceType === "REDIS" ? "6379" : serviceType === "SMTP" ? "587" : "",
    username: "",
    database: "0",
    endpoint: "",
    callbackUrl: "",
    model: "",
    bucket: "",
    region: "",
    accessKey: "",
    fromEmail: "",
    fromName: "",
    security: "STARTTLS",
    provider: "JITSI",
    pathStyle: false,
    secure: true,
    secret: "",
    secretConfigured: false,
  };
}

function browserBrandAsset(path?: string) {
  if (!path) return "";
  if (/^https?:\/\//i.test(path)) return path;
  return `/api/gateway/${path.replace(/^\/+/, "")}`;
}

function applyBrandingPreview(branding: BrandingRow) {
  document.body.style.setProperty("--brand-primary", branding.primaryColor);
  document.body.style.setProperty("--brand-secondary", branding.secondaryColor);
  document.body.style.setProperty("--brand-on-primary", readableText(branding.primaryColor));
}

function serviceStatusLabel(status: string) {
  const labels: Record<string, string> = {
    HEALTHY: "Hoạt động tốt",
    DEGRADED: "Cần kiểm tra",
    UNREACHABLE: "Không thể kết nối",
    MISCONFIGURED: "Sai cấu hình",
    UNKNOWN: "Chưa kiểm tra",
  };
  return labels[status] ?? status;
}

function WorldSettingsConsole({ user }: { user: PortalUser }) {
  const router = useRouter();
  const canBrand = user.permissions.includes("branding:manage");
  const canServices =
    user.permissions.some((permission) =>
      ["configuration:manage", "integrations:manage"].includes(permission),
    );
  const { data, loading, error, refresh } = useLoad(
    async () => ({
      branding: canBrand ? await apiRequest<BrandingRow>("/api/v1/branding") : null,
      services: canServices
        ? await apiRequest<ServiceRow[]>("/api/v1/external-services")
        : [],
    }),
    [canBrand, canServices],
  );
  const [tab, setTabState] = useState<"brand" | "services" | "ai">(
    canBrand ? "brand" : "services",
  );
  const [brand, setBrand] = useState<BrandingRow | null>(null);
  const committedBrandRef = useRef<BrandingRow | null>(null);
  const logoInputRef = useRef<HTMLInputElement | null>(null);
  const backgroundInputRef = useRef<HTMLInputElement | null>(null);
  const [logoPreview, setLogoPreview] = useState("");
  const [backgroundPreview, setBackgroundPreview] = useState("");
  const [service, setService] = useState<ServiceDraft>(defaultServiceDraft());
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);

  function setTab(next: "brand" | "services" | "ai") {
    setTabState(next);
    window.localStorage.setItem("lmspilot-settings-tab", next);
  }

  useEffect(() => {
    const remembered = window.localStorage.getItem("lmspilot-settings-tab");
    if (remembered === "ai" && canServices) setTabState("ai");
    else if (remembered === "services" && canServices) setTabState("services");
    else if (remembered === "brand" && canBrand) setTabState("brand");
  }, [canBrand, canServices]);

  useEffect(() => {
    if (!data?.branding) return;
    const normalized = {
      ...data.branding,
      themeKey: normalizeThemeKey(data.branding.themeKey),
    };
    setBrand(normalized);
    committedBrandRef.current = normalized;
    applyBrandingPreview(normalized);
  }, [data?.branding]);

  useEffect(
    () => () => {
      if (logoPreview.startsWith("blob:")) URL.revokeObjectURL(logoPreview);
      if (backgroundPreview.startsWith("blob:")) URL.revokeObjectURL(backgroundPreview);
    },
    [logoPreview, backgroundPreview],
  );
  useEffect(
    () => () => {
      if (committedBrandRef.current) applyBrandingPreview(committedBrandRef.current);
    },
    [],
  );

  useEffect(() => {
    if (!canBrand && canServices) setTabState("services");
  }, [canBrand, canServices]);

  const services = data?.services ?? [];
  const selectedMeta = SERVICE_META[service.serviceType];

  function updateBrand(patch: Partial<BrandingRow>) {
    setBrand((current) => {
      if (!current) return current;
      const next = { ...current, ...patch };
      applyBrandingPreview(next);
      return next;
    });
  }

  async function uploadLogo(file?: File) {
    if (!file) return;
    if (!new Set(["image/png", "image/jpeg"]).has(file.type)) {
      setNotice({ tone: "error", message: "Logo phải là PNG hoặc JPG." });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setNotice({ tone: "error", message: "Logo không được vượt quá 5 MB." });
      return;
    }
    setWorking(true);
    setNotice(null);
    try {
      const form = new FormData();
      form.set("file", file);
      const uploaded = await apiRequest<{ id: string }>(
        "/api/v1/files?purpose=BRANDING_LOGO",
        { method: "POST", body: form },
      );
      if (logoPreview.startsWith("blob:")) URL.revokeObjectURL(logoPreview);
      setLogoPreview(URL.createObjectURL(file));
      updateBrand({ logoFileId: uploaded.id });
      setNotice({ tone: "success", message: "Đã tải logo lên. Nhấn Lưu thay đổi để áp dụng." });
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể tải logo",
      });
    } finally {
      setWorking(false);
    }
  }

  async function uploadLoginBackground(file?: File) {
    if (!file) return;
    if (!new Set(["image/png", "image/jpeg", "image/webp"]).has(file.type)) {
      setNotice({ tone: "error", message: "Ảnh nền phải là PNG, JPG hoặc WebP." });
      return;
    }
    if (file.size > 12 * 1024 * 1024) {
      setNotice({ tone: "error", message: "Ảnh nền không được vượt quá 12 MB." });
      return;
    }
    setWorking(true);
    setNotice(null);
    try {
      const form = new FormData();
      form.set("file", file);
      const uploaded = await apiRequest<{ id: string }>(
        "/api/v1/files?purpose=BRANDING_BACKGROUND",
        { method: "POST", body: form },
      );
      if (backgroundPreview.startsWith("blob:")) URL.revokeObjectURL(backgroundPreview);
      setBackgroundPreview(URL.createObjectURL(file));
      updateBrand({ backgroundFileId: uploaded.id });
      setNotice({ tone: "success", message: "Đã tải ảnh nền đăng nhập. Nhấn Lưu thay đổi để áp dụng." });
    } catch (cause) {
      setNotice({ tone: "error", message: cause instanceof Error ? cause.message : "Không thể tải ảnh nền" });
    } finally {
      setWorking(false);
    }
  }

  async function saveBrand(event: FormEvent) {
    event.preventDefault();
    if (!brand || !canBrand) return;
    setWorking(true);
    setNotice(null);
    try {
      const saved = await apiRequest<BrandingRow>("/api/v1/branding", {
        method: "PUT",
        body: JSON.stringify({
          systemName: brand.systemName.trim(),
          introduction: brand.introduction?.trim() || null,
          logoFileId: brand.logoFileId || null,
          faviconFileId: brand.faviconFileId || brand.logoFileId || null,
          backgroundFileId: brand.backgroundFileId || null,
          // Chế độ sáng/tối thuộc lựa chọn cá nhân trên thanh trên cùng.
          themeKey: normalizeThemeKey(committedBrandRef.current?.themeKey ?? brand.themeKey),
          primaryColor: brand.primaryColor,
          secondaryColor: brand.secondaryColor,
          backgroundColor: brand.backgroundColor,
          textColor: brand.textColor,
          customDomain: brand.customDomain?.trim() || null,
        }),
      });
      const normalized = { ...saved, themeKey: normalizeThemeKey(saved.themeKey) };
      setBrand(normalized);
      committedBrandRef.current = normalized;
      applyBrandingPreview(normalized);
      setNotice({ tone: "success", message: "Đã cập nhật cấu hình thông tin và màu thương hiệu." });
      await refresh();
      router.refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể lưu cấu hình",
      });
    } finally {
      setWorking(false);
    }
  }

  function editService(item: ServiceRow) {
    const config = item.config ?? {};
    setService({
      ...defaultServiceDraft(item.serviceType),
      id: item.id,
      configKey: item.configKey,
      enabled: item.enabled,
      host: String(config.host ?? ""),
      port: String(config.port ?? ""),
      username: String(config.username ?? ""),
      database: String(config.database ?? "0"),
      endpoint: String(config.endpoint ?? config.baseUrl ?? ""),
      callbackUrl: String(config.callbackUrl ?? ""),
      model: String(config.model ?? ""),
      bucket: String(config.bucket ?? ""),
      region: String(config.region ?? ""),
      accessKey: String(config.accessKey ?? ""),
      fromEmail: String(config.fromEmail ?? ""),
      fromName: String(config.fromName ?? ""),
      security: String(config.security ?? "STARTTLS"),
      provider: String(config.provider ?? "JITSI"),
      pathStyle: Boolean(config.pathStyle),
      secure: config.secure !== false && config.tls !== false,
      secret: "",
      secretConfigured: item.secretConfigured,
    });
    setNotice({ tone: "info", message: `Đang chỉnh sửa ${SERVICE_META[item.serviceType].label}.` });
    document.getElementById("service-editor")?.scrollIntoView({ behavior: "smooth" });
  }

  function buildServiceConfig(draft: ServiceDraft): Record<string, unknown> {
    switch (draft.serviceType) {
      case "REDIS":
        return {
          host: draft.host.trim(),
          port: Number(draft.port || 6379),
          username: draft.username.trim() || undefined,
          database: Number(draft.database || 0),
          tls: draft.secure,
        };
      case "SMTP":
        return {
          host: draft.host.trim(),
          port: Number(draft.port || 587),
          username: draft.username.trim() || undefined,
          security: draft.security,
          fromEmail: draft.fromEmail.trim(),
          fromName: draft.fromName.trim() || undefined,
        };
      case "AI_PROVIDER":
        return {
          endpoint: draft.endpoint.trim(),
          model: draft.model.trim(),
          apiStyle: "OPENAI_COMPATIBLE",
        };
      case "OBJECT_STORAGE":
        return {
          endpoint: draft.endpoint.trim(),
          bucket: draft.bucket.trim(),
          region: draft.region.trim(),
          accessKey: draft.accessKey.trim(),
          pathStyle: draft.pathStyle,
          secure: draft.secure,
        };
      case "DOCUMENT_EDITOR":
        return {
          endpoint: draft.endpoint.trim(),
          callbackUrl: draft.callbackUrl.trim(),
          jwtEnabled: draft.secretConfigured || Boolean(draft.secret.trim()),
        };
      case "VIDEO_CONFERENCE":
        return {
          endpoint: draft.endpoint.trim(),
          provider: draft.provider,
        };
    }
  }

  async function saveService(event: FormEvent) {
    event.preventDefault();
    if (!canServices) return;
    setWorking(true);
    setNotice(null);
    try {
      const path = service.id
        ? `/api/v1/external-services/${service.id}`
        : "/api/v1/external-services";
      await apiRequest(path, {
        method: service.id ? "PUT" : "POST",
        body: JSON.stringify({
          serviceType: service.serviceType,
          configKey: service.configKey.trim() || "default",
          enabled: service.enabled,
          config: buildServiceConfig(service),
          ...(service.secret ? { secret: service.secret } : {}),
        }),
      });
      setNotice({
        tone: "success",
        message: service.id ? "Đã cập nhật dịch vụ." : "Đã thêm dịch vụ.",
      });
      setService(defaultServiceDraft(service.serviceType));
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể lưu dịch vụ",
      });
    } finally {
      setWorking(false);
    }
  }

  async function testService(id: string) {
    setWorking(true);
    setNotice(null);
    try {
      const result = await apiRequest<ServiceRow>(`/api/v1/external-services/${id}/test`, {
        method: "POST",
      });
      setNotice({
        tone: result.healthStatus === "HEALTHY" ? "success" : "error",
        message: `${SERVICE_META[result.serviceType].label}: ${serviceStatusLabel(result.healthStatus)}${
          result.lastError ? ` — ${result.lastError}` : ""
        }`,
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể kiểm tra kết nối",
      });
    } finally {
      setWorking(false);
    }
  }

  return (
    <>
      <WorkspaceHero
        eyebrow="CÀI ĐẶT"
        title="Cấu hình hệ thống"
        description="Cá nhân hóa thương hiệu và kết nối dịch vụ ngoài. Chế độ sáng/tối vẫn do từng người chọn trên thanh trên cùng."
        icon={<Icon name="settings" size={29} />}
        stats={[
          { value: brand?.systemName || "—", label: "Thương hiệu" },
          { value: services.filter((item) => item.enabled).length, label: "Dịch vụ bật", tone: "teal" },
        ]}
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      <div className="workspace-tabs settings-tabs" role="tablist">
        {canBrand && (
          <button
            type="button"
            className={tab === "brand" ? "active" : ""}
            onClick={() => setTab("brand")}
          >
            <Icon name="edit" size={16} /> Cấu hình thông tin
          </button>
        )}
        {canServices && (
          <button
            type="button"
            className={tab === "services" ? "active" : ""}
            onClick={() => setTab("services")}
          >
            <Icon name="operations" size={16} /> Dịch vụ ngoài
          </button>
        )}
        {canServices && (
          <button
            type="button"
            className={tab === "ai" ? "active" : ""}
            onClick={() => setTab("ai")}
          >
            <span className="settings-ai-tab-icon">AI</span> Kết nối model AI
          </button>
        )}
      </div>

      {loading ? (
        <Busy />
      ) : error ? (
        <NoticeBar notice={{ tone: "error", message: error }} onClose={() => void refresh()} />
      ) : null}

      {!loading && tab === "brand" && canBrand && brand && (
        <form className="brand-settings-layout" onSubmit={saveBrand}>
          <Panel
            title="Thông tin thương hiệu"
            subtitle="Logo, tên hiển thị, giới thiệu ngắn và màu chủ đạo."
          >
            <div className="workspace-form brand-form">
              <div className="brand-logo-picker wide">
                <div className="brand-logo-preview">
                  {logoPreview || brand.logoUrl ? (
                    <img
                      src={logoPreview || browserBrandAsset(brand.logoUrl)}
                      alt="Logo xem trước"
                    />
                  ) : (
                    brand.systemName.slice(0, 1).toUpperCase()
                  )}
                </div>
                <div className="brand-logo-actions">
                  <strong>Logo hệ thống</strong>
                  <p>PNG hoặc JPG · tối đa 5 MB · nên dùng ảnh vuông.</p>
                  <input
                    ref={logoInputRef}
                    type="file"
                    accept="image/png,image/jpeg"
                    onChange={(event) => void uploadLogo(event.target.files?.[0])}
                  />
                  <button
                    type="button"
                    className="workspace-button secondary brand-file-button"
                    onClick={() => logoInputRef.current?.click()}
                    disabled={working}
                  >
                    <Icon name="upload" size={16} /> Chọn ảnh logo
                  </button>
                </div>
              </div>

              <div className="brand-login-background wide">
                <div
                  className="brand-background-preview"
                  style={{
                    backgroundImage: `linear-gradient(135deg, rgba(20,27,64,.58), rgba(77,58,168,.38)), url(${
                      backgroundPreview || browserBrandAsset(brand.backgroundUrl) || ""
                    })`,
                  }}
                >
                  <span>Ảnh nền trang đăng nhập</span>
                </div>
                <div className="brand-logo-actions">
                  <strong>Nền đăng nhập</strong>
                  <p>PNG, JPG hoặc WebP · tối đa 12 MB · khuyến nghị 1920×1080.</p>
                  <input
                    ref={backgroundInputRef}
                    type="file"
                    accept="image/png,image/jpeg,image/webp"
                    onChange={(event) => void uploadLoginBackground(event.target.files?.[0])}
                  />
                  <div className="page-actions">
                    <button type="button" className="workspace-button secondary brand-file-button" onClick={() => backgroundInputRef.current?.click()} disabled={working}>
                      <Icon name="upload" size={16} /> Chọn ảnh nền
                    </button>
                    {brand.backgroundFileId && (
                      <button type="button" className="workspace-button ghost" onClick={() => { setBackgroundPreview(""); updateBrand({ backgroundFileId: undefined, backgroundUrl: undefined }); }}>
                        Bỏ ảnh nền
                      </button>
                    )}
                  </div>
                </div>
              </div>

              <Field label="Tên thương hiệu" wide>
                <input
                  required
                  maxLength={240}
                  value={brand.systemName}
                  onChange={(event) => updateBrand({ systemName: event.target.value })}
                  placeholder="Tên hiển thị trong hệ thống"
                />
              </Field>

              <Field label="Giới thiệu ngắn" wide>
                <textarea
                  rows={4}
                  maxLength={500}
                  value={brand.introduction ?? ""}
                  onChange={(event) => updateBrand({ introduction: event.target.value })}
                  placeholder="Một câu giới thiệu ngắn dành cho người dùng"
                />
                <span className="brand-description-counter">
                  {(brand.introduction ?? "").length}/500
                </span>
              </Field>

              <div className="workspace-field wide">
                <span className="workspace-field-label">Màu chủ đạo</span>
                <div className="brand-color-presets" role="group" aria-label="Màu thương hiệu">
                  {BRAND_COLORS.map((preset) => (
                    <button
                      key={preset.primary}
                      type="button"
                      className={`brand-color-swatch ${
                        brand.primaryColor.toUpperCase() === preset.primary ? "selected" : ""
                      }`}
                      style={{ background: preset.primary }}
                      title={preset.name}
                      aria-label={preset.name}
                      onClick={() =>
                        updateBrand({
                          primaryColor: preset.primary,
                          secondaryColor: preset.secondary,
                        })
                      }
                    />
                  ))}
                </div>
              </div>

              <div className="workspace-field wide">
                <span className="workspace-field-label">Màu tùy chỉnh</span>
                <div className="brand-custom-color">
                  <input
                    type="color"
                    value={brand.primaryColor.slice(0, 7)}
                    onChange={(event) => updateBrand({ primaryColor: event.target.value.toUpperCase() })}
                    aria-label="Chọn màu chủ đạo"
                  />
                  <input
                    value={brand.primaryColor}
                    pattern="^#[0-9A-Fa-f]{6}$"
                    onChange={(event) => updateBrand({ primaryColor: event.target.value.toUpperCase() })}
                    aria-label="Mã màu chủ đạo"
                  />
                </div>
              </div>

              <details className="workspace-details wide">
                <summary>Cấu hình tên miền riêng</summary>
                <Field label="Tên miền" wide hint="Ví dụ: academy.example.com">
                  <input
                    value={brand.customDomain ?? ""}
                    onChange={(event) => updateBrand({ customDomain: event.target.value })}
                    placeholder="academy.example.com"
                  />
                </Field>
              </details>
            </div>
            <div className="brand-save-row">
              <Button type="submit" disabled={working || !brand.systemName.trim()}>
                <Icon name="save" size={16} /> {working ? "Đang lưu…" : "Lưu thay đổi"}
              </Button>
            </div>
          </Panel>

          <Panel
            title="Xem trước"
            subtitle="Màu thương hiệu được xem trực tiếp; sáng/tối không bị thay đổi."
            className="sticky-panel"
          >
            <div
              className="branding-preview"
              style={{
                color: readableText(brand.primaryColor),
                background: brand.primaryColor,
              }}
            >
              <header>
                <span>
                  {logoPreview || brand.logoUrl ? (
                    <img src={logoPreview || browserBrandAsset(brand.logoUrl)} alt="" />
                  ) : (
                    brand.systemName.slice(0, 1).toUpperCase()
                  )}
                </span>
                <strong>{brand.systemName || "Tên thương hiệu"}</strong>
              </header>
              <div className="branding-preview-content">
                <small>KHÔNG GIAN HỌC TẬP</small>
                <h3>Chào mừng trở lại</h3>
                <p>{brand.introduction || "Nội dung giới thiệu sẽ hiển thị tại đây."}</p>
                <button
                  type="button"
                  style={{
                    background: readableText(brand.primaryColor),
                    color: brand.primaryColor,
                  }}
                >
                  Tiếp tục học
                </button>
                <div className="preview-cards"><i /><i /><i /></div>
              </div>
            </div>
          </Panel>
        </form>
      )}

      {!loading && tab === "ai" && canServices && <AiConnectionCenter />}

      {!loading && tab === "services" && canServices && (
        <div className="workspace-two-column wide-left settings-services-layout">
          <Panel
            title={service.id ? "Chỉnh sửa dịch vụ" : "Kết nối dịch vụ"}
            subtitle="Biểu mẫu thay đổi theo từng loại dịch vụ; bí mật được mã hóa ở backend."
            className="sticky-panel"
            action={
              service.id ? (
                <Button tone="ghost" onClick={() => setService(defaultServiceDraft(service.serviceType))}>
                  Tạo mới
                </Button>
              ) : undefined
            }
          >
            <form className="workspace-form" onSubmit={saveService} id="service-editor">
              <div className="service-type-grid wide">
                {(Object.keys(SERVICE_META) as ServiceType[]).map((type) => {
                  const meta = SERVICE_META[type];
                  return (
                    <button
                      type="button"
                      key={type}
                      className={`service-type-option ${service.serviceType === type ? "active" : ""}`}
                      onClick={() => setService(defaultServiceDraft(type))}
                    >
                      <span>{meta.icon}</span>
                      <span><strong>{meta.label}</strong><small>{meta.short}</small></span>
                    </button>
                  );
                })}
              </div>

              <div className="service-help wide">
                <Icon name="question" size={18} />
                <div>
                  <strong>{selectedMeta.label}</strong>
                  <p>{selectedMeta.short}. <a href={selectedMeta.docs} target="_blank" rel="noreferrer">Mở tài liệu chính thức</a></p>
                </div>
              </div>

              <Field label="Tên cấu hình">
                <input
                  required
                  value={service.configKey}
                  onChange={(event) => setService({ ...service, configKey: event.target.value })}
                  placeholder="default"
                />
              </Field>

              {service.serviceType === "REDIS" && (
                <>
                  <Field label="Máy chủ"><input required value={service.host} onChange={(event) => setService({ ...service, host: event.target.value })} placeholder="redis.example.com" /></Field>
                  <Field label="Cổng"><input required inputMode="numeric" value={service.port} onChange={(event) => setService({ ...service, port: event.target.value })} /></Field>
                  <Field label="Tên người dùng"><input value={service.username} onChange={(event) => setService({ ...service, username: event.target.value })} placeholder="default" /></Field>
                  <Field label="Database"><input inputMode="numeric" value={service.database} onChange={(event) => setService({ ...service, database: event.target.value })} /></Field>
                </>
              )}

              {service.serviceType === "SMTP" && (
                <>
                  <Field label="Máy chủ SMTP"><input required value={service.host} onChange={(event) => setService({ ...service, host: event.target.value })} placeholder="smtp.example.com" /></Field>
                  <Field label="Cổng"><input required inputMode="numeric" value={service.port} onChange={(event) => setService({ ...service, port: event.target.value })} /></Field>
                  <Field label="Tên đăng nhập"><input value={service.username} onChange={(event) => setService({ ...service, username: event.target.value })} /></Field>
                  <Field label="Bảo mật"><select value={service.security} onChange={(event) => setService({ ...service, security: event.target.value })}><option>STARTTLS</option><option>TLS</option><option>NONE</option></select></Field>
                  <Field label="Email gửi" wide><input required type="email" value={service.fromEmail} onChange={(event) => setService({ ...service, fromEmail: event.target.value })} placeholder="noreply@example.com" /></Field>
                  <Field label="Tên người gửi" wide><input value={service.fromName} onChange={(event) => setService({ ...service, fromName: event.target.value })} /></Field>
                </>
              )}

              {service.serviceType === "AI_PROVIDER" && (
                <>
                  <Field label="API endpoint" wide><input required type="url" value={service.endpoint} onChange={(event) => setService({ ...service, endpoint: event.target.value })} placeholder="https://api.openai.com/v1" /></Field>
                  <Field label="Model" wide><input required value={service.model} onChange={(event) => setService({ ...service, model: event.target.value })} placeholder="Tên model theo nhà cung cấp" /></Field>
                </>
              )}

              {service.serviceType === "OBJECT_STORAGE" && (
                <>
                  <Field label="Endpoint" wide><input required type="url" value={service.endpoint} onChange={(event) => setService({ ...service, endpoint: event.target.value })} placeholder="https://s3.example.com" /></Field>
                  <Field label="Bucket"><input required value={service.bucket} onChange={(event) => setService({ ...service, bucket: event.target.value })} /></Field>
                  <Field label="Region"><input required value={service.region} onChange={(event) => setService({ ...service, region: event.target.value })} placeholder="ap-southeast-1" /></Field>
                  <Field label="Access key"><input required value={service.accessKey} onChange={(event) => setService({ ...service, accessKey: event.target.value })} /></Field>
                  <label className="workspace-check"><input type="checkbox" checked={service.pathStyle} onChange={(event) => setService({ ...service, pathStyle: event.target.checked })} /><span>Dùng path-style URL (MinIO/S3 tương thích)</span></label>
                </>
              )}

              {service.serviceType === "DOCUMENT_EDITOR" && (
                <>
                  <Field label="ONLYOFFICE Docs URL" wide><input required type="url" value={service.endpoint} onChange={(event) => setService({ ...service, endpoint: event.target.value })} placeholder="https://docs.example.com" /></Field>
                  <Field label="Callback URL công khai" wide hint="ONLYOFFICE phải truy cập được URL này để trả trạng thái lưu tài liệu."><input required type="url" value={service.callbackUrl} onChange={(event) => setService({ ...service, callbackUrl: event.target.value })} placeholder="https://lms.example.com/api/..." /></Field>
                </>
              )}

              {service.serviceType === "VIDEO_CONFERENCE" && (
                <>
                  <Field label="Nhà cung cấp"><select value={service.provider} onChange={(event) => setService({ ...service, provider: event.target.value })}><option value="JITSI">Jitsi</option><option value="ZOOM">Zoom</option><option value="TEAMS">Microsoft Teams</option><option value="CUSTOM">Tùy chỉnh</option></select></Field>
                  <Field label="Endpoint" wide><input required type="url" value={service.endpoint} onChange={(event) => setService({ ...service, endpoint: event.target.value })} placeholder="https://meet.example.com" /></Field>
                </>
              )}

              <Field
                label={selectedMeta.secretLabel}
                wide
                hint={service.id ? "Để trống để giữ bí mật hiện tại." : "Bí mật được mã hóa AES-GCM trước khi lưu."}
              >
                <input
                  type="password"
                  autoComplete="new-password"
                  value={service.secret}
                  onChange={(event) => setService({ ...service, secret: event.target.value })}
                />
              </Field>

              {(["REDIS", "OBJECT_STORAGE"] as ServiceType[]).includes(service.serviceType) && (
                <label className="workspace-check">
                  <input type="checkbox" checked={service.secure} onChange={(event) => setService({ ...service, secure: event.target.checked })} />
                  <span>Dùng kết nối mã hóa TLS/HTTPS</span>
                </label>
              )}
              <label className="workspace-check">
                <input type="checkbox" checked={service.enabled} onChange={(event) => setService({ ...service, enabled: event.target.checked })} />
                <span>Bật dịch vụ sau khi lưu</span>
              </label>
              <Button type="submit" disabled={working}>
                <Icon name="save" size={16} /> {working ? "Đang lưu…" : service.id ? "Cập nhật dịch vụ" : "Lưu dịch vụ"}
              </Button>
            </form>
          </Panel>

          <Panel
            title="Dịch vụ đã cấu hình"
            subtitle={`${services.length} cấu hình · ${services.filter((item) => item.enabled).length} đang bật`}
          >
            {services.length ? (
              <div className="workspace-card-list">
                {services.map((item) => {
                  const meta = SERVICE_META[item.serviceType];
                  const visibleEntries = Object.entries(item.config ?? {})
                    .filter(([, value]) => value !== undefined && value !== null && value !== "")
                    .slice(0, 4);
                  return (
                    <article className="workspace-mini-card service-card" key={item.id}>
                      <div>
                        <span className="service-card-icon">{meta.icon}</span>
                        <div><strong>{meta.label}</strong><small>{item.configKey}</small></div>
                        <Tag tone={item.healthStatus === "HEALTHY" ? "teal" : item.healthStatus === "UNREACHABLE" || item.healthStatus === "MISCONFIGURED" ? "danger" : ""}>
                          {serviceStatusLabel(item.healthStatus)}
                        </Tag>
                      </div>
                      <dl>
                        {visibleEntries.map(([key, value]) => (
                          <div key={key}><dt>{key}</dt><dd>{String(value)}</dd></div>
                        ))}
                      </dl>
                      <footer>
                        <span className="service-secret-note">
                          {item.secretConfigured ? "Đã lưu bí mật" : "Chưa có bí mật"} · {item.enabled ? "Đang bật" : "Đang tắt"}
                        </span>
                        <span className="service-card-actions">
                          <Button tone="ghost" onClick={() => editService(item)} title="Chỉnh sửa"><Icon name="edit" size={15} /></Button>
                          <Button tone="secondary" onClick={() => void testService(item.id)} disabled={working}><Icon name="refresh" size={15} /> Kiểm tra</Button>
                        </span>
                      </footer>
                      {item.lastError && <p className="service-error">{item.lastError}</p>}
                    </article>
                  );
                })}
              </div>
            ) : (
              <Empty text="Chưa có dịch vụ ngoài. Chọn loại dịch vụ và nhập thông tin kết nối." />
            )}
          </Panel>
        </div>
      )}
    </>
  );
}

// Core results and appeals

type GradeResultRow = {
  id: string;
  sessionId: string;
  examId: string;
  courseId?: string;
  score: number;
  maxScore: number;
  percentage: number;
  passed: boolean;
  status: string;
  feedback?: string;
  updatedAt: string;
};
type GradeAppealRow = {
  id: string;
  gradeId: string;
  reason: string;
  status: string;
  resolution?: string;
  createdAt: string;
  resolvedAt?: string;
};

function GradeResultsConsole({ user }: { user: PortalUser }) {
  const manage = user.permissions.includes("grade-appeals:manage");
  const { data, loading, error, refresh } = useLoad(async () => {
    if (manage) {
      const [grades, appeals] = await Promise.all([
        apiRequest<GradeResultRow[]>("/api/v1/grades/queue").catch(() => []),
        apiRequest<GradeAppealRow[]>("/api/v1/grades/appeals"),
      ]);
      return { grades, appeals };
    }
    const [grades, appeals] = await Promise.all([
      apiRequest<GradeResultRow[]>("/api/v1/grades/me"),
      apiRequest<GradeAppealRow[]>("/api/v1/grades/appeals/me").catch(() => []),
    ]);
    return { grades, appeals };
  }, [manage]);
  const [notice, setNotice] = useState<Notice>(null);
  const [selectedGrade, setSelectedGrade] = useState<string>("");
  const [reason, setReason] = useState("");
  const [resolution, setResolution] = useState({
    appealId: "",
    status: "APPROVED",
    resolution: "",
    correctedScore: "",
  });
  const [working, setWorking] = useState(false);
  const grades = data?.grades ?? [];
  const appeals = data?.appeals ?? [];

  async function createAppeal(event: FormEvent) {
    event.preventDefault();
    if (!selectedGrade) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest(`/api/v1/grades/${selectedGrade}/appeals`, {
        method: "POST",
        body: JSON.stringify({ reason }),
      });
      setSelectedGrade("");
      setReason("");
      setNotice({
        tone: "success",
        message:
          "Đã gửi yêu cầu phúc khảo. Kết quả và lịch sử điểm sẽ được giữ nguyên để đối chiếu.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể gửi phúc khảo",
      });
    } finally {
      setWorking(false);
    }
  }

  async function resolveAppeal(event: FormEvent) {
    event.preventDefault();
    if (!resolution.appealId) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest(`/api/v1/grades/appeals/${resolution.appealId}`, {
        method: "PUT",
        body: JSON.stringify({
          status: resolution.status,
          resolution: resolution.resolution,
          correctedScore: resolution.correctedScore
            ? Number(resolution.correctedScore)
            : null,
        }),
      });
      setResolution({
        appealId: "",
        status: "APPROVED",
        resolution: "",
        correctedScore: "",
      });
      setNotice({
        tone: "success",
        message: "Đã xử lý phúc khảo và lưu lịch sử thay đổi điểm.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể xử lý phúc khảo",
      });
    } finally {
      setWorking(false);
    }
  }

  return (
    <>
      <WorkspaceHero
        eyebrow="HỒ SƠ ĐÁNH GIÁ"
        title={manage ? "Hàng chờ phúc khảo" : "Kết quả & phúc khảo"}
        description={
          manage
            ? "Xử lý yêu cầu theo đúng phạm vi kỳ thi; mọi điều chỉnh điểm đều tạo bản ghi lịch sử."
            : "Xem điểm, phản hồi và gửi phúc khảo cho kết quả đã được chấm hoàn tất."
        }
        icon={<Icon name="grade" size={31} />}
        stats={[
          { value: grades.length, label: manage ? "Bài chờ chấm" : "Kết quả" },
          {
            value: appeals.filter((item) =>
              ["OPEN", "UNDER_REVIEW"].includes(item.status),
            ).length,
            label: "Phúc khảo mở",
          },
          {
            value: appeals.filter((item) => item.status === "APPROVED").length,
            label: "Đã chấp thuận",
            tone: "teal",
          },
        ]}
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      {loading ? (
        <Busy />
      ) : error ? (
        <NoticeBar
          notice={{ tone: "error", message: error }}
          onClose={() => void refresh()}
        />
      ) : (
        <div className="workspace-grid-three">
          <Panel
            title={manage ? "Kết quả cần xử lý" : "Kết quả của tôi"}
            subtitle="Điểm và trạng thái được lấy trực tiếp từ Grading Service."
            className="span-two"
          >
            {grades.length ? (
              <div className="grade-result-list">
                {grades.map((grade) => (
                  <article key={grade.id}>
                    <div
                      className={`grade-orb ${grade.passed ? "passed" : ""}`}
                    >
                      <strong>{Math.round(grade.percentage)}%</strong>
                      <span>{grade.passed ? "Đạt" : "Chưa đạt"}</span>
                    </div>
                    <div>
                      <h3>Bài thi #{grade.examId.slice(0, 8)}</h3>
                      <p>
                        {grade.score}/{grade.maxScore} điểm · {grade.status}
                      </p>
                      {grade.feedback && (
                        <blockquote>{grade.feedback}</blockquote>
                      )}
                      <small>
                        Cập nhật{" "}
                        {new Date(grade.updatedAt).toLocaleString("vi-VN")}
                      </small>
                    </div>
                    {!manage && grade.status === "COMPLETED" && (
                      <Button
                        tone="secondary"
                        onClick={() => setSelectedGrade(grade.id)}
                      >
                        Phúc khảo
                      </Button>
                    )}
                  </article>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có kết quả phù hợp." />
            )}
          </Panel>
          {!manage ? (
            <Panel
              title="Gửi phúc khảo"
              subtitle="Mô tả cụ thể câu hỏi hoặc điểm cần được xem lại."
            >
              <form className="workspace-form" onSubmit={createAppeal}>
                <Field label="Kết quả" wide>
                  <select
                    required
                    value={selectedGrade}
                    onChange={(e) => setSelectedGrade(e.target.value)}
                  >
                    <option value="">Chọn kết quả</option>
                    {grades
                      .filter((item) => item.status === "COMPLETED")
                      .map((item) => (
                        <option key={item.id} value={item.id}>
                          #{item.examId.slice(0, 8)} ·{" "}
                          {Math.round(item.percentage)}%
                        </option>
                      ))}
                  </select>
                </Field>
                <Field label="Lý do" wide>
                  <textarea
                    required
                    minLength={10}
                    maxLength={4000}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                  />
                </Field>
                <Button type="submit" disabled={working || !selectedGrade}>
                  Gửi phúc khảo
                </Button>
              </form>
            </Panel>
          ) : (
            <Panel
              title="Xử lý phúc khảo"
              subtitle="Chỉ sửa điểm khi có căn cứ và ghi rõ lý do."
            >
              <form className="workspace-form" onSubmit={resolveAppeal}>
                <Field label="Yêu cầu" wide>
                  <select
                    required
                    value={resolution.appealId}
                    onChange={(e) =>
                      setResolution({ ...resolution, appealId: e.target.value })
                    }
                  >
                    <option value="">Chọn yêu cầu</option>
                    {appeals
                      .filter((item) =>
                        ["OPEN", "UNDER_REVIEW"].includes(item.status),
                      )
                      .map((item) => (
                        <option key={item.id} value={item.id}>
                          #{item.gradeId.slice(0, 8)} ·{" "}
                          {item.reason.slice(0, 50)}
                        </option>
                      ))}
                  </select>
                </Field>
                <Field label="Kết luận">
                  <select
                    value={resolution.status}
                    onChange={(e) =>
                      setResolution({ ...resolution, status: e.target.value })
                    }
                  >
                    <option value="APPROVED">Chấp thuận</option>
                    <option value="REJECTED">Từ chối</option>
                  </select>
                </Field>
                <Field label="Điểm sửa (tùy chọn)">
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    value={resolution.correctedScore}
                    onChange={(e) =>
                      setResolution({
                        ...resolution,
                        correctedScore: e.target.value,
                      })
                    }
                  />
                </Field>
                <Field label="Nội dung xử lý" wide>
                  <textarea
                    required
                    minLength={3}
                    maxLength={4000}
                    value={resolution.resolution}
                    onChange={(e) =>
                      setResolution({
                        ...resolution,
                        resolution: e.target.value,
                      })
                    }
                  />
                </Field>
                <Button
                  type="submit"
                  disabled={working || !resolution.appealId}
                >
                  Hoàn tất xử lý
                </Button>
              </form>
            </Panel>
          )}
          <Panel
            title="Lịch sử phúc khảo"
            subtitle="Theo dõi trạng thái và phản hồi chính thức."
            className="span-three"
          >
            {appeals.length ? (
              <div className="appeal-history">
                {appeals.map((item) => (
                  <article key={item.id}>
                    <Tag
                      tone={
                        item.status === "APPROVED"
                          ? "teal"
                          : item.status === "REJECTED"
                            ? "danger"
                            : ""
                      }
                    >
                      {item.status}
                    </Tag>
                    <div>
                      <strong>Kết quả #{item.gradeId.slice(0, 8)}</strong>
                      <p>{item.reason}</p>
                      {item.resolution && (
                        <blockquote>{item.resolution}</blockquote>
                      )}
                      <small>
                        {new Date(item.createdAt).toLocaleString("vi-VN")}
                      </small>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có yêu cầu phúc khảo." />
            )}
          </Panel>
        </div>
      )}
    </>
  );
}
