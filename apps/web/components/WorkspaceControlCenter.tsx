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
import {
  getTheme,
  normalizeThemeKey,
  THEMES,
  type ThemeDefinition,
  type ThemeKey,
} from "@/lib/themes";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
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
    user.accountType === "SYSTEM_ADMIN" ||
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
  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await loader());
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Không thể tải dữ liệu",
      );
    } finally {
      setLoading(false);
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
  const systemAdmin = user.accountType === "SYSTEM_ADMIN";
  const canReadUsers = systemAdmin || user.permissions.includes("users:read");
  const canReadRoles =
    systemAdmin ||
    user.permissions.some((permission) =>
      ["roles:read", "users:read", "authorization:grant"].includes(permission),
    );
  const canGrant = systemAdmin || user.permissions.includes("authorization:grant");
  const canRevoke = systemAdmin || user.permissions.includes("authorization:revoke");
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
    roleCode: "BASIC_USER",
  });
  const [grant, setGrant] = useState({
    kind: "ROLE",
    value: "BASIC_USER",
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
    () =>
      Array.from(
        new Set([
          ...(catalog?.profiles ?? []).map((item) => item.code),
          ...roles.map((item) => item.code),
        ]),
      ),
    [catalog, roles],
  );
  const filtered = users.filter((item) =>
    `${item.code} ${item.username} ${item.fullName} ${item.email ?? ""}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );
  const inspectedUser = users.find((item) => item.id === inspectedUserId);
  const canWrite =
    systemAdmin ||
    user.permissions.some((value) =>
      ["users:create", "users:write", "users:bulk-manage"].includes(value),
    );
  const tabs: Array<{
    key: "accounts" | "grant" | "inspect" | "bulk";
    label: string;
  }> = [
    ...(canReadUsers ? [{ key: "accounts" as const, label: "Tài khoản" }] : []),
    ...(canGrant && canReadUsers
      ? [{ key: "grant" as const, label: "Cấp gói quyền" }]
      : []),
    ...(canReadUsers && canReadRoles
      ? [{ key: "inspect" as const, label: "Thu hồi quyền" }]
      : []),
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
        roleCode: "BASIC_USER",
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
        title="Tài khoản và gói quyền"
        description="Chọn người, chọn gói công việc và phạm vi áp dụng. Quyền kỹ thuật chi tiết được hệ thống quản lý ở phía sau."
        icon={<Icon name="users" size={31} />}
        stats={[
          { value: users.length, label: "Tài khoản" },
          { value: catalog?.profiles.length ?? 0, label: "Gói công việc" },
          { value: selected.size, label: "Đang chọn", tone: "violet" },
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
                subtitle="Chọn nhiều người để cấp cùng một gói quyền. Quản trị gốc được bảo vệ khỏi các thao tác thông thường."
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
                        <th>
                          <input
                            type="checkbox"
                            checked={filtered.some((item) => !item.protectedAccount) && filtered.filter((item) => !item.protectedAccount).every((item) => selected.has(item.id))}
                            onChange={(event) =>
                              setSelected(
                                event.target.checked
                                  ? new Set(filtered.filter((item) => !item.protectedAccount).map((item) => item.id))
                                  : new Set(),
                              )
                            }
                          />
                        </th>
                        <th>Danh tính</th>
                        <th>Gói quyền cơ sở</th>
                        <th>Loại</th>
                        <th>Trạng thái</th>
                        <th>Giải thích</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filtered.map((item) => (
                        <tr key={item.id}>
                          <td>
                            <input
                              type="checkbox"
                              checked={selected.has(item.id)}
                              disabled={item.protectedAccount}
                              onChange={(event) =>
                                setSelected((current) => {
                                  const next = new Set(current);
                                  event.target.checked ? next.add(item.id) : next.delete(item.id);
                                  return next;
                                })
                              }
                            />
                          </td>
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
                          <td>{item.protectedAccount ? <Tag tone="gold">SYSTEM ADMIN</Tag> : <Tag>USER</Tag>}</td>
                          <td><Tag tone={item.status === "ACTIVE" ? "teal" : "danger"}>{item.status}</Tag></td>
                          <td>
                            <Button tone="secondary" onClick={() => void loadInspection(item.id)}>
                              <Icon name="search" size={14} /> Kiểm tra
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
              {canWrite && (
                <Panel title="Tạo tài khoản" subtitle="Không có đăng ký công khai. Quản trị tạo tài khoản và cấp gói quyền ban đầu." className="sticky-panel">
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
                    <Field label="Gói quyền ban đầu" wide>
                      <select value={create.roleCode} onChange={(event) => setCreate({ ...create, roleCode: event.target.value })}>
                        {availableRoleCodes.map((code) => <option key={code} value={code}>{roleLabel(code)}</option>)}
                      </select>
                    </Field>
                    <Button type="submit" disabled={working || !canWrite}>
                      <Icon name="plus" size={16} /> {working ? "Đang tạo…" : "Tạo và cấp tài khoản"}
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
                <Link className="workspace-button primary" href="/users/import">
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

function OrganizationConsole({ user }: { user: PortalUser }) {
  const canManageUnits =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.some((permission) =>
      ["organization:manage", "organization:write"].includes(permission),
    );
  const canManageMembers =
    canManageUnits ||
    user.permissions.includes("organization:membership:manage");
  const { data, loading, error, refresh } = useLoad(
    async () => ({
      units: await apiRequest<UnitRow[]>("/api/v1/organization/units/tree"),
      flat: await apiRequest<UnitRow[]>("/api/v1/organization/units"),
    }),
    [],
  );
  const [selectedUnit, setSelectedUnit] = useState<string>("");
  const [members, setMembers] = useState<MembershipRow[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [unit, setUnit] = useState({
    code: "",
    name: "",
    type: "DEPARTMENT",
    parentId: "",
  });
  const [membership, setMembership] = useState({
    userIds: [""],
    membershipType: "MEMBER",
    primaryMembership: false,
  });
  const [working, setWorking] = useState(false);
  const flat = data?.flat ?? [];
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
        message:
          cause instanceof Error ? cause.message : "Không thể tạo đơn vị",
      });
    } finally {
      setWorking(false);
    }
  }
  async function grantMembers(event: FormEvent) {
    event.preventDefault();
    if (!canManageMembers) return;
    const ids = membership.userIds
      .map((value) => value.trim())
      .filter(Boolean);
    if (!selectedUnit || !ids.length) {
      setNotice({
        tone: "error",
        message: "Cần chọn đơn vị và nhập ít nhất một user ID.",
      });
      return;
    }
    setWorking(true);
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
        message: `Đã gán ${ids.length} người vào đơn vị.`,
      });
      setMembership({ ...membership, userIds: [""] });
      setMembers(
        await apiRequest<MembershipRow[]>(
          `/api/v1/organization/memberships?unitId=${selectedUnit}`,
        ),
      );
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể gán thành viên",
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
        eyebrow="BẢN ĐỒ TỔ CHỨC"
        title="Chi nhánh, phòng ban & nhóm người dùng"
        description="Tạo cây tổ chức nhiều cấp, gán thành viên và dùng chính cấu trúc này làm phạm vi cho quyền, khóa học, tin tức và báo cáo."
        icon={<Icon name="building" size={31} />}
        stats={[
          { value: countTree(data?.units ?? []), label: "Đơn vị" },
          {
            value: flat.filter((v) => v.type === "BRANCH").length,
            label: "Chi nhánh",
          },
          {
            value: flat.filter((v) => v.type === "DEPARTMENT").length,
            label: "Phòng ban",
          },
          { value: members.length, label: "Thành viên đang xem", tone: "teal" },
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
        <div className="workspace-grid-three org-layout">
          <Panel
            title="Cây tổ chức"
            subtitle="Chọn một đơn vị để xem và gán thành viên."
            className="span-two"
          >
            <OrganizationTree
              units={data?.units ?? []}
              selected={selectedUnit}
              onSelect={setSelectedUnit}
            />
          </Panel>
          {canManageUnits && (
            <Panel
              title="Tạo đơn vị"
              subtitle="Mô hình hỗ trợ công ty, chi nhánh, phòng ban và nhóm."
            >
              <form className="workspace-form" onSubmit={createUnit}>
                <Field label="Mã">
                  <input
                    required
                    value={unit.code}
                    onChange={(e) =>
                      setUnit({ ...unit, code: e.target.value.toUpperCase() })
                    }
                  />
                </Field>
                <Field label="Loại">
                  <select
                    value={unit.type}
                    onChange={(e) => setUnit({ ...unit, type: e.target.value })}
                  >
                    {[
                      "ORGANIZATION",
                      "BRANCH",
                      "DIVISION",
                      "DEPARTMENT",
                      "TEAM",
                      "GROUP",
                    ].map((v) => (
                      <option key={v}>{v}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Tên đơn vị" wide>
                  <input
                    required
                    value={unit.name}
                    onChange={(e) => setUnit({ ...unit, name: e.target.value })}
                  />
                </Field>
                <Field label="Đơn vị cha" wide>
                  <select
                    value={unit.parentId}
                    onChange={(e) =>
                      setUnit({ ...unit, parentId: e.target.value })
                    }
                  >
                    <option value="">— Cấp gốc —</option>
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
          <Panel
            title="Thành viên trong đơn vị"
            subtitle={
              selectedUnit
                ? `${members.length} thành viên trực tiếp`
                : "Chọn đơn vị trong cây"
            }
            className="span-two"
          >
            {members.length ? (
              <div className="member-list">
                {members.map((item) => (
                  <div key={item.id}>
                    <span className="mini-avatar">
                      {item.membershipType.slice(0, 1)}
                    </span>
                    <div>
                      <strong>{item.userId}</strong>
                      <small>
                        {item.membershipType}
                        {item.primaryMembership ? " · Đơn vị chính" : ""}
                      </small>
                    </div>
                    <Tag tone={item.active ? "teal" : "danger"}>
                      {item.active ? "ACTIVE" : "EXPIRED"}
                    </Tag>
                  </div>
                ))}
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
              title="Gán thành viên hàng loạt"
              subtitle="Một người có thể thuộc nhiều đơn vị với loại membership khác nhau."
            >
              <form className="workspace-form" onSubmit={grantMembers}>
                <Field
                  label="Người dùng"
                  wide
                  hint="Mỗi ô chứa một mã người dùng. Dùng + hoặc − để thay đổi danh sách."
                >
                  <RepeatableField
                    name="organizationUserIds"
                    initialValues={membership.userIds}
                    onValuesChange={(userIds) =>
                      setMembership((current) => ({ ...current, userIds }))
                    }
                    addLabel="Thêm người dùng"
                    placeholder="Mã người dùng"
                  />
                </Field>
                <Field label="Quan hệ trong đơn vị" hint="Chỉ mô tả quan hệ tổ chức, không cấp quyền hệ thống.">
                  <select
                    value={membership.membershipType}
                    onChange={(e) =>
                      setMembership({
                        ...membership,
                        membershipType: e.target.value,
                      })
                    }
                  >
                    {["MEMBER", "MANAGER", "INSTRUCTOR", "LEARNER"].map((v) => (
                      <option key={v}>{v}</option>
                    ))}
                  </select>
                </Field>
                <label className="workspace-check">
                  <input
                    type="checkbox"
                    checked={membership.primaryMembership}
                    onChange={(e) =>
                      setMembership({
                        ...membership,
                        primaryMembership: e.target.checked,
                      })
                    }
                  />
                  <span>Đặt làm đơn vị chính</span>
                </label>
                <Button type="submit" disabled={working || !selectedUnit}>
                  <Icon name="users" size={16} /> Gán vào đơn vị
                </Button>
              </form>
            </Panel>
          )}
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
            <div>
              <strong>{item.name}</strong>
              <small>
                {item.code} · {item.type}
              </small>
            </div>
            <Tag tone={item.status === "ACTIVE" ? "teal" : "danger"}>
              {item.status}
            </Tag>
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
  backgroundUrl?: string;
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
  customDomain?: string;
  updatedAt: string;
};

function applyBrandingPreview(branding: BrandingRow) {
  document.documentElement.dataset.theme = normalizeThemeKey(
    branding.themeKey,
  );
  document.body.style.setProperty("--brand-primary", branding.primaryColor);
  document.body.style.setProperty(
    "--brand-secondary",
    branding.secondaryColor,
  );
  document.body.style.setProperty(
    "--brand-background",
    branding.backgroundColor,
  );
  document.body.style.setProperty(
    "--brand-on-primary",
    readableText(branding.primaryColor),
  );
  document.body.style.setProperty(
    "--brand-on-background",
    readableText(branding.backgroundColor),
  );
}

function AppearanceStudio({
  brand,
  activeThemeKey,
  hasThemeDraft,
  working,
  onPreview,
  onReset,
  onApply,
}: {
  brand: BrandingRow;
  activeThemeKey: ThemeKey;
  hasThemeDraft: boolean;
  working: boolean;
  onPreview: (theme: ThemeDefinition) => void;
  onReset: () => void;
  onApply: () => void;
}) {
  const selectedTheme = getTheme(brand.themeKey);
  return (
    <section className="appearance-studio" aria-labelledby="appearance-studio-title">
      <header className="appearance-studio-intro">
        <div>
          <span className="appearance-studio-kicker">Giao diện</span>
          <h2 id="appearance-studio-title">Chọn chế độ sáng hoặc tối</h2>
          <p>
            Hai chế độ dùng cùng kiểu chữ, khoảng cách, component và bố cục. Màu
            thương hiệu chỉ tạo điểm nhấn; màu chữ được hệ thống tự tính để bảo
            đảm dễ đọc.
          </p>
        </div>
        <div className="appearance-studio-count" aria-label="Hai chế độ hiển thị">
          <strong>2</strong>
          <small>chế độ an toàn</small>
        </div>
      </header>

      <div className="appearance-gallery">
        {THEMES.map((theme) => {
          const selected = theme.key === selectedTheme.key;
          const active = theme.key === activeThemeKey;
          return (
            <article className={`appearance-card ${selected ? "selected" : ""}`} key={theme.key}>
              <div className="appearance-card-visual" data-preview-theme={theme.key} aria-hidden>
                <div className="appearance-preview-rail"><b /><i className="active" /><i /><i /></div>
                <div className="appearance-preview-canvas">
                  <div className="appearance-preview-topbar"><span /><i /></div>
                  <div className="appearance-preview-hero">
                    <small>Không gian học tập</small>
                    <strong>Học tập rõ ràng, tập trung</strong>
                  </div>
                  <div className="appearance-preview-modules"><i /><i /><i /></div>
                </div>
              </div>
              <div className="appearance-card-body">
                <div className="appearance-card-heading">
                  <div>
                    <small>{theme.mode === "light" ? "Nền sáng" : "Nền tối"}</small>
                    <h3>{theme.name}</h3>
                  </div>
                  <span className="appearance-card-state">
                    {active && !hasThemeDraft ? "Đang dùng" : selected ? "Đang xem" : "Có thể chọn"}
                  </span>
                </div>
                <p>{theme.description}</p>
                <div className="appearance-tags">
                  {theme.tags.map((tag) => <span key={tag}>{tag}</span>)}
                </div>
                <div className="appearance-card-actions">
                  <button
                    type="button"
                    className={`button ${selected ? "primary" : "secondary"}`}
                    onClick={() => onPreview(theme)}
                    aria-pressed={selected}
                  >
                    <Icon name={selected ? "check" : "eye"} size={15} />
                    {selected ? "Đang xem trực tiếp" : "Xem thử"}
                  </button>
                </div>
              </div>
            </article>
          );
        })}
      </div>

      <div className="appearance-selection-bar">
        <div className="appearance-selection-copy">
          <span className="appearance-selection-swatch" />
          <div>
            <strong>{selectedTheme.name}</strong>
            <small>{hasThemeDraft ? "Bản xem thử chưa được lưu" : "Đang áp dụng cho toàn hệ thống"}</small>
          </div>
        </div>
        <div className="appearance-selection-actions">
          <button type="button" className="button secondary" onClick={onReset} disabled={!hasThemeDraft || working}>
            Hoàn tác
          </button>
          <button type="button" className="button primary" onClick={onApply} disabled={!hasThemeDraft || working}>
            <Icon name="save" size={15} />
            {working ? "Đang áp dụng…" : "Áp dụng toàn hệ thống"}
          </button>
        </div>
      </div>
    </section>
  );
}

type ServiceRow = {
  id: string;
  serviceType: string;
  configKey: string;
  enabled: boolean;
  config: Record<string, any>;
  secretConfigured: boolean;
  healthStatus: string;
  lastCheckedAt?: string;
  lastError?: string;
  updatedAt: string;
};

function WorldSettingsConsole({ user }: { user: PortalUser }) {
  const router = useRouter();
  const systemAdmin = user.accountType === "SYSTEM_ADMIN";
  const canBrand = systemAdmin || user.permissions.includes("branding:manage");
  const canServices =
    systemAdmin ||
    user.permissions.some((permission) =>
      ["configuration:manage", "integrations:manage"].includes(permission),
    );
  const { data, loading, error, refresh } = useLoad(
    async () => ({
      branding: canBrand
        ? await apiRequest<BrandingRow>("/api/v1/branding")
        : null,
      services: canServices
        ? await apiRequest<ServiceRow[]>("/api/v1/external-services")
        : [],
    }),
    [canBrand, canServices],
  );
  const [tab, setTab] = useState<"themes" | "brand" | "services">(
    canBrand ? "themes" : "services",
  );
  const [brand, setBrand] = useState<BrandingRow | null>(null);
  const [activeThemeKey, setActiveThemeKey] =
    useState<ThemeKey>("unified-light");
  const committedBrandRef = useRef<BrandingRow | null>(null);
  const [service, setService] = useState({
    serviceType: "REDIS",
    configKey: "default",
    enabled: true,
    host: "redis",
    port: "6379",
    username: "",
    database: "0",
    endpoint: "",
    model: "",
    bucket: "",
    secure: false,
    secret: "",
  });
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  useEffect(() => {
    if (!data?.branding) return;
    const normalized = {
      ...data.branding,
      themeKey: normalizeThemeKey(data.branding.themeKey),
    };
    setBrand(normalized);
    setActiveThemeKey(normalized.themeKey);
    committedBrandRef.current = normalized;
    applyBrandingPreview(normalized);
  }, [data?.branding]);
  useEffect(
    () => () => {
      if (committedBrandRef.current) {
        applyBrandingPreview(committedBrandRef.current);
      }
    },
    [],
  );
  useEffect(() => {
    if (!canBrand && canServices) setTab("services");
  }, [canBrand, canServices]);
  const services = data?.services ?? [];
  const committedBrand = committedBrandRef.current;
  const hasThemeDraft = Boolean(
    brand &&
      committedBrand &&
      (brand.themeKey !== committedBrand.themeKey ||
        brand.primaryColor !== committedBrand.primaryColor ||
        brand.secondaryColor !== committedBrand.secondaryColor ||
        brand.backgroundColor !== committedBrand.backgroundColor ||
        brand.textColor !== committedBrand.textColor),
  );

  async function persistBranding(nextBrand: BrandingRow, message: string) {
    if (!canBrand) return;
    setWorking(true);
    try {
      const saved = await apiRequest<BrandingRow>("/api/v1/branding", {
        method: "PUT",
        body: JSON.stringify({
          systemName: nextBrand.systemName,
          introduction: nextBrand.introduction || null,
          logoFileId: nextBrand.logoFileId || null,
          faviconFileId: nextBrand.faviconFileId || null,
          backgroundFileId: nextBrand.backgroundFileId || null,
          themeKey: normalizeThemeKey(nextBrand.themeKey),
          primaryColor: nextBrand.primaryColor,
          secondaryColor: nextBrand.secondaryColor,
          backgroundColor: nextBrand.backgroundColor,
          textColor: nextBrand.textColor,
          customDomain: nextBrand.customDomain || null,
        }),
      });
      const normalized = {
        ...saved,
        themeKey: normalizeThemeKey(saved.themeKey),
      };
      setBrand(normalized);
      setActiveThemeKey(normalized.themeKey);
      committedBrandRef.current = normalized;
      applyBrandingPreview(normalized);
      setNotice({ tone: "success", message });
      router.refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể lưu thương hiệu",
      });
    } finally {
      setWorking(false);
    }
  }

  async function saveBrand(event: FormEvent) {
    event.preventDefault();
    if (!brand) return;
    await persistBranding(
      brand,
      "Đã cập nhật nhận diện và đồng bộ giao diện toàn hệ thống.",
    );
  }

  function previewTheme(theme: ThemeDefinition) {
    if (!brand) return;
    const nextBrand: BrandingRow = {
      ...brand,
      themeKey: normalizeThemeKey(theme.key),
      primaryColor: theme.palette.primary,
      secondaryColor: theme.palette.secondary,
      backgroundColor: theme.palette.background,
      textColor: theme.palette.text,
    };
    setBrand(nextBrand);
    applyBrandingPreview(nextBrand);
    setNotice({
      tone: "info",
      message: `Đang xem thử “${theme.name}”. Chưa có người dùng nào khác bị ảnh hưởng.`,
    });
  }

  function resetThemePreview() {
    if (!committedBrandRef.current) return;
    setBrand(committedBrandRef.current);
    applyBrandingPreview(committedBrandRef.current);
    setNotice({ tone: "info", message: "Đã hoàn tác bản xem thử." });
  }

  async function applySelectedTheme() {
    if (!brand) return;
    const theme = getTheme(brand.themeKey);
    await persistBranding(
      brand,
      `Đã áp dụng “${theme.name}” cho toàn hệ thống.`,
    );
  }

  async function addService(event: FormEvent) {
    event.preventDefault();
    if (!canServices) return;
    setWorking(true);
    try {
      const config =
        service.serviceType === "REDIS"
          ? {
              host: service.host,
              port: Number(service.port || 6379),
              username: service.username || undefined,
              database: Number(service.database || 0),
              tls: service.secure,
            }
          : service.serviceType === "SMTP"
            ? {
                host: service.host,
                port: Number(service.port || 587),
                username: service.username || undefined,
                secure: service.secure,
              }
            : service.serviceType === "AI_PROVIDER"
              ? { endpoint: service.endpoint, model: service.model }
              : service.serviceType === "OBJECT_STORAGE"
                ? { endpoint: service.endpoint, bucket: service.bucket, secure: service.secure }
                : { endpoint: service.endpoint, secure: service.secure };
      await apiRequest("/api/v1/external-services", {
        method: "POST",
        body: JSON.stringify({
          serviceType: service.serviceType,
          configKey: service.configKey,
          enabled: service.enabled,
          config,
          secret: service.secret || null,
        }),
      });
      setNotice({
        tone: "success",
        message: `Đã lưu cấu hình ${service.serviceType}.`,
      });
      setService((current) => ({ ...current, secret: "" }));
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Cấu hình không hợp lệ",
      });
    } finally {
      setWorking(false);
    }
  }

  async function testService(id: string) {
    if (!canServices) return;
    setWorking(true);
    try {
      const result = await apiRequest<ServiceRow>(
        `/api/v1/external-services/${id}/test`,
        { method: "POST" },
      );
      setNotice({
        tone: result.healthStatus === "HEALTHY" ? "success" : "error",
        message:
          result.healthStatus === "HEALTHY"
            ? "Kết nối dịch vụ thành công."
            : (result.lastError ?? "Dịch vụ chưa sẵn sàng"),
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể thử kết nối",
      });
    } finally {
      setWorking(false);
    }
  }

  return (
    <>
      <WorkspaceHero
        eyebrow="CÀI ĐẶT HỆ THỐNG"
        title="Giao diện và cài đặt"
        description="Chọn sáng hoặc tối, thiết lập màu thương hiệu an toàn và chỉ kết nối dịch vụ ngoài khi tổ chức thực sự sử dụng."
        icon={<Icon name="settings" size={31} />}
        stats={[
          {
            value: brand ? getTheme(brand.themeKey).shortName : "—",
            label: "Chế độ hiện tại",
          },
          {
            value: THEMES.length,
            label: "Chế độ khả dụng",
          },
          {
            value: services.filter((item) => item.healthStatus === "HEALTHY")
              .length,
            label: "Kết nối khỏe",
            tone: "teal",
          },
        ]}
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      <nav className="workspace-tabs">
        {canBrand && (
          <button
            className={tab === "themes" ? "active" : ""}
            onClick={() => setTab("themes")}
          >
            Giao diện
          </button>
        )}
        {canBrand && (
          <button
            className={tab === "brand" ? "active" : ""}
            onClick={() => setTab("brand")}
          >
            Nhận diện
          </button>
        )}
        {canServices && (
          <button
            className={tab === "services" ? "active" : ""}
            onClick={() => setTab("services")}
          >
            Dịch vụ ngoài
          </button>
        )}
      </nav>
      {loading ? (
        <Busy />
      ) : error ? (
        <NoticeBar
          notice={{ tone: "error", message: error }}
          onClose={() => void refresh()}
        />
      ) : tab === "themes" && canBrand && brand ? (
        <AppearanceStudio
          brand={brand}
          activeThemeKey={activeThemeKey}
          hasThemeDraft={hasThemeDraft}
          working={working}
          onPreview={previewTheme}
          onReset={resetThemePreview}
          onApply={() => void applySelectedTheme()}
        />
      ) : tab === "brand" && canBrand && brand ? (
        <div className="workspace-two-column wide-left">
          <Panel
            title="Bộ nhận diện"
            subtitle="Chỉ chọn màu chính và màu nền. Màu chữ được tính tự động để luôn dễ đọc."
          >
            <form className="workspace-form branding-form" onSubmit={saveBrand}>
              <Field label="Tên hệ thống" wide>
                <input
                  required
                  value={brand.systemName}
                  onChange={(event) =>
                    setBrand({ ...brand, systemName: event.target.value })
                  }
                />
              </Field>
              <Field label="Nội dung giới thiệu" wide>
                <textarea
                  value={brand.introduction ?? ""}
                  onChange={(event) =>
                    setBrand({ ...brand, introduction: event.target.value })
                  }
                />
              </Field>
              <Field label="Logo file ID">
                <input
                  value={brand.logoFileId ?? ""}
                  onChange={(event) =>
                    setBrand({ ...brand, logoFileId: event.target.value })
                  }
                />
              </Field>
              <Field label="Favicon file ID">
                <input
                  value={brand.faviconFileId ?? ""}
                  onChange={(event) =>
                    setBrand({ ...brand, faviconFileId: event.target.value })
                  }
                />
              </Field>
              <Field label="Ảnh nền file ID">
                <input
                  value={brand.backgroundFileId ?? ""}
                  onChange={(event) =>
                    setBrand({ ...brand, backgroundFileId: event.target.value })
                  }
                />
              </Field>
              <Field label="Tên miền">
                <input
                  value={brand.customDomain ?? ""}
                  onChange={(event) =>
                    setBrand({ ...brand, customDomain: event.target.value })
                  }
                  placeholder="learn.example.vn"
                />
              </Field>
              <div className="color-fields">
                {(
                  [
                    ["primaryColor", "Màu chính"],
                    ["backgroundColor", "Màu nền"],
                  ] as const
                ).map(([key, label]) => (
                  <Field label={label} key={key}>
                    <div className="color-input">
                      <input
                        type="color"
                        value={brand[key].slice(0, 7)}
                        onChange={(event) =>
                          setBrand({
                            ...brand,
                            [key]: event.target.value.toUpperCase(),
                            ...(key === "backgroundColor"
                              ? { textColor: readableText(event.target.value) }
                              : {}),
                          })
                        }
                      />
                      <input
                        value={brand[key]}
                        onChange={(event) =>
                          setBrand({
                            ...brand,
                            [key]: event.target.value.toUpperCase(),
                            ...(key === "backgroundColor"
                              ? { textColor: readableText(event.target.value) }
                              : {}),
                          })
                        }
                      />
                    </div>
                  </Field>
                ))}
              </div>
              <Button type="submit" disabled={working}>
                <Icon name="save" size={16} />{" "}
                {working ? "Đang lưu…" : "Lưu nhận diện"}
              </Button>
            </form>
          </Panel>
          <Panel
            title="Xem trước không gian"
            subtitle="Bản xem trước phản ánh màu thương hiệu nhưng vẫn giữ độ tương phản cần thiết."
            className="sticky-panel"
          >
            <div
              className="branding-preview"
              style={{
                background: brand.backgroundColor,
                color: brand.textColor,
                borderColor: `${brand.primaryColor}66`,
              }}
            >
              <header>
                <span
                  style={{
                    background: brand.primaryColor,
                    color: readableText(brand.primaryColor),
                  }}
                >
                  L
                </span>
                <strong>{brand.systemName}</strong>
              </header>
              <h3>Không gian học tập rõ ràng và nhất quán.</h3>
              <p>
                {brand.introduction || "Không gian học tập riêng của tổ chức."}
              </p>
              <button
                type="button"
                style={{
                  background: brand.primaryColor,
                  color: readableText(brand.primaryColor),
                }}
              >
                Bắt đầu học
              </button>
              <div className="preview-cards">
                <i />
                <i />
                <i />
              </div>
            </div>
          </Panel>
        </div>
      ) : canServices ? (
        <div className="workspace-two-column wide-left">
          <Panel
            title="Dịch vụ đã cấu hình"
            subtitle="Chỉ các kết nối đang dùng mới cần được cấu hình."
          >
            {services.length ? (
              <div className="service-grid">
                {services.map((item) => (
                  <div className="service-card" key={item.id}>
                    <div>
                      <span
                        className={`service-orb ${item.healthStatus.toLowerCase()}`}
                      >
                        <Icon
                          name={
                            item.serviceType === "REDIS"
                              ? "operations"
                              : item.serviceType === "AI_PROVIDER"
                                ? "question"
                                : item.serviceType === "DOCUMENT_EDITOR"
                                  ? "edit"
                                  : "settings"
                          }
                          size={20}
                        />
                      </span>
                      <div>
                        <strong>{item.serviceType}</strong>
                        <small>{item.configKey}</small>
                      </div>
                      <Tag tone={item.enabled ? "teal" : ""}>
                        {item.enabled ? "ENABLED" : "OFF"}
                      </Tag>
                    </div>
                    <dl>
                      {Object.entries(item.config)
                        .slice(0, 4)
                        .map(([key, value]) => (
                          <div key={key}>
                            <dt>{key}</dt>
                            <dd>{String(value)}</dd>
                          </div>
                        ))}
                    </dl>
                    {item.lastError && (
                      <p className="service-error">{item.lastError}</p>
                    )}
                    <footer>
                      <Tag
                        tone={
                          item.healthStatus === "HEALTHY"
                            ? "teal"
                            : item.healthStatus === "UNREACHABLE"
                              ? "danger"
                              : "gold"
                        }
                      >
                        {item.healthStatus}
                      </Tag>
                      <Button
                        tone="ghost"
                        disabled={working}
                        onClick={() => void testService(item.id)}
                      >
                        <Icon name="refresh" size={14} /> Thử kết nối
                      </Button>
                    </footer>
                  </div>
                ))}
              </div>
            ) : (
              <Empty text="Chưa cấu hình dịch vụ ngoài. Hệ thống lõi vẫn hoạt động độc lập." />
            )}
          </Panel>
          <Panel
            title="Kết nối dịch vụ"
            subtitle="Điền từng trường riêng; không cần viết JSON hoặc nhớ cấu trúc kỹ thuật."
          >
            <form className="workspace-form" onSubmit={addService}>
              <Field label="Loại dịch vụ">
                <select
                  value={service.serviceType}
                  onChange={(event) =>
                    setService({ ...service, serviceType: event.target.value })
                  }
                >
                  {[
                    "REDIS",
                    "SMTP",
                    "VIDEO_CONFERENCE",
                    "AI_PROVIDER",
                    "OBJECT_STORAGE",
                    "DOCUMENT_EDITOR",
                  ].map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </Field>
              <Field label="Tên cấu hình">
                <input
                  required
                  value={service.configKey}
                  onChange={(event) =>
                    setService({ ...service, configKey: event.target.value })
                  }
                />
              </Field>
              {(["REDIS", "SMTP"].includes(service.serviceType)) && (
                <>
                  <Field label="Máy chủ">
                    <input
                      required
                      value={service.host}
                      onChange={(event) => setService({ ...service, host: event.target.value })}
                      placeholder={service.serviceType === "REDIS" ? "redis" : "smtp.example.vn"}
                    />
                  </Field>
                  <Field label="Cổng">
                    <input
                      type="number"
                      min="1"
                      max="65535"
                      value={service.port}
                      onChange={(event) => setService({ ...service, port: event.target.value })}
                    />
                  </Field>
                  <Field label="Tên đăng nhập">
                    <input
                      value={service.username}
                      onChange={(event) => setService({ ...service, username: event.target.value })}
                    />
                  </Field>
                  {service.serviceType === "REDIS" && (
                    <Field label="Database">
                      <input
                        type="number"
                        min="0"
                        value={service.database}
                        onChange={(event) => setService({ ...service, database: event.target.value })}
                      />
                    </Field>
                  )}
                </>
              )}
              {service.serviceType === "AI_PROVIDER" && (
                <>
                  <Field label="Endpoint" wide>
                    <input
                      required
                      type="url"
                      value={service.endpoint}
                      onChange={(event) => setService({ ...service, endpoint: event.target.value })}
                      placeholder="https://api.example.vn/v1"
                    />
                  </Field>
                  <Field label="Tên model" wide>
                    <input
                      required
                      value={service.model}
                      onChange={(event) => setService({ ...service, model: event.target.value })}
                    />
                  </Field>
                </>
              )}
              {service.serviceType === "OBJECT_STORAGE" && (
                <>
                  <Field label="Endpoint" wide>
                    <input
                      required
                      value={service.endpoint}
                      onChange={(event) => setService({ ...service, endpoint: event.target.value })}
                    />
                  </Field>
                  <Field label="Bucket" wide>
                    <input
                      required
                      value={service.bucket}
                      onChange={(event) => setService({ ...service, bucket: event.target.value })}
                    />
                  </Field>
                </>
              )}
              {["VIDEO_CONFERENCE", "DOCUMENT_EDITOR"].includes(service.serviceType) && (
                <Field label="Địa chỉ dịch vụ" wide>
                  <input
                    required
                    type="url"
                    value={service.endpoint}
                    onChange={(event) => setService({ ...service, endpoint: event.target.value })}
                    placeholder="https://service.example.vn"
                  />
                </Field>
              )}
              <label className="workspace-check">
                <input
                  type="checkbox"
                  checked={service.secure}
                  onChange={(event) => setService({ ...service, secure: event.target.checked })}
                />
                <span>Dùng kết nối bảo mật TLS/HTTPS</span>
              </label>
              <Field label="Secret/API key" wide>
                <input
                  type="password"
                  value={service.secret}
                  onChange={(event) =>
                    setService({ ...service, secret: event.target.value })
                  }
                  placeholder="Được mã hóa trước khi lưu"
                />
              </Field>
              <label className="workspace-check">
                <input
                  type="checkbox"
                  checked={service.enabled}
                  onChange={(event) =>
                    setService({ ...service, enabled: event.target.checked })
                  }
                />
                <span>Bật cấu hình này</span>
              </label>
              <Button type="submit" disabled={working}>
                <Icon name="plus" size={16} /> Lưu kết nối
              </Button>
            </form>
          </Panel>
        </div>
      ) : (
        <WorkspaceDenied />
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
  const manage =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("grade-appeals:manage");
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
