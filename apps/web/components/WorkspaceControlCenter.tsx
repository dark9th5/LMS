"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type {
  CSSProperties,
  FormEvent,
  MouseEventHandler,
  ReactNode,
} from "react";
import { apiRequest, createIdempotencyKey, unwrapItems } from "@/lib/api";
import { readableText } from "@/lib/color";
import {
  getTheme,
  normalizeThemeKey,
  THEME_CATEGORIES,
  THEMES,
  type ThemeCategory,
  type ThemeDefinition,
  type ThemeKey,
  type ThemeMode,
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

function Busy({ label = "Đang triệu hồi dữ liệu…" }: { label?: string }) {
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
  const canManageRoles = systemAdmin || user.permissions.includes("roles:manage");
  const { data, loading, error, refresh } = useLoad(async () => {
    const [users, roles, catalog] = await Promise.all([
      canReadUsers
        ? apiRequest<UserRow[] | { items: UserRow[] }>("/api/v1/users?size=1000")
        : Promise.resolve([] as UserRow[]),
      canReadRoles
        ? apiRequest<RoleRow[]>("/api/v1/roles")
        : Promise.resolve([] as RoleRow[]),
      apiRequest<PermissionCatalog>("/api/v1/authorization/catalog"),
    ]);
    return { users: unwrapItems(users), roles, catalog };
  }, [canReadUsers, canReadRoles]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [tab, setTab] = useState<
    "accounts" | "grant" | "inspect" | "roles" | "bulk"
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
  const [role, setRole] = useState({
    code: "",
    name: "",
    permissions: new Set<string>(),
  });
  const [bulkText, setBulkText] = useState(
    "Mã,Tài khoản,Họ tên,Email,Mật khẩu,Gói quyền\n",
  );
  const [inspectedUserId, setInspectedUserId] = useState("");
  const [assignments, setAssignments] = useState<AssignmentBundle | null>(null);
  const [explanation, setExplanation] = useState<AuthorizationExplanation | null>(null);
  const [inspectionLoading, setInspectionLoading] = useState(false);

  const users = data?.users ?? [];
  const roles = data?.roles ?? [];
  const catalog = data?.catalog;
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
    key: "accounts" | "grant" | "inspect" | "roles" | "bulk";
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

  async function createRole(event: FormEvent) {
    event.preventDefault();
    if (!canManageRoles) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/roles", {
        method: "POST",
        body: JSON.stringify({
          code: role.code.toUpperCase(),
          name: role.name,
          permissions: Array.from(role.permissions),
        }),
      });
      setNotice({ tone: "success", message: `Đã tạo gói quyền ${role.code.toUpperCase()}.` });
      setRole({ code: "", name: "", permissions: new Set() });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể tạo gói quyền",
      });
    } finally {
      setWorking(false);
    }
  }

  async function importBulk(event: FormEvent) {
    event.preventDefault();
    if (!canWrite) return;
    const rows = bulkText
      .split(/\r?\n/)
      .slice(1)
      .map((line) => line.split(",").map((cell) => cell.trim()))
      .filter((cells) => cells[0] && cells[1]);
    if (!rows.length) {
      setNotice({ tone: "error", message: "Chưa có dòng tài khoản hợp lệ." });
      return;
    }
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/users/bulk", {
        method: "POST",
        body: JSON.stringify({
          operationId: createIdempotencyKey("accounts"),
          users: rows.map(
            ([code, username, fullName, email, password, roleCodes]) => ({
              code,
              username,
              fullName,
              email: email || null,
              password,
              organizationUnitId: null,
              roleCodes: (roleCodes || "BASIC_USER")
                .split("|")
                .map((value) => value.trim())
                .filter(Boolean),
            }),
          ),
        }),
      });
      setNotice({ tone: "success", message: `Đã tạo ${rows.length} tài khoản từ danh sách.` });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể nhập tài khoản",
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
                      <Field label="Mã phạm vi" wide hint="Dán mã của chi nhánh, phòng ban, nhóm, khóa học hoặc kỳ thi đã chọn.">
                        <input required value={grant.scopeId} onChange={(event) => { setGrant({ ...grant, scopeId: event.target.value }); setPreview(null); }} />
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

          {tab === "roles" && (
            <div className="workspace-two-column wide-left">
              <Panel title="Gói quyền hiện có" subtitle="Gói hệ thống là mẫu theo công việc và chỉ đọc; quản trị có thể tạo gói riêng từ các quyền chi tiết.">
                <div className="role-card-grid">
                  {roles.map((item) => {
                    const profile = profileByCode.get(item.code);
                    return (
                      <div className="role-card" key={item.id}>
                        <div>
                          <span className="role-symbol">{item.name.slice(0, 1)}</span>
                          <div><strong>{profile?.name ?? item.name}</strong><small>{item.code}</small></div>
                          {item.systemRole && <Tag tone="gold">Mẫu hệ thống</Tag>}
                        </div>
                        <p>{profile?.description ?? `${item.permissions.length} quyền được ghép.`}</p>
                        <div className="workspace-tags compact">
                          {item.permissions.slice(0, 5).map((permission) => <Tag key={permission}>{permissionByCode.get(permission)?.label ?? permission}</Tag>)}
                          {item.permissions.length > 5 && <Tag>+{item.permissions.length - 5}</Tag>}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </Panel>
              {canManageRoles && (
                <Panel title="Tạo gói quyền tùy chỉnh" subtitle="Chọn năng lực tối thiểu cần thiết. Phạm vi cụ thể được đặt khi gán gói cho người dùng.">
                  <form className="workspace-form" onSubmit={createRole}>
                    <Field label="Mã gói">
                      <input required value={role.code} onChange={(event) => setRole({ ...role, code: event.target.value.toUpperCase().replace(/\s+/g, "_") })} placeholder="COURSE_REVIEWER" />
                    </Field>
                    <Field label="Tên hiển thị">
                      <input required value={role.name} onChange={(event) => setRole({ ...role, name: event.target.value })} />
                    </Field>
                    <div className="permission-matrix">
                      {Object.entries(catalog?.groups ?? {}).map(([group, permissions]) => (
                        <details key={group}>
                          <summary><span>{group}</span><small>{permissions.filter((permission) => role.permissions.has(permission)).length}/{permissions.length}</small></summary>
                          {permissions.map((permission) => {
                            const definition = permissionByCode.get(permission);
                            return (
                              <label key={permission}>
                                <input type="checkbox" checked={role.permissions.has(permission)} onChange={(event) => setRole((current) => {
                                  const next = new Set(current.permissions);
                                  event.target.checked ? next.add(permission) : next.delete(permission);
                                  return { ...current, permissions: next };
                                })} />
                                <span><strong>{definition?.label ?? permission}</strong><small className="block">{definition?.description ?? permission} · {definition?.risk ?? "LOW"}</small></span>
                              </label>
                            );
                          })}
                        </details>
                      ))}
                    </div>
                    <Button type="submit" disabled={working || !role.permissions.size}>
                      <Icon name="save" size={16} /> Tạo gói quyền
                    </Button>
                  </form>
                </Panel>
              )}
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

function ThemeStudio({
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
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<ThemeCategory | "all">("all");
  const [mode, setMode] = useState<ThemeMode | "all">("all");
  const selectedTheme = getTheme(brand.themeKey);
  const hasPreview = hasThemeDraft;
  const normalizedQuery = query.trim().toLocaleLowerCase("vi");
  const filteredThemes = useMemo(
    () =>
      THEMES.filter((theme) => {
        if (category !== "all" && theme.category !== category) return false;
        if (mode !== "all" && theme.mode !== mode) return false;
        if (!normalizedQuery) return true;
        const haystack = [
          theme.name,
          theme.shortName,
          theme.description,
          ...theme.tags,
        ]
          .join(" ")
          .toLocaleLowerCase("vi");
        return haystack.includes(normalizedQuery);
      }),
    [category, mode, normalizedQuery],
  );

  return (
    <section className="theme-studio" aria-labelledby="theme-studio-title">
      <header className="theme-studio-intro">
        <div>
          <span className="theme-studio-kicker">HỆ THỐNG GIAO DIỆN THỐNG NHẤT</span>
          <h2 id="theme-studio-title">Chế độ hiển thị</h2>
          <p>
            Toàn hệ thống dùng chung một kiểu chữ, khoảng cách, component và bố cục.
            Bạn chỉ chọn chế độ sáng hoặc tối; màu thương hiệu được chỉnh riêng và
            luôn được hệ thống tính màu chữ tương phản.
          </p>
        </div>
        <div className="theme-studio-count" aria-label="Hai chế độ hiển thị">
          <strong>{THEMES.length}</strong>
          <small>chế độ hiển thị</small>
        </div>
      </header>

      <div className="theme-studio-toolbar">
        <label className="theme-search">
          <Icon name="search" size={17} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm chế độ sáng hoặc tối"
            aria-label="Tìm chế độ hiển thị"
          />
        </label>
        <div className="theme-mode-switch" aria-label="Lọc theo chế độ màu">
          {(
            [
              ["all", "Mọi chế độ"],
              ["dark", "Nền tối"],
              ["light", "Nền sáng"],
            ] as const
          ).map(([value, label]) => (
            <button
              type="button"
              key={value}
              className={`theme-filter ${mode === value ? "active" : ""}`}
              onClick={() => setMode(value)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="theme-filters" aria-label="Lọc theo nhóm chủ đề">
        {(Object.entries(THEME_CATEGORIES) as Array<
          [ThemeCategory | "all", string]
        >).map(([value, label]) => (
          <button
            type="button"
            key={value}
            className={`theme-filter ${category === value ? "active" : ""}`}
            onClick={() => setCategory(value)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="theme-gallery" aria-live="polite">
        {filteredThemes.length ? (
          filteredThemes.map((theme) => {
            const selected = theme.key === selectedTheme.key;
            const active = theme.key === activeThemeKey;
            const previewStyle = {
              "--preview-primary": theme.palette.primary,
              "--preview-secondary": theme.palette.secondary,
              "--preview-background": theme.palette.background,
              "--preview-surface": theme.palette.surface,
              "--preview-text": theme.palette.text,
            } as CSSProperties;
            return (
              <article
                className={`theme-card ${selected ? "selected" : ""}`}
                key={theme.key}
              >
                <div
                  className="theme-card-visual"
                  data-preview-theme={theme.key}
                  style={previewStyle}
                >
                  <div className="theme-preview-rail" aria-hidden>
                    <b />
                    <i className="active" />
                    <i />
                    <i />
                    <i />
                  </div>
                  <div className="theme-preview-canvas" aria-hidden>
                    <div className="theme-preview-topbar">
                      <span />
                      <i />
                    </div>
                    <div className="theme-preview-hero">
                      <small>{theme.shortName.toUpperCase()} / 01</small>
                      <strong>Knowledge System</strong>
                    </div>
                    <div className="theme-preview-modules">
                      <i />
                      <i />
                      <i />
                    </div>
                  </div>
                </div>
                <div className="theme-card-body">
                  <div className="theme-card-heading">
                    <div>
                      <small>
                        {THEME_CATEGORIES[theme.category]} · {theme.mode}
                      </small>
                      <h3>{theme.name}</h3>
                    </div>
                    <span className="theme-card-state">
                      {active && !hasPreview
                        ? "Đang dùng"
                        : selected
                          ? "Đang xem"
                          : "Preset"}
                    </span>
                  </div>
                  <p>{theme.description}</p>
                  <div className="theme-tags">
                    {theme.tags.map((tag) => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                  <div className="theme-card-actions">
                    <button
                      type="button"
                      className={`button ${selected ? "primary" : "secondary"}`}
                      onClick={() => onPreview(theme)}
                      aria-pressed={selected}
                    >
                      <Icon name={selected ? "check" : "eye"} size={15} />
                      {selected ? "Đang xem trực tiếp" : "Xem thử toàn trang"}
                    </button>
                  </div>
                </div>
              </article>
            );
          })
        ) : (
          <div className="theme-empty">
            Không có chế độ phù hợp. Hãy đổi từ khóa.
          </div>
        )}
      </div>

      <div className="theme-selection-bar">
        <div className="theme-selection-copy">
          <span
            className="theme-selection-swatch"
            style={
              {
                "--selected-primary": selectedTheme.palette.primary,
                "--selected-secondary": selectedTheme.palette.secondary,
              } as CSSProperties
            }
          />
          <div>
            <strong>{selectedTheme.name}</strong>
            <small>
              {hasPreview
                ? "Đang xem thử · chưa ảnh hưởng người dùng khác"
                : "Chế độ đang áp dụng cho toàn hệ thống"}
            </small>
          </div>
        </div>
        <div className="theme-selection-actions">
          <button
            type="button"
            className="button secondary"
            onClick={onReset}
            disabled={!hasPreview || working}
          >
            Hoàn tác xem thử
          </button>
          <button
            type="button"
            className="button primary"
            onClick={onApply}
            disabled={!hasPreview || working}
          >
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
        eyebrow="APPEARANCE / SYSTEM IDENTITY"
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
        <ThemeStudio
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
                {working ? "Đang khắc ghi…" : "Lưu nhận diện"}
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
                background: `radial-gradient(circle at 70% 15%, ${brand.primaryColor}33, transparent 38%), ${brand.backgroundColor}`,
                color: brand.textColor,
                borderColor: `${brand.primaryColor}66`,
              }}
            >
              <div className="preview-stars">✦ · · ◇ · ✧</div>
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

// ─────────────────────────────────────────────────────────────────────────────
// News
// ─────────────────────────────────────────────────────────────────────────────

type NewsRow = {
  id: string;
  title: string;
  summary?: string;
  contentHtml: string;
  status: string;
  audienceType: string;
  audienceId?: string;
  pinned: boolean;
  priority: number;
  acknowledgementRequired: boolean;
  publishFrom?: string;
  publishUntil?: string;
  attachmentFileIds: string[];
  read: boolean;
  acknowledged: boolean;
  authorId: string;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
};

function NewsConsole({ user }: { user: PortalUser }) {
  const manage =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("news:manage");
  const { data, loading, error, refresh } = useLoad(
    () => apiRequest<NewsRow[]>(manage ? "/api/v1/news" : "/api/v1/news/feed"),
    [manage],
  );
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [draft, setDraft] = useState({
    title: "",
    summary: "",
    contentHtml: "",
    audienceType: "SYSTEM",
    audienceId: "",
    pinned: false,
    priority: 0,
    acknowledgementRequired: false,
    publishFrom: "",
    publishUntil: "",
  });
  const [newsAttachments, setNewsAttachments] = useState<
    Array<{ id: string; originalName: string }>
  >([]);
  async function uploadNewsAttachment(file?: File) {
    if (!file) return;
    setWorking(true);
    try {
      const body = new FormData();
      body.set("file", file);
      const saved = await apiRequest<{ id: string; originalName: string }>(
        "/api/v1/files?purpose=NEWS_ATTACHMENT",
        { method: "POST", body },
      );
      setNewsAttachments((current) =>
        [...current.filter((item) => item.id !== saved.id), saved].slice(0, 20),
      );
      setNotice({
        tone: "success",
        message: `Đã tải tệp ${saved.originalName}.`,
      });
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể tải tệp đính kèm",
      });
    } finally {
      setWorking(false);
    }
  }
  async function createNews(event: FormEvent) {
    event.preventDefault();
    setWorking(true);
    try {
      await apiRequest("/api/v1/news", {
        method: "POST",
        body: JSON.stringify({
          ...draft,
          audienceId: draft.audienceType === "SYSTEM" ? null : draft.audienceId,
          publishFrom: draft.publishFrom
            ? new Date(draft.publishFrom).toISOString()
            : null,
          publishUntil: draft.publishUntil
            ? new Date(draft.publishUntil).toISOString()
            : null,
          attachmentFileIds: newsAttachments.map((item) => item.id),
        }),
      });
      setDraft({
        title: "",
        summary: "",
        contentHtml: "",
        audienceType: "SYSTEM",
        audienceId: "",
        pinned: false,
        priority: 0,
        acknowledgementRequired: false,
        publishFrom: "",
        publishUntil: "",
      });
      setNewsAttachments([]);
      setNotice({ tone: "success", message: "Đã lưu tin ở trạng thái nháp." });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể tạo tin",
      });
    } finally {
      setWorking(false);
    }
  }
  async function action(
    id: string,
    type: "publish" | "archive" | "read" | "acknowledge",
  ) {
    setWorking(true);
    try {
      await apiRequest(`/api/v1/news/${id}/${type}`, {
        method: type === "read" || type === "acknowledge" ? "PUT" : "POST",
      });
      setNotice({
        tone: "success",
        message:
          type === "publish"
            ? "Tin đã được phát hành."
            : type === "archive"
              ? "Tin đã được lưu trữ."
              : "Đã cập nhật trạng thái đọc.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể cập nhật tin",
      });
    } finally {
      setWorking(false);
    }
  }
  const rows = data ?? [];
  return (
    <>
      <WorkspaceHero
        eyebrow="ĐÀI TRUYỀN TIN"
        title="Tin tức & thông báo tập thể"
        description="Phát thông tin cho toàn hệ thống hoặc đúng chi nhánh, phòng ban, nhóm; hỗ trợ ghim, lịch phát và yêu cầu xác nhận đã đọc."
        icon={<Icon name="bell" size={31} />}
        stats={[
          {
            value: rows.length,
            label: manage ? "Tổng tin" : "Tin dành cho bạn",
          },
          { value: rows.filter((v) => v.pinned).length, label: "Tin ghim" },
          {
            value: rows.filter((v) => v.status === "DRAFT").length,
            label: "Bản nháp",
          },
          {
            value: rows.filter(
              (v) => v.acknowledgementRequired && !v.acknowledged,
            ).length,
            label: "Chờ xác nhận",
            tone: "gold",
          },
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
        <div
          className={`workspace-two-column ${manage ? "wide-left" : "single"}`}
        >
          <Panel
            title={manage ? "Kho tin tức" : "Thông báo dành cho bạn"}
            subtitle="Nội dung mới nhất, tin ghim luôn được đưa lên trước."
          >
            {rows.length ? (
              <div className="news-list">
                {rows.map((item) => (
                  <article
                    className={`news-card ${item.pinned ? "pinned" : ""}`}
                    key={item.id}
                  >
                    <header>
                      <span className="news-sigil">
                        {item.pinned ? "✦" : "◇"}
                      </span>
                      <div>
                        <div className="workspace-tags">
                          <Tag
                            tone={
                              item.status === "PUBLISHED"
                                ? "teal"
                                : item.status === "DRAFT"
                                  ? "gold"
                                  : ""
                            }
                          >
                            {item.status}
                          </Tag>
                          <Tag>{item.audienceType}</Tag>
                          {item.acknowledgementRequired && (
                            <Tag tone="violet">Cần xác nhận</Tag>
                          )}
                        </div>
                        <h3>{item.title}</h3>
                        <small>
                          {new Date(
                            item.publishedAt ?? item.createdAt,
                          ).toLocaleString("vi-VN")}
                        </small>
                      </div>
                    </header>
                    {item.summary && <p>{item.summary}</p>}
                    <div
                      className="news-content"
                      dangerouslySetInnerHTML={{ __html: item.contentHtml }}
                    />
                    {item.attachmentFileIds?.length ? (
                      <div className="workspace-tags">
                        {item.attachmentFileIds.map((fileId, index) => (
                          <a
                            className="workspace-button ghost"
                            href={`/api/gateway/api/v1/files/${fileId}/content`}
                            target="_blank"
                            rel="noreferrer"
                            key={fileId}
                          >
                            <Icon name="download" size={14} /> Tệp {index + 1}
                          </a>
                        ))}
                      </div>
                    ) : null}
                    <footer>
                      {manage ? (
                        <>
                          <Button
                            tone="ghost"
                            disabled={working || item.status === "PUBLISHED"}
                            onClick={() => void action(item.id, "publish")}
                          >
                            <Icon name="play" size={14} /> Phát hành
                          </Button>
                          <Button
                            tone="ghost"
                            disabled={working || item.status === "ARCHIVED"}
                            onClick={() => void action(item.id, "archive")}
                          >
                            <Icon name="trash" size={14} /> Lưu trữ
                          </Button>
                        </>
                      ) : (
                        <Button
                          tone={
                            item.acknowledgementRequired && !item.acknowledged
                              ? "primary"
                              : "ghost"
                          }
                          disabled={working || item.acknowledged}
                          onClick={() =>
                            void action(
                              item.id,
                              item.acknowledgementRequired
                                ? "acknowledge"
                                : "read",
                            )
                          }
                        >
                          <Icon name="check" size={14} />{" "}
                          {item.acknowledged
                            ? "Đã xác nhận"
                            : item.read
                              ? "Đã đọc"
                              : item.acknowledgementRequired
                                ? "Xác nhận đã đọc"
                                : "Đánh dấu đã đọc"}
                        </Button>
                      )}
                    </footer>
                  </article>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có tin tức trong phạm vi của bạn." />
            )}
          </Panel>
          {manage && (
            <Panel
              title="Soạn tin mới"
              subtitle="Nội dung HTML được lọc an toàn trước khi lưu."
              className="sticky-panel"
            >
              <form className="workspace-form" onSubmit={createNews}>
                <Field label="Tiêu đề" wide>
                  <input
                    required
                    value={draft.title}
                    onChange={(e) =>
                      setDraft({ ...draft, title: e.target.value })
                    }
                  />
                </Field>
                <Field label="Tóm tắt" wide>
                  <textarea
                    value={draft.summary}
                    onChange={(e) =>
                      setDraft({ ...draft, summary: e.target.value })
                    }
                  />
                </Field>
                <Field label="Nội dung" wide>
                  <textarea
                    className="tall"
                    required
                    value={draft.contentHtml}
                    onChange={(e) =>
                      setDraft({ ...draft, contentHtml: e.target.value })
                    }
                    placeholder="Cho phép p, br, strong, em, ul, ol, li, blockquote và h2-h4; không cho thuộc tính HTML."
                  />
                </Field>
                <Field
                  label="Tệp đính kèm"
                  wide
                  hint="Tối đa 20 tệp; quyền đọc được cấp theo đúng đối tượng nhận tin."
                >
                  <input
                    type="file"
                    disabled={working || newsAttachments.length >= 20}
                    onChange={(event) => {
                      void uploadNewsAttachment(event.target.files?.[0]);
                      event.currentTarget.value = "";
                    }}
                  />
                  {newsAttachments.map((item) => (
                    <span key={item.id}>
                      {item.originalName}{" "}
                      <button
                        type="button"
                        onClick={() =>
                          setNewsAttachments((current) =>
                            current.filter((value) => value.id !== item.id),
                          )
                        }
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </Field>
                <Field label="Đối tượng">
                  <select
                    value={draft.audienceType}
                    onChange={(e) =>
                      setDraft({
                        ...draft,
                        audienceType: e.target.value,
                        audienceId: "",
                      })
                    }
                  >
                    {["SYSTEM", "BRANCH", "DEPARTMENT", "GROUP"].map((v) => (
                      <option key={v}>{v}</option>
                    ))}
                  </select>
                </Field>
                {draft.audienceType !== "SYSTEM" && (
                  <Field label="Audience ID">
                    <input
                      required
                      value={draft.audienceId}
                      onChange={(e) =>
                        setDraft({ ...draft, audienceId: e.target.value })
                      }
                    />
                  </Field>
                )}
                <Field label="Bắt đầu phát">
                  <input
                    type="datetime-local"
                    value={draft.publishFrom}
                    onChange={(e) =>
                      setDraft({ ...draft, publishFrom: e.target.value })
                    }
                  />
                </Field>
                <Field label="Kết thúc phát">
                  <input
                    type="datetime-local"
                    value={draft.publishUntil}
                    onChange={(e) =>
                      setDraft({ ...draft, publishUntil: e.target.value })
                    }
                  />
                </Field>
                <label className="workspace-check">
                  <input
                    type="checkbox"
                    checked={draft.pinned}
                    onChange={(e) =>
                      setDraft({ ...draft, pinned: e.target.checked })
                    }
                  />
                  <span>Ghim lên đầu</span>
                </label>
                <label className="workspace-check">
                  <input
                    type="checkbox"
                    checked={draft.acknowledgementRequired}
                    onChange={(e) =>
                      setDraft({
                        ...draft,
                        acknowledgementRequired: e.target.checked,
                      })
                    }
                  />
                  <span>Yêu cầu xác nhận đã đọc</span>
                </label>
                <Button type="submit" disabled={working}>
                  <Icon name="save" size={16} /> Lưu bản nháp
                </Button>
              </form>
            </Panel>
          )}
        </div>
      )}
    </>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// AI question laboratory
// ─────────────────────────────────────────────────────────────────────────────

type AiProviderRow = {
  id: string;
  code: string;
  providerType: string;
  baseUrl: string;
  model: string;
  enabled: boolean;
  apiKeyConfigured: boolean;
  requestTimeoutSeconds: number;
  maxOutputTokens?: number;
  config: Record<string, any>;
  updatedAt: string;
};
type GenerationJobRow = {
  id: string;
  courseId: string;
  requestedBy: string;
  providerConfigId: string;
  documentFileIds: string[];
  options: Record<string, any>;
  status: string;
  questionSet?: {
    questions?: Array<{
      externalId: string;
      type: string;
      stem: string;
      options?: Array<{ id: string; text: string }>;
      correctOptionIds?: string[];
      difficulty?: "EASY" | "MEDIUM" | "HARD";
      points?: number;
      citations?: Array<{
        documentVersionId: string;
        page?: number;
        section?: string;
        quote?: string;
      }>;
    }>;
  };
  validationProblems: Array<{ path: string; message: string }>;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
};

function AiLabConsole({ user }: { user: PortalUser }) {
  const canApprove =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("questions:approve:ai");
  const canConfigure =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("configuration:manage");
  const { data, loading, error, refresh } = useLoad(
    async () => ({
      providers: await apiRequest<AiProviderRow[]>("/api/v1/ai/providers"),
      jobs: await apiRequest<GenerationJobRow[]>(
        "/api/v1/ai/question-generation-jobs",
      ),
    }),
    [],
  );
  const [tab, setTab] = useState<"generate" | "review" | "providers">(
    "generate",
  );
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [selectedJob, setSelectedJob] = useState<string>("");
  const [provider, setProvider] = useState({
    code: "LOCAL_AI",
    providerType: "LOCAL_OPENAI_COMPATIBLE",
    baseUrl: "http://host.docker.internal:11434/v1",
    model: "qwen3:8b",
    enabled: true,
    apiKey: "",
    requestTimeoutSeconds: 120,
    maxOutputTokens: 4096,
  });
  const [generate, setGenerate] = useState({
    courseId: "",
    providerConfigId: "",
    documentFileIds: "",
    sourceText: "",
    language: "vi",
    numberOfQuestions: 10,
    questionTypes: new Set(["SINGLE_CHOICE", "TRUE_FALSE"]),
  });
  const providers = data?.providers ?? [];
  const jobs = data?.jobs ?? [];
  const activeJob = jobs.find((job) => job.id === selectedJob) ?? jobs[0];
  useEffect(() => {
    if (!generate.providerConfigId && providers[0])
      setGenerate((current) => ({
        ...current,
        providerConfigId: providers[0].id,
      }));
  }, [providers, generate.providerConfigId]);

  async function saveProvider(event: FormEvent) {
    event.preventDefault();
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/ai/providers", {
        method: "POST",
        body: JSON.stringify({
          ...provider,
          apiKey: provider.apiKey || null,
          config: {},
        }),
      });
      setNotice({ tone: "success", message: "Đã lưu cấu hình mô hình AI." });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể lưu provider",
      });
    } finally {
      setWorking(false);
    }
  }
  async function runGeneration(event: FormEvent) {
    event.preventDefault();
    setWorking(true);
    setNotice({
      tone: "info",
      message: "Đang đọc tài liệu và sinh bộ câu hỏi…",
    });
    try {
      const job = await apiRequest<GenerationJobRow>(
        "/api/v1/ai/question-generation-jobs",
        {
          method: "POST",
          body: JSON.stringify({
            courseId: generate.courseId,
            providerConfigId: generate.providerConfigId,
            documentFileIds: generate.documentFileIds
              .split(/[\s,;]+/)
              .filter(Boolean),
            sourceText: generate.sourceText || null,
            language: generate.language,
            numberOfQuestions: Number(generate.numberOfQuestions),
            questionTypes: Array.from(generate.questionTypes),
            difficultyDistribution: { EASY: 30, MEDIUM: 50, HARD: 20 },
          }),
        },
      );
      setSelectedJob(job.id);
      setTab("review");
      setNotice({
        tone: job.status === "REVIEW_REQUIRED" ? "success" : "error",
        message:
          job.status === "REVIEW_REQUIRED"
            ? "Bộ câu hỏi đã sinh xong và đang chờ duyệt."
            : (job.errorMessage ?? "Mô hình không trả kết quả hợp lệ"),
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể sinh câu hỏi",
      });
    } finally {
      setWorking(false);
    }
  }
  async function reviewJob(
    id: string,
    decision: "APPROVE" | "REJECT" | "REQUEST_CHANGES",
  ) {
    setWorking(true);
    try {
      await apiRequest(`/api/v1/ai/question-generation-jobs/${id}/review`, {
        method: "POST",
        body: JSON.stringify({
          decision,
          comments:
            decision === "APPROVE"
              ? "Đã kiểm tra nội dung và trích dẫn"
              : "Cần chỉnh lại nội dung",
        }),
      });
      setNotice({
        tone: "success",
        message:
          decision === "APPROVE"
            ? "Bộ câu hỏi đã được duyệt."
            : "Đã trả bộ câu hỏi về trạng thái cần chỉnh sửa.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể duyệt",
      });
    } finally {
      setWorking(false);
    }
  }
  async function importJob(id: string) {
    setWorking(true);
    try {
      const result = await apiRequest<{ importedQuestionIds: string[] }>(
        `/api/v1/ai/question-generation-jobs/${id}/import`,
        { method: "POST" },
      );
      setNotice({
        tone: "success",
        message: `Đã nhập ${result.importedQuestionIds.length} câu hỏi ở trạng thái DRAFT vào ngân hàng.`,
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể nhập câu hỏi",
      });
    } finally {
      setWorking(false);
    }
  }
  return (
    <>
      <WorkspaceHero
        eyebrow="XƯỞNG LUYỆN KIM TRI THỨC"
        title="AI sinh bộ câu hỏi từ tài liệu"
        description="Dùng mô hình local hoặc API riêng của khách hàng. Mọi provider cùng trả một JSON chuẩn, có trích dẫn nguồn, kiểm định cấu trúc và bước duyệt của con người trước khi nhập ngân hàng."
        icon={<Icon name="question" size={31} />}
        stats={[
          {
            value: providers.filter((v) => v.enabled).length,
            label: "Mô hình đang bật",
          },
          { value: jobs.length, label: "Tác vụ" },
          {
            value: jobs.filter((v) => v.status === "REVIEW_REQUIRED").length,
            label: "Chờ duyệt",
            tone: "gold",
          },
          {
            value: jobs.reduce(
              (sum, job) => sum + (job.questionSet?.questions?.length ?? 0),
              0,
            ),
            label: "Câu đã sinh",
            tone: "violet",
          },
        ]}
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      <nav className="workspace-tabs">
        <button
          className={tab === "generate" ? "active" : ""}
          onClick={() => setTab("generate")}
        >
          Sinh câu hỏi
        </button>
        <button
          className={tab === "review" ? "active" : ""}
          onClick={() => setTab("review")}
        >
          Duyệt kết quả
        </button>
        {canConfigure && (
          <button
            className={tab === "providers" ? "active" : ""}
            onClick={() => setTab("providers")}
          >
            Mô hình AI
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
      ) : tab === "generate" ? (
        <div className="workspace-two-column wide-left">
          <Panel
            title="Nguồn tri thức"
            subtitle="Có thể chọn file DOCX/PDF đã tải lên hoặc dán nội dung trực tiếp."
          >
            <form className="workspace-form" onSubmit={runGeneration}>
              <Field label="Course ID">
                <input
                  required
                  value={generate.courseId}
                  onChange={(e) =>
                    setGenerate({ ...generate, courseId: e.target.value })
                  }
                  placeholder="UUID khóa học"
                />
              </Field>
              <Field label="Mô hình">
                <select
                  required
                  value={generate.providerConfigId}
                  onChange={(e) =>
                    setGenerate({
                      ...generate,
                      providerConfigId: e.target.value,
                    })
                  }
                >
                  <option value="">Chọn provider</option>
                  {providers
                    .filter((v) => v.enabled)
                    .map((item) => (
                      <option value={item.id} key={item.id}>
                        {item.code} · {item.model}
                      </option>
                    ))}
                </select>
              </Field>
              <Field
                label="File ID tài liệu"
                wide
                hint="Nhiều UUID cách nhau bằng dấu phẩy hoặc xuống dòng."
              >
                <textarea
                  value={generate.documentFileIds}
                  onChange={(e) =>
                    setGenerate({
                      ...generate,
                      documentFileIds: e.target.value,
                    })
                  }
                />
              </Field>
              <div className="source-divider">
                <span>hoặc</span>
              </div>
              <Field label="Nội dung nhập trực tiếp" wide>
                <textarea
                  className="tall"
                  value={generate.sourceText}
                  onChange={(e) =>
                    setGenerate({ ...generate, sourceText: e.target.value })
                  }
                  placeholder="Dán phần tài liệu cần tạo câu hỏi…"
                />
              </Field>
              <Button
                type="submit"
                disabled={
                  working ||
                  (!generate.sourceText.trim() &&
                    !generate.documentFileIds.trim())
                }
              >
                <span className="button-spark">✦</span>{" "}
                {working ? "Đang chuyển hóa…" : "Sinh bộ câu hỏi"}
              </Button>
            </form>
          </Panel>
          <Panel
            title="Thiết lập bộ câu hỏi"
            subtitle="Mô hình chỉ được dùng nội dung nguồn và bắt buộc dẫn chứng."
          >
            <div className="ai-orbital">
              <span>AI</span>
              <i />
              <b />
            </div>
            <form className="workspace-form compact">
              <Field label="Số lượng">
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={generate.numberOfQuestions}
                  onChange={(e) =>
                    setGenerate({
                      ...generate,
                      numberOfQuestions: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Ngôn ngữ">
                <select
                  value={generate.language}
                  onChange={(e) =>
                    setGenerate({ ...generate, language: e.target.value })
                  }
                >
                  <option value="vi">Tiếng Việt</option>
                  <option value="en">English</option>
                </select>
              </Field>
              <div className="question-type-pills">
                {["SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE"].map(
                  (type) => (
                    <label
                      key={type}
                      className={
                        generate.questionTypes.has(type) ? "active" : ""
                      }
                    >
                      <input
                        type="checkbox"
                        checked={generate.questionTypes.has(type)}
                        onChange={(e) =>
                          setGenerate((current) => {
                            const next = new Set(current.questionTypes);
                            e.target.checked
                              ? next.add(type)
                              : next.delete(type);
                            return { ...current, questionTypes: next };
                          })
                        }
                      />
                      <span>{type.replaceAll("_", " ")}</span>
                    </label>
                  ),
                )}
              </div>
            </form>
            <div className="schema-preview">
              <span>JSON SCHEMA 1.0</span>
              <code>{`{\n  "questions": [\n    { "type": "SINGLE_CHOICE",\n      "citations": [...] }\n  ]\n}`}</code>
            </div>
          </Panel>
        </div>
      ) : tab === "providers" ? (
        <div className="workspace-two-column wide-left">
          <Panel
            title="Provider đã cấu hình"
            subtitle="API key chỉ được hiển thị dưới dạng trạng thái đã cấu hình."
          >
            {providers.length ? (
              <div className="provider-list">
                {providers.map((item) => (
                  <div key={item.id}>
                    <span
                      className={`provider-light ${item.enabled ? "on" : ""}`}
                    />
                    <div>
                      <strong>{item.code}</strong>
                      <small>
                        {item.providerType} · {item.model}
                      </small>
                      <code>{item.baseUrl}</code>
                    </div>
                    <div>
                      <Tag tone={item.enabled ? "teal" : ""}>
                        {item.enabled ? "ON" : "OFF"}
                      </Tag>
                      {item.apiKeyConfigured && <Tag tone="violet">KEY</Tag>}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có mô hình AI. Có thể cấu hình local trước, không cần API key." />
            )}
          </Panel>
          <Panel
            title="Thêm mô hình"
            subtitle="OpenAI-compatible giúp dùng chung Ollama, vLLM, LocalAI hoặc API thương mại."
          >
            <form className="workspace-form" onSubmit={saveProvider}>
              <Field label="Mã">
                <input
                  required
                  value={provider.code}
                  onChange={(e) =>
                    setProvider({
                      ...provider,
                      code: e.target.value.toUpperCase(),
                    })
                  }
                />
              </Field>
              <Field label="Loại">
                <select
                  value={provider.providerType}
                  onChange={(e) =>
                    setProvider({ ...provider, providerType: e.target.value })
                  }
                >
                  <option>LOCAL_OPENAI_COMPATIBLE</option>
                  <option>REMOTE_OPENAI_COMPATIBLE</option>
                  <option>CUSTOM_ADAPTER</option>
                </select>
              </Field>
              <Field label="Base URL" wide>
                <input
                  required
                  value={provider.baseUrl}
                  onChange={(e) =>
                    setProvider({ ...provider, baseUrl: e.target.value })
                  }
                />
              </Field>
              <Field label="Model">
                <input
                  required
                  value={provider.model}
                  onChange={(e) =>
                    setProvider({ ...provider, model: e.target.value })
                  }
                />
              </Field>
              <Field label="API key">
                <input
                  type="password"
                  value={provider.apiKey}
                  onChange={(e) =>
                    setProvider({ ...provider, apiKey: e.target.value })
                  }
                  placeholder="Để trống với mô hình local"
                />
              </Field>
              <label className="workspace-check">
                <input
                  type="checkbox"
                  checked={provider.enabled}
                  onChange={(e) =>
                    setProvider({ ...provider, enabled: e.target.checked })
                  }
                />
                <span>Bật provider</span>
              </label>
              <Button type="submit" disabled={working}>
                <Icon name="save" size={16} /> Lưu mô hình
              </Button>
            </form>
          </Panel>
        </div>
      ) : (
        <div className="workspace-two-column ai-review-layout">
          <Panel
            title="Lịch sử sinh câu hỏi"
            subtitle="Chọn một tác vụ để xem nội dung và trích dẫn."
          >
            {jobs.length ? (
              <div className="job-list">
                {jobs.map((job) => (
                  <button
                    key={job.id}
                    onClick={() => setSelectedJob(job.id)}
                    className={
                      (activeJob?.id === job.id ? "active " : "") +
                      job.status.toLowerCase()
                    }
                  >
                    <span className="job-status-orb" />
                    <div>
                      <strong>
                        {job.questionSet?.questions?.length ?? 0} câu hỏi
                      </strong>
                      <small>{job.courseId}</small>
                      <time>
                        {new Date(job.createdAt).toLocaleString("vi-VN")}
                      </time>
                    </div>
                    <Tag
                      tone={
                        job.status === "REVIEW_REQUIRED"
                          ? "gold"
                          : job.status === "APPROVED" ||
                              job.status === "IMPORTED"
                            ? "teal"
                            : job.status === "FAILED"
                              ? "danger"
                              : ""
                      }
                    >
                      {job.status}
                    </Tag>
                  </button>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có tác vụ sinh câu hỏi." />
            )}
          </Panel>
          <Panel
            title="Bộ câu hỏi được sinh"
            subtitle={activeJob ? `Job ${activeJob.id}` : "Chọn một tác vụ"}
            className="span-two"
          >
            {activeJob?.errorMessage && (
              <div className="workspace-callout danger">
                <Icon name="warning" />
                <p>{activeJob.errorMessage}</p>
              </div>
            )}
            {activeJob?.validationProblems?.length ? (
              <div className="validation-list">
                {activeJob.validationProblems.map((problem) => (
                  <div key={`${problem.path}-${problem.message}`}>
                    <code>{problem.path}</code>
                    <span>{problem.message}</span>
                  </div>
                ))}
              </div>
            ) : null}
            {activeJob?.questionSet?.questions?.length ? (
              <div className="generated-questions">
                {activeJob.questionSet.questions.map((question, index) => (
                  <article key={question.externalId ?? index}>
                    <header>
                      <span>{String(index + 1).padStart(2, "0")}</span>
                      <div>
                        <Tag tone="violet">{question.type}</Tag>
                        <Tag>Độ khó {question.difficulty ?? "MEDIUM"}</Tag>
                        <Tag tone="gold">{question.points ?? 1} điểm</Tag>
                      </div>
                    </header>
                    <h3>{question.stem}</h3>
                    {question.options?.length ? (
                      <div className="generated-options">
                        {question.options.map((option) => (
                          <div
                            className={
                              question.correctOptionIds?.includes(option.id)
                                ? "correct"
                                : ""
                            }
                            key={option.id}
                          >
                            <b>{option.id}</b>
                            <span>{option.text}</span>
                            {question.correctOptionIds?.includes(option.id) && (
                              <Icon name="check" size={15} />
                            )}
                          </div>
                        ))}
                      </div>
                    ) : null}
                    <footer>
                      {question.citations?.map((citation, i) => (
                        <details key={i}>
                          <summary>
                            <Icon name="file" size={14} /> Nguồn {i + 1}
                            {citation.page ? ` · trang ${citation.page}` : ""}
                          </summary>
                          <p>{citation.quote}</p>
                          <small>
                            {citation.documentVersionId} · {citation.section}
                          </small>
                        </details>
                      ))}
                    </footer>
                  </article>
                ))}
              </div>
            ) : (
              <Empty text="Tác vụ chưa có bộ câu hỏi để hiển thị." />
            )}{" "}
            {activeJob && canApprove && (
              <div className="review-actions">
                <Button
                  tone="secondary"
                  disabled={working}
                  onClick={() =>
                    void reviewJob(activeJob.id, "REQUEST_CHANGES")
                  }
                >
                  Yêu cầu chỉnh sửa
                </Button>
                <Button
                  tone="danger"
                  disabled={working}
                  onClick={() => void reviewJob(activeJob.id, "REJECT")}
                >
                  Từ chối
                </Button>
                <Button
                  disabled={
                    working ||
                    !["REVIEW_REQUIRED", "APPROVED"].includes(activeJob.status)
                  }
                  onClick={() =>
                    activeJob.status === "APPROVED"
                      ? void importJob(activeJob.id)
                      : void reviewJob(activeJob.id, "APPROVE")
                  }
                >
                  <Icon name="check" size={16} />{" "}
                  {activeJob.status === "APPROVED"
                    ? "Nhập vào ngân hàng"
                    : "Duyệt bộ câu hỏi"}
                </Button>
              </div>
            )}
          </Panel>
        </div>
      )}
    </>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Document studio with versions and OnlyOffice
// ─────────────────────────────────────────────────────────────────────────────

type StoredFileRow = {
  id: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  purpose: string;
  status: string;
  createdAt: string;
};
type FileVersionRow = {
  id: string;
  fileId: string;
  versionNumber: number;
  mediaType: string;
  sizeBytes: number;
  sha256: string;
  sourceType: string;
  parentVersionId?: string;
  changeSummary?: string;
  createdBy: string;
  createdAt: string;
};
type EditSessionRow = {
  id: string;
  fileId: string;
  baseVersionId: string;
  editorType: string;
  status: string;
  expiresAt: string;
  documentUrl: string;
  callbackUrl?: string;
  editorServerUrl?: string;
  editorConfig: Record<string, any>;
};

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (
        id: string,
        config: Record<string, any>,
      ) => { destroyEditor?: () => void };
    };
  }
}

function DocumentStudio({ user }: { user: PortalUser }) {
  const canUpload =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("files:upload");
  const canEdit =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("files:edit");
  const canReadVersions =
    canEdit || user.permissions.includes("files:version:read");
  const {
    data: files,
    loading,
    error,
    refresh,
  } = useLoad(() => apiRequest<StoredFileRow[]>("/api/v1/files"), []);
  const [selected, setSelected] = useState<StoredFileRow | null>(null);
  const [versions, setVersions] = useState<FileVersionRow[]>([]);
  const [session, setSession] = useState<EditSessionRow | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const uploadRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (!selected) {
      setVersions([]);
      return;
    }
    if (canReadVersions)
      void apiRequest<FileVersionRow[]>(`/api/v1/files/${selected.id}/versions`)
        .then(setVersions)
        .catch(() => setVersions([]));
  }, [selected, canReadVersions]);
  async function upload(file?: File) {
    if (!file) return;
    setWorking(true);
    try {
      const body = new FormData();
      body.set("file", file);
      const saved = await apiRequest<StoredFileRow>(
        "/api/v1/files?purpose=COURSE_DOCUMENT",
        { method: "POST", body },
      );
      setNotice({
        tone: "success",
        message: `Đã tải lên ${saved.originalName}.`,
      });
      await refresh();
      setSelected(saved);
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể tải tài liệu",
      });
    } finally {
      setWorking(false);
      if (uploadRef.current) uploadRef.current.value = "";
    }
  }
  async function edit(file: StoredFileRow) {
    setWorking(true);
    try {
      const result = await apiRequest<EditSessionRow>(
        `/api/v1/files/${file.id}/edit-sessions`,
        {
          method: "POST",
          body: JSON.stringify({
            editorType: "ONLYOFFICE",
            changeSummary: "Chỉnh sửa trực tuyến",
          }),
        },
      );
      setSelected(file);
      setSession(result);
      setNotice({
        tone: "info",
        message: "Phiên chỉnh sửa đã mở. Mọi lần lưu tạo một phiên bản mới.",
      });
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error
            ? cause.message
            : "Không thể mở trình chỉnh sửa",
      });
    } finally {
      setWorking(false);
    }
  }
  function readableBytes(value: number) {
    if (value < 1024) return `${value} B`;
    if (value < 1024 ** 2) return `${(value / 1024).toFixed(1)} KB`;
    return `${(value / 1024 ** 2).toFixed(1)} MB`;
  }
  const rows = files ?? [];
  return (
    <>
      <WorkspaceHero
        eyebrow="THƯ VIỆN SỐNG"
        title="Tài liệu & xưởng biên tập"
        description="Tải lên, xem, sửa DOCX/PDF trực tiếp qua trình biên tập được cấu hình. Mỗi lần lưu tạo version, checksum và dấu vết người sửa để không bao giờ mất bản cũ."
        icon={<Icon name="file" size={31} />}
        stats={[
          { value: rows.length, label: "Tài liệu" },
          {
            value: rows.filter((v) => v.contentType.includes("wordprocessing"))
              .length,
            label: "DOCX",
          },
          {
            value: rows.filter((v) => v.contentType === "application/pdf")
              .length,
            label: "PDF",
          },
          {
            value: versions.length,
            label: "Phiên bản file đang chọn",
            tone: "violet",
          },
        ]}
        actions={
          canUpload ? (
            <>
              <input
                ref={uploadRef}
                type="file"
                accept=".docx,.pdf"
                hidden
                onChange={(e) => void upload(e.target.files?.[0])}
              />
              <Button
                onClick={() => uploadRef.current?.click()}
                disabled={working}
              >
                <Icon name="upload" size={16} /> Tải tài liệu
              </Button>
            </>
          ) : undefined
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
        <div className="workspace-grid-three document-layout">
          <Panel
            title="Kho tài liệu"
            subtitle="Chọn một tệp để xem lịch sử phiên bản."
            className="span-two"
          >
            {rows.length ? (
              <div className="document-grid">
                {rows.map((file) => (
                  <article
                    className={`document-card ${selected?.id === file.id ? "active" : ""}`}
                    key={file.id}
                    onClick={() => setSelected(file)}
                  >
                    <div
                      className={`document-cover ${file.contentType === "application/pdf" ? "pdf" : "docx"}`}
                    >
                      <span>
                        {file.contentType === "application/pdf"
                          ? "PDF"
                          : "DOCX"}
                      </span>
                      <i>✦</i>
                    </div>
                    <div>
                      <strong>{file.originalName}</strong>
                      <small>
                        {readableBytes(file.sizeBytes)} ·{" "}
                        {new Date(file.createdAt).toLocaleDateString("vi-VN")}
                      </small>
                      <code>{file.sha256.slice(0, 12)}…</code>
                    </div>
                    <footer>
                      <a
                        className="workspace-button ghost"
                        href={`/api/gateway/api/v1/files/${file.id}/content?inline=true`}
                        target="_blank"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <Icon name="eye" size={14} /> Xem
                      </a>
                      <Button
                        tone="secondary"
                        disabled={
                          working ||
                          !canEdit ||
                          ![
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                          ].includes(file.contentType)
                        }
                        onClick={(event) => {
                          event.stopPropagation();
                          void edit(file);
                        }}
                      >
                        <Icon name="edit" size={14} /> Chỉnh sửa
                      </Button>
                    </footer>
                  </article>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có tài liệu. Tải DOCX hoặc PDF đầu tiên lên thư viện." />
            )}
          </Panel>
          <Panel
            title="Dòng thời gian phiên bản"
            subtitle={selected?.originalName ?? "Chọn tài liệu"}
            className="sticky-panel"
          >
            {versions.length ? (
              <div className="version-timeline">
                {versions.map((version, index) => (
                  <div key={version.id}>
                    <span>{index === 0 ? "✦" : "◇"}</span>
                    <div>
                      <strong>Phiên bản {version.versionNumber}</strong>
                      <small>
                        {version.sourceType} ·{" "}
                        {readableBytes(version.sizeBytes)}
                      </small>
                      <p>
                        {version.changeSummary ||
                          (version.sourceType === "UPLOAD"
                            ? "Bản tải lên ban đầu"
                            : "Không có mô tả")}
                      </p>
                      <time>
                        {new Date(version.createdAt).toLocaleString("vi-VN")}
                      </time>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <Empty
                text={
                  selected
                    ? "Bản đầu sẽ được ghi nhận khi mở phiên chỉnh sửa."
                    : "Chọn một tài liệu để xem lịch sử."
                }
              />
            )}
          </Panel>
          {session && (
            <Panel
              title="Không gian biên tập"
              subtitle={`Phiên hết hạn lúc ${new Date(session.expiresAt).toLocaleTimeString("vi-VN")}`}
              className="span-three editor-panel"
              action={
                <Button tone="ghost" onClick={() => setSession(null)}>
                  <Icon name="close" size={14} /> Đóng khung
                </Button>
              }
            >
              <OnlyOfficeWorkspace session={session} />
            </Panel>
          )}
        </div>
      )}
    </>
  );
}

function OnlyOfficeWorkspace({ session }: { session: EditSessionRow }) {
  const hostRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<{ destroyEditor?: () => void } | null>(null);
  const [status, setStatus] = useState("Đang kết nối trình biên tập…");
  useEffect(() => {
    const server = session.editorServerUrl?.replace(/\/$/, "");
    if (!server) {
      setStatus("Chưa cấu hình máy chủ OnlyOffice.");
      return;
    }
    const start = () => {
      if (!window.DocsAPI || !hostRef.current) return;
      try {
        editorRef.current = new window.DocsAPI.DocEditor(hostRef.current.id, {
          ...session.editorConfig,
          width: "100%",
          height: "100%",
          events: {
            onAppReady: () => setStatus("Sẵn sàng chỉnh sửa"),
            onError: (event: any) =>
              setStatus(`Lỗi trình biên tập: ${event?.data ?? "unknown"}`),
          },
        });
      } catch {
        setStatus("Không thể khởi tạo trình biên tập.");
      }
    };
    if (window.DocsAPI) start();
    else {
      const script = document.createElement("script");
      script.src = `${server}/web-apps/apps/api/documents/api.js`;
      script.async = true;
      script.onload = start;
      script.onerror = () =>
        setStatus("Không kết nối được máy chủ OnlyOffice.");
      document.head.appendChild(script);
    }
    return () => {
      editorRef.current?.destroyEditor?.();
    };
  }, [session]);
  return (
    <div className="onlyoffice-workspace">
      <div className="editor-status">
        <span className={status === "Sẵn sàng chỉnh sửa" ? "on" : ""} />
        {status}
      </div>
      <div
        id={`onlyoffice-${session.id}`}
        ref={hostRef}
        className="onlyoffice-host"
      />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Competitions
// ─────────────────────────────────────────────────────────────────────────────

type QuestionRow = {
  id: string;
  type: string;
  prompt: string;
  options: string[];
  defaultPoints: number;
  status: string;
};
type CompetitionRow = {
  id: string;
  exam: {
    id: string;
    title: string;
    durationMinutes: number;
    opensAt?: string;
    closesAt?: string;
    status: string;
    questions: QuestionRow[];
  };
  registrationOpensAt?: string;
  registrationClosesAt?: string;
  leaderboardVisibility: string;
  resultStatus: string;
  publishedAt?: string;
  rewards: Array<{
    id: string;
    rankFrom: number;
    rankTo: number;
    rewardType: string;
    rewardPayload: Record<string, any>;
  }>;
};
type LeaderboardRow = {
  rank?: number;
  userId: string;
  attemptId: string;
  score: number;
  durationMs: number;
  submittedAt: string;
  mine: boolean;
};

function CompetitionConsole({ user }: { user: PortalUser }) {
  const manage =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("competitions:manage");
  const { data, loading, error, refresh } = useLoad(
    async () => ({
      competitions: await apiRequest<CompetitionRow[]>("/api/v1/competitions"),
      questions: manage
        ? await apiRequest<QuestionRow[]>("/api/v1/questions")
        : [],
    }),
    [manage],
  );
  const [selected, setSelected] = useState<string>("");
  const [leaderboard, setLeaderboard] = useState<LeaderboardRow[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [draft, setDraft] = useState({
    title: "",
    durationMinutes: 30,
    opensAt: "",
    closesAt: "",
    questionIds: new Set<string>(),
    rewardType: "CERTIFICATE",
    rewardLabel: "Quán quân",
    leaderboardVisibility: "AFTER_CLOSE",
  });
  const rows = data?.competitions ?? [];
  const questions = data?.questions ?? [];
  const active = rows.find((v) => v.id === selected) ?? rows[0];
  useEffect(() => {
    if (!active) {
      setLeaderboard([]);
      return;
    }
    void apiRequest<LeaderboardRow[]>(
      `/api/v1/competitions/${active.id}/leaderboard`,
    )
      .then(setLeaderboard)
      .catch(() => setLeaderboard([]));
  }, [active?.id]);
  async function createCompetition(event: FormEvent) {
    event.preventDefault();
    if (!draft.questionIds.size) {
      setNotice({ tone: "error", message: "Hãy chọn ít nhất một câu hỏi." });
      return;
    }
    setWorking(true);
    try {
      await apiRequest("/api/v1/competitions", {
        method: "POST",
        body: JSON.stringify({
          exam: {
            title: draft.title,
            courseId: null,
            contextType: "COMPETITION",
            durationMinutes: Number(draft.durationMinutes),
            opensAt: draft.opensAt
              ? new Date(draft.opensAt).toISOString()
              : null,
            closesAt: draft.closesAt
              ? new Date(draft.closesAt).toISOString()
              : null,
            maxAttempts: 1,
            waitMinutesBetweenAttempts: 0,
            passingScore: 0,
            shuffleQuestions: true,
            shuffleAnswers: true,
            scoreStrategy: "HIGHEST",
            status: "ACTIVE",
            autoGrade: true,
            questions: Array.from(draft.questionIds).map(
              (questionId, index) => ({
                questionId,
                points:
                  questions.find((q) => q.id === questionId)?.defaultPoints ??
                  1,
                sortOrder: index,
              }),
            ),
          },
          competition: {
            registrationOpensAt: draft.opensAt
              ? new Date(draft.opensAt).toISOString()
              : null,
            registrationClosesAt: draft.closesAt
              ? new Date(draft.closesAt).toISOString()
              : null,
            leaderboardVisibility: draft.leaderboardVisibility,
            rewards: [
              {
                rankFrom: 1,
                rankTo: 1,
                rewardType: draft.rewardType,
                rewardPayload: { label: draft.rewardLabel },
              },
            ],
          },
        }),
      });
      setNotice({
        tone: "success",
        message: "Đã tạo cuộc thi và kỳ thi độc lập đi kèm.",
      });
      setDraft({
        title: "",
        durationMinutes: 30,
        opensAt: "",
        closesAt: "",
        questionIds: new Set(),
        rewardType: "CERTIFICATE",
        rewardLabel: "Quán quân",
        leaderboardVisibility: "AFTER_CLOSE",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể tạo cuộc thi",
      });
    } finally {
      setWorking(false);
    }
  }
  async function publish(id: string) {
    setWorking(true);
    try {
      await apiRequest(`/api/v1/competitions/${id}/publish`, {
        method: "POST",
      });
      setNotice({
        tone: "success",
        message: "Đã chốt và công bố bảng xếp hạng.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể công bố",
      });
    } finally {
      setWorking(false);
    }
  }
  async function issueRewards(id: string) {
    setWorking(true);
    try {
      const result = await apiRequest<any[]>(
        `/api/v1/competitions/${id}/rewards/issue`,
        { method: "POST" },
      );
      setNotice({
        tone: "success",
        message: `Đã ghi nhận ${result.length} phần thưởng, không phát trùng.`,
      });
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể phát thưởng",
      });
    } finally {
      setWorking(false);
    }
  }
  return (
    <>
      <WorkspaceHero
        eyebrow="CUỘC THI KIẾN THỨC"
        title="Cuộc thi, xếp hạng & phần thưởng"
        description="Cuộc thi là kỳ thi độc lập, không thuộc khóa học. Xếp hạng theo điểm, thời gian làm và thời điểm nộp; người thắng có thể nhận chứng chỉ, điểm thưởng hoặc quà do tổ chức định nghĩa."
        icon={<Icon name="grade" size={31} />}
        stats={[
          { value: rows.length, label: "Cuộc thi" },
          {
            value: rows.filter((v) => v.exam.status === "ACTIVE").length,
            label: "Đang mở",
          },
          { value: leaderboard.length, label: "Người trên bảng hạng" },
          {
            value: active?.rewards.length ?? 0,
            label: "Luật thưởng",
            tone: "gold",
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
        <div
          className={`workspace-grid-three competition-layout ${manage ? "" : "participant"}`}
        >
          <Panel
            title="Các cuộc thi"
            subtitle="Chọn cuộc thi để xem bảng xếp hạng."
          >
            {rows.length ? (
              <div className="competition-list">
                {rows.map((item) => (
                  <button
                    key={item.id}
                    className={active?.id === item.id ? "active" : ""}
                    onClick={() => setSelected(item.id)}
                  >
                    <span className="competition-medal">✦</span>
                    <div>
                      <strong>{item.exam.title}</strong>
                      <small>
                        {item.exam.durationMinutes} phút ·{" "}
                        {item.exam.questions.length} câu
                      </small>
                      <div className="workspace-tags">
                        <Tag tone={item.exam.status === "ACTIVE" ? "teal" : ""}>
                          {item.exam.status}
                        </Tag>
                        <Tag>{item.resultStatus}</Tag>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có cuộc thi nào trong phạm vi." />
            )}
          </Panel>
          <Panel
            title="Bảng xếp hạng"
            subtitle={active?.exam.title ?? "Chọn cuộc thi"}
            className="span-two"
          >
            <div className="leaderboard-stage">
              <div className="podium">
                {[1, 0, 2].map((index) => {
                  const entry = leaderboard[index];
                  const position = index + 1;
                  return (
                    <div className={`podium-place p${position}`} key={position}>
                      <span>
                        {entry ? entry.userId.slice(0, 2).toUpperCase() : "?"}
                      </span>
                      <strong>
                        {entry ? `#${entry.rank}` : `#${position}`}
                      </strong>
                      <small>
                        {entry ? `${entry.score.toFixed(1)} điểm` : "Chưa có"}
                      </small>
                      <i />
                    </div>
                  );
                })}
              </div>
              {leaderboard.length ? (
                <div className="leaderboard-table">
                  {leaderboard.map((entry) => (
                    <div
                      key={entry.userId}
                      className={entry.mine ? "mine" : ""}
                    >
                      <b>#{entry.rank ?? "—"}</b>
                      <span className="mini-avatar">
                        {entry.userId.slice(0, 2).toUpperCase()}
                      </span>
                      <div>
                        <strong>{entry.userId}</strong>
                        <small>
                          Nộp{" "}
                          {new Date(entry.submittedAt).toLocaleString("vi-VN")}
                        </small>
                      </div>
                      <strong>{entry.score.toFixed(2)}</strong>
                      <small>
                        {(entry.durationMs / 60000).toFixed(1)} phút
                      </small>
                    </div>
                  ))}
                </div>
              ) : (
                <Empty text="Bảng xếp hạng sẽ xuất hiện sau khi có bài nộp hợp lệ." />
              )}
            </div>
            {active && !manage && (
              <div className="review-actions">
                <a
                  className="workspace-button primary"
                  href={`/exams/${active.id}`}
                >
                  <Icon name="play" size={16} /> Tham gia cuộc thi
                </a>
              </div>
            )}
            {active && manage && (
              <div className="review-actions">
                <Button
                  tone="secondary"
                  disabled={working}
                  onClick={() => void publish(active.id)}
                >
                  Công bố kết quả
                </Button>
                <Button
                  disabled={working || active.resultStatus !== "PUBLISHED"}
                  onClick={() => void issueRewards(active.id)}
                >
                  <Icon name="certificate" size={16} /> Phát thưởng
                </Button>
              </div>
            )}
          </Panel>
          {manage && (
            <Panel
              title="Mở cuộc thi mới"
              subtitle="Chọn câu hỏi từ ngân hàng; chấm tự động và chỉ cho một lượt thi."
              className="span-three"
            >
              <form className="competition-form" onSubmit={createCompetition}>
                <div className="workspace-form">
                  <Field label="Tên cuộc thi" wide>
                    <input
                      required
                      value={draft.title}
                      onChange={(e) =>
                        setDraft({ ...draft, title: e.target.value })
                      }
                    />
                  </Field>
                  <Field label="Mở từ">
                    <input
                      type="datetime-local"
                      value={draft.opensAt}
                      onChange={(e) =>
                        setDraft({ ...draft, opensAt: e.target.value })
                      }
                    />
                  </Field>
                  <Field label="Đóng lúc">
                    <input
                      type="datetime-local"
                      value={draft.closesAt}
                      onChange={(e) =>
                        setDraft({ ...draft, closesAt: e.target.value })
                      }
                    />
                  </Field>
                  <Field label="Thời lượng">
                    <input
                      type="number"
                      min={1}
                      max={480}
                      value={draft.durationMinutes}
                      onChange={(e) =>
                        setDraft({
                          ...draft,
                          durationMinutes: Number(e.target.value),
                        })
                      }
                    />
                  </Field>
                  <Field label="Hiển thị bảng hạng">
                    <select
                      value={draft.leaderboardVisibility}
                      onChange={(e) =>
                        setDraft({
                          ...draft,
                          leaderboardVisibility: e.target.value,
                        })
                      }
                    >
                      <option>LIVE</option>
                      <option>AFTER_CLOSE</option>
                      <option>ADMIN_ONLY</option>
                    </select>
                  </Field>
                  <Field label="Loại phần thưởng">
                    <select
                      value={draft.rewardType}
                      onChange={(e) =>
                        setDraft({ ...draft, rewardType: e.target.value })
                      }
                    >
                      <option>CERTIFICATE</option>
                      <option>POINTS</option>
                      <option>BADGE</option>
                      <option>GIFT</option>
                    </select>
                  </Field>
                  <Field label="Tên phần thưởng" wide>
                    <input
                      value={draft.rewardLabel}
                      onChange={(e) =>
                        setDraft({ ...draft, rewardLabel: e.target.value })
                      }
                    />
                  </Field>
                  <Button
                    type="submit"
                    disabled={working || !draft.questionIds.size}
                  >
                    <Icon name="plus" size={16} /> Tạo cuộc thi
                  </Button>
                </div>
                <div className="question-picker">
                  <header>
                    <strong>Ngân hàng câu hỏi</strong>
                    <span>{draft.questionIds.size} câu đã chọn</span>
                  </header>
                  {questions.length ? (
                    questions.map((question) => (
                      <label
                        key={question.id}
                        className={
                          draft.questionIds.has(question.id) ? "selected" : ""
                        }
                      >
                        <input
                          type="checkbox"
                          checked={draft.questionIds.has(question.id)}
                          onChange={(e) =>
                            setDraft((current) => {
                              const next = new Set(current.questionIds);
                              e.target.checked
                                ? next.add(question.id)
                                : next.delete(question.id);
                              return { ...current, questionIds: next };
                            })
                          }
                        />
                        <span>
                          <Tag tone="violet">{question.type}</Tag>
                          <strong>{question.prompt}</strong>
                          <small>{question.defaultPoints} điểm</small>
                        </span>
                      </label>
                    ))
                  ) : (
                    <Empty text="Hãy tạo câu hỏi trong ngân hàng trước khi mở cuộc thi." />
                  )}
                </div>
              </form>
            </Panel>
          )}
        </div>
      )}
    </>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Live sessions and course assignments
// ─────────────────────────────────────────────────────────────────────────────

type TrainingClassRow = {
  id: string;
  code: string;
  name: string;
  courseId: string;
  startsAt?: string;
  endsAt?: string;
  dueAt?: string;
  instructorIds: string[];
  status: string;
};
type LiveSessionRow = {
  id: string;
  classId: string;
  courseId: string;
  title: string;
  provider: string;
  joinUrl: string;
  hostUrl?: string;
  startsAt: string;
  endsAt: string;
  status: string;
};
type AssignmentRow = {
  id: string;
  classId?: string;
  courseId: string;
  assigneeType: string;
  assigneeId: string;
  assignedAt: string;
  availableFrom?: string;
  dueAt?: string;
  gracePeriodMinutes: number;
  required: boolean;
  status: string;
  enrolledUsers: number;
};

function LiveSessionConsole({ user }: { user: PortalUser }) {
  const systemAdmin = user.accountType === "SYSTEM_ADMIN";
  const manage =
    systemAdmin || user.permissions.includes("live-sessions:manage");
  const canAssign =
    systemAdmin ||
    user.permissions.some((permission) =>
      ["courses:assign", "enrollments:write"].includes(permission),
    );
  const { data, loading, error, refresh } = useLoad(async () => {
    if (manage)
      return {
        classes: await apiRequest<TrainingClassRow[]>("/api/v1/classes"),
        mine: [] as LiveSessionRow[],
      };
    return {
      classes: [] as TrainingClassRow[],
      mine: await apiRequest<LiveSessionRow[]>("/api/v1/live-sessions/me"),
    };
  }, [manage]);
  const [classId, setClassId] = useState("");
  const [sessions, setSessions] = useState<LiveSessionRow[]>([]);
  const [assignments, setAssignments] = useState<AssignmentRow[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [live, setLive] = useState({
    title: "",
    provider: "EXTERNAL",
    joinUrl: "",
    hostUrl: "",
    startsAt: "",
    endsAt: "",
  });
  const [assignment, setAssignment] = useState({
    assigneeType: "USER",
    assigneeId: "",
    availableFrom: "",
    dueAt: "",
    gracePeriodMinutes: 0,
    required: true,
  });
  const rows = data?.classes ?? [];
  const activeClass = rows.find((item) => item.id === classId) ?? rows[0];

  useEffect(() => {
    if (!manage) {
      setSessions(data?.mine ?? []);
      setAssignments([]);
      return;
    }
    if (!classId && rows[0]) setClassId(rows[0].id);
  }, [manage, data?.mine, rows, classId]);

  useEffect(() => {
    if (!manage || !activeClass) return;
    void apiRequest<LiveSessionRow[]>(
      `/api/v1/live-sessions?classId=${activeClass.id}`,
    )
      .then(setSessions)
      .catch(() => setSessions([]));
    if (canAssign) {
      void apiRequest<AssignmentRow[]>(
        `/api/v1/course-assignments?courseId=${activeClass.courseId}`,
      )
        .then(setAssignments)
        .catch(() => setAssignments([]));
    } else setAssignments([]);
  }, [manage, canAssign, activeClass?.id, activeClass?.courseId]);

  async function createLive(event: FormEvent) {
    event.preventDefault();
    if (!activeClass) return;
    setWorking(true);
    try {
      await apiRequest("/api/v1/live-sessions", {
        method: "POST",
        body: JSON.stringify({
          classId: activeClass.id,
          title: live.title,
          provider: live.provider,
          joinUrl: live.joinUrl,
          hostUrl: live.hostUrl || null,
          startsAt: new Date(live.startsAt).toISOString(),
          endsAt: new Date(live.endsAt).toISOString(),
        }),
      });
      setNotice({ tone: "success", message: "Đã lên lịch lớp trực tuyến." });
      setLive({
        title: "",
        provider: "EXTERNAL",
        joinUrl: "",
        hostUrl: "",
        startsAt: "",
        endsAt: "",
      });
      setSessions(
        await apiRequest<LiveSessionRow[]>(
          `/api/v1/live-sessions?classId=${activeClass.id}`,
        ),
      );
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể tạo lịch học",
      });
    } finally {
      setWorking(false);
    }
  }

  async function assignCourse(event: FormEvent) {
    event.preventDefault();
    if (!activeClass || !canAssign) return;
    setWorking(true);
    try {
      const result = await apiRequest<AssignmentRow>(
        "/api/v1/course-assignments",
        {
          method: "POST",
          body: JSON.stringify({
            classId: activeClass.id,
            assigneeType: assignment.assigneeType,
            assigneeId: assignment.assigneeId,
            availableFrom: assignment.availableFrom
              ? new Date(assignment.availableFrom).toISOString()
              : null,
            dueAt: assignment.dueAt
              ? new Date(assignment.dueAt).toISOString()
              : null,
            gracePeriodMinutes: Number(assignment.gracePeriodMinutes),
            required: assignment.required,
          }),
        },
      );
      setNotice({
        tone: "success",
        message: `Đã giao khóa học và tạo ${result.enrolledUsers} ghi danh mới.`,
      });
      setAssignments(
        await apiRequest<AssignmentRow[]>(
          `/api/v1/course-assignments?courseId=${activeClass.courseId}`,
        ),
      );
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể giao khóa học",
      });
    } finally {
      setWorking(false);
    }
  }

  const timeline = (
    <div className="live-timeline">
      {sessions.map((session) => (
        <article key={session.id}>
          <time>
            <strong>
              {new Date(session.startsAt).toLocaleDateString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
              })}
            </strong>
            <span>
              {new Date(session.startsAt).toLocaleTimeString("vi-VN", {
                hour: "2-digit",
                minute: "2-digit",
              })}
            </span>
          </time>
          <span className="live-line">
            <i />
          </span>
          <div>
            <div className="workspace-tags">
              <Tag
                tone={
                  session.status === "LIVE"
                    ? "danger"
                    : session.status === "SCHEDULED"
                      ? "teal"
                      : ""
                }
              >
                {session.status}
              </Tag>
              <Tag>{session.provider}</Tag>
            </div>
            <h3>{session.title}</h3>
            <p>Kết thúc {new Date(session.endsAt).toLocaleString("vi-VN")}</p>
            <a
              className="workspace-button primary"
              href={session.hostUrl ?? session.joinUrl}
              target="_blank"
              rel="noreferrer"
            >
              <Icon name="play" size={15} />{" "}
              {session.hostUrl ? "Mở phòng điều phối" : "Tham gia lớp"}
            </a>
          </div>
        </article>
      ))}
    </div>
  );

  return (
    <>
      <WorkspaceHero
        eyebrow="PHÒNG HỌC LIÊN KẾT"
        title="Lớp trực tuyến & giao khóa học"
        description={
          manage
            ? "Lên lịch lớp trực tuyến, giao khóa cho người dùng hoặc đơn vị và ấn định hạn hoàn thành."
            : "Các buổi trực tuyến dưới đây chỉ thuộc những lớp bạn đang được ghi danh."
        }
        icon={<Icon name="class" size={31} />}
        stats={[
          {
            value: manage
              ? rows.length
              : new Set(sessions.map((item) => item.classId)).size,
            label: manage ? "Lớp phụ trách" : "Lớp của tôi",
          },
          { value: sessions.length, label: "Buổi trực tuyến" },
          { value: assignments.length, label: "Lệnh giao khóa" },
          {
            value: assignments.reduce(
              (sum, item) => sum + item.enrolledUsers,
              0,
            ),
            label: "Ghi danh mới",
            tone: "teal",
          },
        ]}
        actions={
          <Button tone="secondary" onClick={() => void refresh()}>
            <Icon name="refresh" size={16} /> Đồng bộ
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
      ) : manage ? (
        <div className="workspace-grid-three live-layout">
          <Panel
            title="Các lớp học"
            subtitle="Mỗi lớp luôn thuộc một khóa học."
          >
            {rows.length ? (
              <div className="class-selector">
                {rows.map((item) => (
                  <button
                    key={item.id}
                    className={activeClass?.id === item.id ? "active" : ""}
                    onClick={() => setClassId(item.id)}
                  >
                    <span className="class-symbol">
                      {item.code.slice(0, 2)}
                    </span>
                    <div>
                      <strong>{item.name}</strong>
                      <small>{item.code}</small>
                      <Tag tone={item.status === "OPEN" ? "teal" : ""}>
                        {item.status}
                      </Tag>
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <Empty text="Chưa có lớp trong phạm vi của bạn." />
            )}
          </Panel>
          <Panel
            title="Lịch học trực tuyến"
            subtitle={activeClass?.name ?? "Chọn lớp"}
            className="span-two"
          >
            {sessions.length ? (
              timeline
            ) : (
              <Empty text="Lớp chưa có buổi học trực tuyến." />
            )}
          </Panel>
          <Panel
            title="Lên lịch buổi học"
            subtitle="Dùng URL từ Zoom, Teams, Meet, Jitsi hoặc dịch vụ nội bộ."
          >
            <form className="workspace-form" onSubmit={createLive}>
              <Field label="Tên buổi học" wide>
                <input
                  required
                  value={live.title}
                  onChange={(e) => setLive({ ...live, title: e.target.value })}
                />
              </Field>
              <Field label="Provider">
                <input
                  value={live.provider}
                  onChange={(e) =>
                    setLive({ ...live, provider: e.target.value })
                  }
                />
              </Field>
              <Field label="Link tham gia">
                <input
                  type="url"
                  required
                  value={live.joinUrl}
                  onChange={(e) =>
                    setLive({ ...live, joinUrl: e.target.value })
                  }
                />
              </Field>
              <Field label="Link giảng viên" wide>
                <input
                  type="url"
                  value={live.hostUrl}
                  onChange={(e) =>
                    setLive({ ...live, hostUrl: e.target.value })
                  }
                />
              </Field>
              <Field label="Bắt đầu">
                <input
                  type="datetime-local"
                  required
                  value={live.startsAt}
                  onChange={(e) =>
                    setLive({ ...live, startsAt: e.target.value })
                  }
                />
              </Field>
              <Field label="Kết thúc">
                <input
                  type="datetime-local"
                  required
                  value={live.endsAt}
                  onChange={(e) => setLive({ ...live, endsAt: e.target.value })}
                />
              </Field>
              <Button type="submit" disabled={working || !activeClass}>
                <Icon name="calendar" size={16} /> Lên lịch
              </Button>
            </form>
          </Panel>
          {canAssign && (
            <Panel
              title="Giao khóa học theo phạm vi"
              subtitle="Tự mở rộng người dùng trong nhóm, phòng ban hoặc chi nhánh và tạo ghi danh idempotent."
              className="span-two"
            >
              <form className="assignment-form" onSubmit={assignCourse}>
                <Field label="Đối tượng">
                  <select
                    value={assignment.assigneeType}
                    onChange={(e) =>
                      setAssignment({
                        ...assignment,
                        assigneeType: e.target.value,
                      })
                    }
                  >
                    <option>USER</option>
                    <option>GROUP</option>
                    <option>DEPARTMENT</option>
                    <option>BRANCH</option>
                  </select>
                </Field>
                <Field label="ID đối tượng">
                  <input
                    required
                    value={assignment.assigneeId}
                    onChange={(e) =>
                      setAssignment({
                        ...assignment,
                        assigneeId: e.target.value,
                      })
                    }
                  />
                </Field>
                <Field label="Có hiệu lực từ">
                  <input
                    type="datetime-local"
                    value={assignment.availableFrom}
                    onChange={(e) =>
                      setAssignment({
                        ...assignment,
                        availableFrom: e.target.value,
                      })
                    }
                  />
                </Field>
                <Field label="Hạn hoàn thành">
                  <input
                    type="datetime-local"
                    value={assignment.dueAt}
                    onChange={(e) =>
                      setAssignment({ ...assignment, dueAt: e.target.value })
                    }
                  />
                </Field>
                <Field label="Gia hạn (phút)">
                  <input
                    type="number"
                    min={0}
                    max={10080}
                    value={assignment.gracePeriodMinutes}
                    onChange={(e) =>
                      setAssignment({
                        ...assignment,
                        gracePeriodMinutes: Number(e.target.value),
                      })
                    }
                  />
                </Field>
                <label className="workspace-check">
                  <input
                    type="checkbox"
                    checked={assignment.required}
                    onChange={(e) =>
                      setAssignment({
                        ...assignment,
                        required: e.target.checked,
                      })
                    }
                  />
                  <span>Khóa học bắt buộc</span>
                </label>
                <Button type="submit" disabled={working || !activeClass}>
                  <Icon name="target" size={16} /> Giao khóa học
                </Button>
              </form>
              {assignments.length ? (
                <div className="assignment-history">
                  {assignments.slice(0, 8).map((item) => (
                    <div key={item.id}>
                      <span className="assignment-sigil">
                        {item.assigneeType.slice(0, 1)}
                      </span>
                      <div>
                        <strong>
                          {item.assigneeType} · {item.assigneeId}
                        </strong>
                        <small>
                          Hạn:{" "}
                          {item.dueAt
                            ? new Date(item.dueAt).toLocaleString("vi-VN")
                            : "Không giới hạn"}
                        </small>
                      </div>
                      <Tag tone={item.status === "ACTIVE" ? "teal" : ""}>
                        {item.status}
                      </Tag>
                    </div>
                  ))}
                </div>
              ) : null}
            </Panel>
          )}
        </div>
      ) : (
        <div className="workspace-grid-three live-layout participant">
          <Panel
            title="Lịch học của tôi"
            subtitle="Chỉ hiển thị lịch từ các lớp bạn đang tham gia."
            className="span-three"
          >
            {sessions.length ? (
              timeline
            ) : (
              <Empty text="Bạn chưa có buổi học trực tuyến sắp tới." />
            )}
          </Panel>
        </div>
      )}
    </>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Competency framework and account security
// ─────────────────────────────────────────────────────────────────────────────

type CompetencyRow = {
  id: string;
  code: string;
  name: string;
  description?: string;
  category?: string;
  maxLevel: number;
  status: string;
};
type CompetencyGapRow = {
  competencyId: string;
  code: string;
  name: string;
  category?: string;
  currentLevel: number;
  requiredLevel: number;
  gap: number;
  weight: number;
  recommendedCourseIds: string[];
};
type CompetencyGap = {
  readinessPercent: number;
  profileIds: string[];
  gaps: CompetencyGapRow[];
  assessedAt: string;
};
type CompetencyAssessment = {
  id: string;
  competencyId: string;
  competencyCode: string;
  competencyName: string;
  level: number;
  source: string;
  assessedAt: string;
  validUntil?: string;
};

function CompetencyConsole({ user }: { user: PortalUser }) {
  const manage =
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("competencies:manage");
  const { data, loading, error, refresh } = useLoad(async () => {
    const [catalog, gap, assessments] = await Promise.all([
      apiRequest<CompetencyRow[]>("/api/v1/competencies"),
      user.permissions.includes("competencies:read:self")
        ? apiRequest<CompetencyGap>("/api/v1/competencies/me/gaps")
        : Promise.resolve({
            readinessPercent: 0,
            profileIds: [],
            gaps: [],
            assessedAt: new Date().toISOString(),
          }),
      user.permissions.includes("competencies:read:self")
        ? apiRequest<CompetencyAssessment[]>(
            "/api/v1/competencies/me/assessments",
          )
        : Promise.resolve([]),
    ]);
    return { catalog, gap, assessments };
  }, [user.permissions.join("|")]);
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [assessment, setAssessment] = useState({ competencyId: "", level: 1 });
  const [create, setCreate] = useState({
    code: "",
    name: "",
    category: "",
    description: "",
    maxLevel: 5,
  });
  const catalog = data?.catalog ?? [];
  const gap = data?.gap;
  const latest = new Map(
    (data?.assessments ?? []).map((item) => [item.competencyId, item]),
  );

  async function selfAssess(event: FormEvent) {
    event.preventDefault();
    if (!assessment.competencyId) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/competencies/assessments", {
        method: "POST",
        body: JSON.stringify({
          competencyId: assessment.competencyId,
          level: Number(assessment.level),
          source: "SELF",
          evidenceJson: "{}",
        }),
      });
      setNotice({
        tone: "success",
        message:
          "Đã ghi nhận mức tự đánh giá. Kết quả quản lý/kiểm tra vẫn được hiển thị riêng để đối chiếu.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể lưu đánh giá",
      });
    } finally {
      setWorking(false);
    }
  }

  async function createCompetency(event: FormEvent) {
    event.preventDefault();
    if (!manage) return;
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/competencies", {
        method: "POST",
        body: JSON.stringify({
          ...create,
          maxLevel: Number(create.maxLevel),
          active: true,
        }),
      });
      setCreate({
        code: "",
        name: "",
        category: "",
        description: "",
        maxLevel: 5,
      });
      setNotice({
        tone: "success",
        message: "Đã bổ sung năng lực vào danh mục.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể tạo năng lực",
      });
    } finally {
      setWorking(false);
    }
  }

  return (
    <>
      <WorkspaceHero
        eyebrow="KHUNG NĂNG LỰC SỐ"
        title="Bản đồ năng lực & lộ trình phát triển"
        description="Đối chiếu mức hiện tại với chuẩn vị trí, nhìn rõ khoảng cách và các khóa học được đề xuất để bù đắp năng lực còn thiếu."
        icon={<Icon name="target" size={31} />}
        stats={[
          {
            value: `${Math.round(gap?.readinessPercent ?? 0)}%`,
            label: "Mức sẵn sàng",
            tone: "teal",
          },
          {
            value: gap?.gaps.filter((item) => item.gap > 0).length ?? 0,
            label: "Khoảng trống",
          },
          { value: catalog.length, label: "Năng lực" },
          { value: data?.assessments.length ?? 0, label: "Lần đánh giá" },
        ]}
        actions={
          <Button tone="secondary" onClick={() => void refresh()}>
            <Icon name="refresh" size={16} /> Đồng bộ
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
        <div className="workspace-grid-three">
          <Panel
            title="Khoảng cách theo chuẩn vị trí"
            subtitle="Ưu tiên năng lực có độ thiếu và trọng số cao."
            className="span-two"
          >
            {gap?.gaps.length ? (
              <div className="competency-gap-list">
                {gap.gaps.map((item) => (
                  <article key={item.competencyId}>
                    <div>
                      <span className="competency-code">{item.code}</span>
                      <h3>{item.name}</h3>
                      <small>{item.category || "Năng lực chung"}</small>
                    </div>
                    <div className="competency-level">
                      <span>
                        Hiện tại <b>{item.currentLevel}</b>
                      </span>
                      <span>
                        Yêu cầu <b>{item.requiredLevel}</b>
                      </span>
                    </div>
                    <div className="competency-meter">
                      <i
                        style={{
                          width: `${Math.min(100, item.requiredLevel ? (item.currentLevel * 100) / item.requiredLevel : 100)}%`,
                        }}
                      />
                    </div>
                    <Tag tone={item.gap > 0 ? "danger" : "teal"}>
                      {item.gap > 0 ? `Thiếu ${item.gap} mức` : "Đã đạt"}
                    </Tag>
                    {item.recommendedCourseIds.length > 0 && (
                      <p className="competency-recommendation">
                        Khóa học đề xuất:{" "}
                        {item.recommendedCourseIds.map((id) => (
                          <Link key={id} href={`/courses/${id}`}>
                            #{id.slice(0, 8)}
                          </Link>
                        ))}
                      </p>
                    )}
                  </article>
                ))}
              </div>
            ) : (
              <Empty text="Chưa được gán khung năng lực hoặc không còn khoảng trống." />
            )}
          </Panel>
          <Panel
            title="Tự đánh giá"
            subtitle="Tự đánh giá là một nguồn bằng chứng, không tự động thay thế đánh giá của quản lý hoặc kết quả thi."
          >
            <form className="workspace-form" onSubmit={selfAssess}>
              <Field label="Năng lực" wide>
                <select
                  required
                  value={assessment.competencyId}
                  onChange={(e) =>
                    setAssessment({
                      ...assessment,
                      competencyId: e.target.value,
                    })
                  }
                >
                  <option value="">Chọn năng lực</option>
                  {catalog.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.code} · {item.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Mức hiện tại">
                <input
                  type="number"
                  min={0}
                  max={
                    catalog.find((item) => item.id === assessment.competencyId)
                      ?.maxLevel ?? 10
                  }
                  value={assessment.level}
                  onChange={(e) =>
                    setAssessment({
                      ...assessment,
                      level: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Button
                type="submit"
                disabled={working || !assessment.competencyId}
              >
                Ghi nhận đánh giá
              </Button>
            </form>
            <div className="assessment-history">
              {Array.from(latest.values())
                .slice(0, 6)
                .map((item) => (
                  <div key={item.id}>
                    <span>{item.competencyCode}</span>
                    <strong>Mức {item.level}</strong>
                    <small>
                      {item.source} ·{" "}
                      {new Date(item.assessedAt).toLocaleDateString("vi-VN")}
                    </small>
                  </div>
                ))}
            </div>
          </Panel>
          {manage && (
            <Panel
              title="Thêm năng lực"
              subtitle="Quản trị danh mục dùng chung cho khung vị trí và khóa học."
              className="span-three"
            >
              <form className="assignment-form" onSubmit={createCompetency}>
                <Field label="Mã">
                  <input
                    required
                    value={create.code}
                    onChange={(e) =>
                      setCreate({ ...create, code: e.target.value })
                    }
                  />
                </Field>
                <Field label="Tên">
                  <input
                    required
                    value={create.name}
                    onChange={(e) =>
                      setCreate({ ...create, name: e.target.value })
                    }
                  />
                </Field>
                <Field label="Nhóm">
                  <input
                    value={create.category}
                    onChange={(e) =>
                      setCreate({ ...create, category: e.target.value })
                    }
                  />
                </Field>
                <Field label="Mức tối đa">
                  <input
                    type="number"
                    min={1}
                    max={10}
                    value={create.maxLevel}
                    onChange={(e) =>
                      setCreate({ ...create, maxLevel: Number(e.target.value) })
                    }
                  />
                </Field>
                <Field label="Mô tả" wide>
                  <textarea
                    value={create.description}
                    onChange={(e) =>
                      setCreate({ ...create, description: e.target.value })
                    }
                  />
                </Field>
                <Button type="submit" disabled={working}>
                  Tạo năng lực
                </Button>
              </form>
            </Panel>
          )}
        </div>
      )}
    </>
  );
}

type SessionRow = {
  id: string;
  issuedAt: string;
  expiresAt: string;
  revokedAt?: string;
  revokedReason?: string;
  current: boolean;
};

function AccountSecurityConsole() {
  const { data, loading, error, refresh } = useLoad(
    () => apiRequest<SessionRow[]>("/api/v1/auth/sessions"),
    [],
  );
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [password, setPassword] = useState({
    currentPassword: "",
    newPassword: "",
    confirm: "",
  });

  async function changePassword(event: FormEvent) {
    event.preventDefault();
    if (password.newPassword !== password.confirm) {
      setNotice({ tone: "error", message: "Mật khẩu xác nhận không khớp." });
      return;
    }
    setWorking(true);
    try {
      await apiRequest("/api/v1/auth/change-password", {
        method: "POST",
        body: JSON.stringify({
          currentPassword: password.currentPassword,
          newPassword: password.newPassword,
        }),
      });
      setPassword({ currentPassword: "", newPassword: "", confirm: "" });
      setNotice({
        tone: "success",
        message: "Đã đổi mật khẩu và thu hồi các phiên đăng nhập khác.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể đổi mật khẩu",
      });
    } finally {
      setWorking(false);
    }
  }

  async function revoke(id: string) {
    try {
      await apiRequest(`/api/v1/auth/sessions/${id}`, { method: "DELETE" });
      setNotice({ tone: "success", message: "Đã thu hồi phiên đăng nhập." });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể thu hồi phiên",
      });
    }
  }

  async function revokeOthers() {
    try {
      await apiRequest("/api/v1/auth/sessions", { method: "DELETE" });
      setNotice({
        tone: "success",
        message: "Đã thu hồi các phiên đăng nhập khác.",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể thu hồi phiên",
      });
    }
  }

  return (
    <>
      <WorkspaceHero
        eyebrow="AN TOÀN TÀI KHOẢN"
        title="Mật khẩu & phiên đăng nhập"
        description="Kiểm soát các phiên đang hoạt động, thay mật khẩu và đóng ngay các phiên không còn tin cậy."
        icon={<Icon name="settings" size={31} />}
        stats={[
          {
            value: data?.filter((item) => !item.revokedAt).length ?? 0,
            label: "Phiên hoạt động",
          },
          {
            value: data?.filter((item) => item.current).length ?? 0,
            label: "Phiên hiện tại",
            tone: "teal",
          },
        ]}
      />
      <NoticeBar notice={notice} onClose={() => setNotice(null)} />
      <div className="workspace-grid-three">
        <Panel
          title="Đổi mật khẩu"
          subtitle="Mật khẩu mới phải tuân theo chính sách của hệ thống."
        >
          <form className="workspace-form" onSubmit={changePassword}>
            <Field label="Mật khẩu hiện tại" wide>
              <input
                type="password"
                required
                value={password.currentPassword}
                onChange={(e) =>
                  setPassword({ ...password, currentPassword: e.target.value })
                }
              />
            </Field>
            <Field label="Mật khẩu mới" wide>
              <input
                type="password"
                required
                minLength={12}
                value={password.newPassword}
                onChange={(e) =>
                  setPassword({ ...password, newPassword: e.target.value })
                }
              />
            </Field>
            <Field label="Nhập lại mật khẩu" wide>
              <input
                type="password"
                required
                minLength={12}
                value={password.confirm}
                onChange={(e) =>
                  setPassword({ ...password, confirm: e.target.value })
                }
              />
            </Field>
            <Button type="submit" disabled={working}>
              Đổi mật khẩu
            </Button>
          </form>
        </Panel>
        <Panel
          title="Các phiên đăng nhập"
          subtitle="Thu hồi phiên lạ hoặc đóng toàn bộ phiên khác."
          className="span-two"
          action={
            <Button tone="danger" onClick={() => void revokeOthers()}>
              Thu hồi phiên khác
            </Button>
          }
        >
          {loading ? (
            <Busy />
          ) : error ? (
            <NoticeBar
              notice={{ tone: "error", message: error }}
              onClose={() => void refresh()}
            />
          ) : data?.length ? (
            <div className="security-session-list">
              {data.map((item) => (
                <article key={item.id}>
                  <span
                    className={`session-orb ${item.current ? "current" : ""}`}
                  />
                  <div>
                    <strong>
                      {item.current
                        ? "Phiên hiện tại"
                        : `Phiên ${item.id.slice(0, 8)}`}
                    </strong>
                    <small>
                      Tạo {new Date(item.issuedAt).toLocaleString("vi-VN")} ·
                      Hết hạn {new Date(item.expiresAt).toLocaleString("vi-VN")}
                    </small>
                    {item.revokedAt && (
                      <small>
                        Đã thu hồi: {item.revokedReason || "Theo yêu cầu"}
                      </small>
                    )}
                  </div>
                  {!item.current && !item.revokedAt && (
                    <Button tone="danger" onClick={() => void revoke(item.id)}>
                      Thu hồi
                    </Button>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <Empty text="Không có phiên đăng nhập nào." />
          )}
        </Panel>
      </div>
    </>
  );
}

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
