"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent, MouseEventHandler, ReactNode } from "react";
import { apiRequest, createIdempotencyKey, unwrapItems } from "@/lib/api";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { LearningPathCenter } from "./LearningPathCenter";

type Row = Record<string, any>;
type Notice = { tone: "success" | "error" | "info"; message: string } | null;

const centerPermissions: Record<string, string[]> = {
  users: ["users:read", "roles:read"],
  organization: ["organization:read"],
  settings: ["branding:manage", "configuration:manage", "integrations:manage"],
  news: ["news:read", "news:manage"],
  "ai-lab": ["questions:generate:ai", "questions:approve:ai"],
  documents: ["files:read", "files:upload", "files:edit"],
  competitions: ["competitions:participate", "competitions:manage"],
  "live-sessions": ["live-sessions:join", "live-sessions:manage"],
  competencies: [
    "competencies:read:self",
    "competencies:read:scope",
    "competencies:manage",
  ],
  "learning-paths": [
    "learning-paths:read",
    "learning-paths:manage",
    "learning-paths:assign",
  ],
  "account-security": [],
  results: ["grades:read:self", "grade-appeals:create", "grade-appeals:manage"],
};

export const realmSections = new Set(Object.keys(centerPermissions));

export function RealmControlCenter({
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
  if (!allowed) return <RealmDenied />;
  switch (section) {
    case "users":
      return <UserAccessConsole user={user} />;
    case "organization":
      return <OrganizationConsole user={user} />;
    case "settings":
      return <WorldSettingsConsole user={user} />;
    case "news":
      return <NewsConsole user={user} />;
    case "ai-lab":
      return <AiLabConsole user={user} />;
    case "documents":
      return <DocumentStudio user={user} />;
    case "competitions":
      return <CompetitionConsole user={user} />;
    case "live-sessions":
      return <LiveSessionConsole user={user} />;
    case "competencies":
      return <CompetencyConsole user={user} />;
    case "learning-paths":
      return <LearningPathCenter user={user} />;
    case "account-security":
      return <AccountSecurityConsole />;
    case "results":
      return <GradeResultsConsole user={user} />;
    default:
      return <RealmDenied />;
  }
}

function RealmDenied() {
  return (
    <section className="realm-empty-state">
      <span className="realm-empty-rune">◇</span>
      <h1>Cánh cổng đang khóa</h1>
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

function RealmHero({
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
    <header className="realm-hero">
      <div className="realm-hero-glyph" aria-hidden>
        {icon}
        <i />
        <b />
      </div>
      <div className="realm-hero-copy">
        <span>{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
        {actions && <div className="realm-hero-actions">{actions}</div>}
      </div>
      {stats && (
        <div className="realm-hero-stats">
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
    <article className={`realm-panel ${className}`}>
      <header>
        <div>
          <h2>{title}</h2>
          {subtitle && <p>{subtitle}</p>}
        </div>
        {action}
      </header>
      <div className="realm-panel-body">{children}</div>
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
    <div className={`realm-notice ${notice.tone}`} role="status">
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
    <div className="realm-busy">
      <span />
      <p>{label}</p>
    </div>
  );
}
function Empty({ text }: { text: string }) {
  return (
    <div className="realm-inline-empty">
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
    <label className={`realm-field ${wide ? "wide" : ""}`}>
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
function Tag({ children, tone = "" }: { children: ReactNode; tone?: string }) {
  return <span className={`realm-tag ${tone}`}>{children}</span>;
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
      className={`realm-button ${tone}`}
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
type PermissionCatalog = {
  permissions: string[];
  groups: Record<string, string[]>;
  defaultRoles: Record<string, string[]>;
};

function UserAccessConsole({ user }: { user: PortalUser }) {
  const systemAdmin = user.accountType === "SYSTEM_ADMIN";
  const canReadUsers = systemAdmin || user.permissions.includes("users:read");
  const canReadRoles =
    systemAdmin ||
    user.permissions.some((permission) =>
      ["roles:read", "users:read", "authorization:grant"].includes(permission),
    );
  const canGrant =
    systemAdmin || user.permissions.includes("authorization:grant");
  const canManageRoles =
    systemAdmin || user.permissions.includes("roles:manage");
  const { data, loading, error, refresh } = useLoad(async () => {
    const [users, roles, catalog] = await Promise.all([
      canReadUsers
        ? apiRequest<UserRow[] | { items: UserRow[] }>(
            "/api/v1/users?size=1000",
          )
        : Promise.resolve([] as UserRow[]),
      canReadRoles
        ? apiRequest<RoleRow[]>("/api/v1/roles")
        : Promise.resolve([] as RoleRow[]),
      apiRequest<PermissionCatalog>("/api/v1/authorization/catalog"),
    ]);
    return { users: unwrapItems(users), roles, catalog };
  }, [canReadUsers, canReadRoles]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [tab, setTab] = useState<"accounts" | "grant" | "roles" | "bulk">(
    "accounts",
  );
  const [query, setQuery] = useState("");
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  const [create, setCreate] = useState({
    code: "",
    username: "",
    password: "",
    fullName: "",
    email: "",
    roleCode: "LEARNER",
  });
  const [grant, setGrant] = useState({
    kind: "ROLE",
    value: "LEARNER",
    scopeType: "SYSTEM",
    scopeId: "",
    effect: "ALLOW",
  });
  const [role, setRole] = useState({
    code: "",
    name: "",
    permissions: new Set<string>(),
  });
  const [bulkText, setBulkText] = useState(
    "Mã,Tài khoản,Họ tên,Email,Mật khẩu,Vai trò\n",
  );

  const users = data?.users ?? [];
  const roles = data?.roles ?? [];
  const catalog = data?.catalog;
  const filtered = users.filter((item) =>
    `${item.code} ${item.username} ${item.fullName} ${item.email ?? ""}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );
  const canWrite =
    systemAdmin ||
    user.permissions.some((value) =>
      ["users:create", "users:write", "users:bulk-manage"].includes(value),
    );
  const tabs: Array<{
    key: "accounts" | "grant" | "roles" | "bulk";
    label: string;
  }> = [
    ...(canReadUsers ? [{ key: "accounts" as const, label: "Tài khoản" }] : []),
    ...(canGrant && canReadUsers
      ? [{ key: "grant" as const, label: "Cấp quyền hàng loạt" }]
      : []),
    ...(canReadRoles
      ? [{ key: "roles" as const, label: "Role tùy chỉnh" }]
      : []),
    ...(canWrite ? [{ key: "bulk" as const, label: "Nhập hàng loạt" }] : []),
  ];
  useEffect(() => {
    if (!tabs.some((item) => item.key === tab) && tabs[0]) setTab(tabs[0].key);
  }, [tab, canReadUsers, canReadRoles, canGrant, canWrite]);

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
      setNotice({
        tone: "success",
        message: `Đã tạo tài khoản ${create.username}.`,
      });
      setCreate({
        code: "",
        username: "",
        password: "",
        fullName: "",
        email: "",
        roleCode: "LEARNER",
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể tạo tài khoản",
      });
    } finally {
      setWorking(false);
    }
  }

  async function grantAccess(event: FormEvent) {
    event.preventDefault();
    if (!canGrant) return;
    if (!selected.size) {
      setNotice({ tone: "error", message: "Hãy chọn ít nhất một tài khoản." });
      return;
    }
    setWorking(true);
    setNotice(null);
    try {
      await apiRequest("/api/v1/authorization/grants/bulk", {
        method: "POST",
        body: JSON.stringify({
          operationId: createIdempotencyKey("grant"),
          userIds: Array.from(selected),
          grants: [
            {
              roleCode: grant.kind === "ROLE" ? grant.value : null,
              permissionCode: grant.kind === "PERMISSION" ? grant.value : null,
              scopeType: grant.scopeType,
              scopeId: grant.scopeType === "SYSTEM" ? null : grant.scopeId,
              effect: grant.effect,
            },
          ],
        }),
      });
      setNotice({
        tone: "success",
        message: `Đã áp dụng quyền cho ${selected.size} tài khoản.`,
      });
      setSelected(new Set());
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể cấp quyền",
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
      setNotice({
        tone: "success",
        message: `Đã tạo role ${role.code.toUpperCase()}.`,
      });
      setRole({ code: "", name: "", permissions: new Set() });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message: cause instanceof Error ? cause.message : "Không thể tạo role",
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
              roleCodes: (roleCodes || "LEARNER")
                .split("|")
                .map((v) => v.trim())
                .filter(Boolean),
            }),
          ),
        }),
      });
      setNotice({
        tone: "success",
        message: `Đã tạo ${rows.length} tài khoản từ danh sách.`,
      });
      await refresh();
    } catch (cause) {
      setNotice({
        tone: "error",
        message:
          cause instanceof Error ? cause.message : "Không thể nhập tài khoản",
      });
    } finally {
      setWorking(false);
    }
  }

  return (
    <>
      <RealmHero
        eyebrow="ĐIỆN QUẢN TRỊ DANH TÍNH"
        title="Tài khoản, vai trò & quyền hạn"
        description="Mỗi người chỉ có một tài khoản USER; năng lực giảng dạy, học tập hay quản trị được trao bằng role và permission theo đúng phạm vi."
        icon={<Icon name="users" size={31} />}
        stats={[
          { value: users.length, label: "Tài khoản" },
          { value: roles.length, label: "Role khả dụng" },
          { value: catalog?.permissions.length ?? 0, label: "Quyền hệ thống" },
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
      <nav className="realm-tabs">
        {tabs.map(({ key, label }) => (
          <button
            key={key}
            className={tab === key ? "active" : ""}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </nav>
      {loading ? (
        <Busy />
      ) : error ? (
        <NoticeBar
          notice={{ tone: "error", message: error }}
          onClose={() => void refresh()}
        />
      ) : (
        <>
          {tab === "accounts" && (
            <div className="realm-two-column wide-left">
              <Panel
                title="Danh sách tài khoản"
                subtitle="Tài khoản quản trị gốc được đánh dấu và không thể vô hiệu hóa qua luồng thông thường."
                action={
                  <div className="realm-search">
                    <Icon name="search" size={16} />
                    <input
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                      placeholder="Tìm tên, mã, tài khoản…"
                    />
                  </div>
                }
              >
                <div className="realm-table-wrap">
                  <table className="realm-table">
                    <thead>
                      <tr>
                        <th>
                          <input
                            type="checkbox"
                            checked={
                              filtered.length > 0 &&
                              filtered.every((item) => selected.has(item.id))
                            }
                            onChange={(e) =>
                              setSelected(
                                e.target.checked
                                  ? new Set(filtered.map((item) => item.id))
                                  : new Set(),
                              )
                            }
                          />
                        </th>
                        <th>Danh tính</th>
                        <th>Vai trò</th>
                        <th>Loại</th>
                        <th>Trạng thái</th>
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
                              onChange={(e) =>
                                setSelected((current) => {
                                  const next = new Set(current);
                                  e.target.checked
                                    ? next.add(item.id)
                                    : next.delete(item.id);
                                  return next;
                                })
                              }
                            />
                          </td>
                          <td>
                            <div className="realm-primary-cell">
                              <span className="mini-avatar">
                                {item.fullName.slice(0, 1).toUpperCase()}
                              </span>
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
                            <div className="realm-tags">
                              {item.roles.map((value) => (
                                <Tag
                                  key={value}
                                  tone={
                                    value === "ADMIN"
                                      ? "gold"
                                      : value === "INSTRUCTOR"
                                        ? "violet"
                                        : "teal"
                                  }
                                >
                                  {value}
                                </Tag>
                              ))}
                            </div>
                          </td>
                          <td>
                            {item.protectedAccount ? (
                              <Tag tone="gold">SYSTEM ADMIN</Tag>
                            ) : (
                              <Tag>USER</Tag>
                            )}
                          </td>
                          <td>
                            <Tag
                              tone={
                                item.status === "ACTIVE" ? "teal" : "danger"
                              }
                            >
                              {item.status}
                            </Tag>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
              {canWrite && (
                <Panel
                  title="Tạo tài khoản"
                  subtitle="Không có đăng ký công khai. Mọi tài khoản đều do quản trị cấp."
                  className="sticky-panel"
                >
                  <form className="realm-form" onSubmit={createAccount}>
                    <Field label="Mã người dùng">
                      <input
                        required
                        maxLength={80}
                        value={create.code}
                        onChange={(e) =>
                          setCreate({
                            ...create,
                            code: e.target.value.toUpperCase(),
                          })
                        }
                        placeholder="NV001"
                      />
                    </Field>
                    <Field label="Tên đăng nhập">
                      <input
                        required
                        minLength={3}
                        value={create.username}
                        onChange={(e) =>
                          setCreate({ ...create, username: e.target.value })
                        }
                        placeholder="nguyen.an"
                      />
                    </Field>
                    <Field label="Họ và tên" wide>
                      <input
                        required
                        value={create.fullName}
                        onChange={(e) =>
                          setCreate({ ...create, fullName: e.target.value })
                        }
                      />
                    </Field>
                    <Field label="Email" wide>
                      <input
                        type="email"
                        value={create.email}
                        onChange={(e) =>
                          setCreate({ ...create, email: e.target.value })
                        }
                      />
                    </Field>
                    <Field
                      label="Mật khẩu tạm"
                      wide
                      hint="Tối thiểu 12 ký tự; quản trị gửi riêng cho người dùng."
                    >
                      <input
                        required
                        minLength={12}
                        type="password"
                        value={create.password}
                        onChange={(e) =>
                          setCreate({ ...create, password: e.target.value })
                        }
                      />
                    </Field>
                    <Field label="Role khởi tạo" wide>
                      <select
                        value={create.roleCode}
                        onChange={(e) =>
                          setCreate({ ...create, roleCode: e.target.value })
                        }
                      >
                        {roles.map((item) => (
                          <option key={item.code}>{item.code}</option>
                        ))}
                      </select>
                    </Field>
                    <Button type="submit" disabled={working || !canWrite}>
                      <Icon name="plus" size={16} />{" "}
                      {working ? "Đang tạo…" : "Tạo và cấp tài khoản"}
                    </Button>
                  </form>
                </Panel>
              )}
            </div>
          )}
          {tab === "grant" && (
            <div className="realm-two-column">
              <Panel
                title={`Tài khoản nhận quyền (${selected.size})`}
                subtitle="Chọn nhiều tài khoản ở thẻ Tài khoản rồi cấp cùng một role hoặc quyền trong một thao tác."
              >
                {selected.size ? (
                  <div className="selection-cloud">
                    {users
                      .filter((item) => selected.has(item.id))
                      .map((item) => (
                        <button
                          key={item.id}
                          onClick={() =>
                            setSelected((current) => {
                              const next = new Set(current);
                              next.delete(item.id);
                              return next;
                            })
                          }
                        >
                          <span>{item.fullName}</span>
                          <Icon name="close" size={13} />
                        </button>
                      ))}
                  </div>
                ) : (
                  <Empty text="Chưa chọn tài khoản. Quay lại thẻ Tài khoản để chọn nhiều người." />
                )}
              </Panel>
              <Panel
                title="Ấn định quyền"
                subtitle="Phạm vi SYSTEM, chi nhánh, phòng ban, nhóm, khóa học hoặc kỳ thi."
              >
                <form className="realm-form" onSubmit={grantAccess}>
                  <Field label="Loại cấp">
                    <select
                      value={grant.kind}
                      onChange={(e) =>
                        setGrant({
                          ...grant,
                          kind: e.target.value,
                          value:
                            e.target.value === "ROLE"
                              ? "LEARNER"
                              : (catalog?.permissions[0] ?? ""),
                        })
                      }
                    >
                      <option value="ROLE">Role</option>
                      <option value="PERMISSION">Quyền đơn lẻ</option>
                    </select>
                  </Field>
                  <Field label={grant.kind === "ROLE" ? "Role" : "Permission"}>
                    <select
                      value={grant.value}
                      onChange={(e) =>
                        setGrant({ ...grant, value: e.target.value })
                      }
                    >
                      {grant.kind === "ROLE"
                        ? roles.map((item) => (
                            <option key={item.code}>{item.code}</option>
                          ))
                        : catalog?.permissions.map((item) => (
                            <option key={item}>{item}</option>
                          ))}
                    </select>
                  </Field>
                  <Field label="Phạm vi">
                    <select
                      value={grant.scopeType}
                      onChange={(e) =>
                        setGrant({
                          ...grant,
                          scopeType: e.target.value,
                          scopeId:
                            e.target.value === "SYSTEM" ? "" : grant.scopeId,
                        })
                      }
                    >
                      {[
                        "SYSTEM",
                        "BRANCH",
                        "DEPARTMENT",
                        "GROUP",
                        "COURSE",
                        "EXAM",
                      ].map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Hiệu lực">
                    <select
                      value={grant.effect}
                      onChange={(e) =>
                        setGrant({ ...grant, effect: e.target.value })
                      }
                    >
                      <option>ALLOW</option>
                      <option>DENY</option>
                    </select>
                  </Field>
                  {grant.scopeType !== "SYSTEM" && (
                    <Field label="ID đối tượng phạm vi" wide>
                      <input
                        required
                        value={grant.scopeId}
                        onChange={(e) =>
                          setGrant({ ...grant, scopeId: e.target.value })
                        }
                        placeholder="UUID chi nhánh/phòng ban/khóa học/kỳ thi"
                      />
                    </Field>
                  )}
                  <Button
                    type="submit"
                    disabled={working || selected.size === 0}
                  >
                    <Icon name="lock" size={16} />{" "}
                    {working ? "Đang áp dụng…" : "Cấp quyền đồng thời"}
                  </Button>
                </form>
              </Panel>
            </div>
          )}
          {tab === "roles" && (
            <div className="realm-two-column wide-left">
              <Panel
                title="Role hiện có"
                subtitle="Ba role mặc định là mẫu quyền; Admin có thể tạo thêm role riêng."
              >
                <div className="role-card-grid">
                  {roles.map((item) => (
                    <div className="role-card" key={item.id}>
                      <div>
                        <span className="role-rune">
                          {item.code.slice(0, 1)}
                        </span>
                        <div>
                          <strong>{item.name}</strong>
                          <small>{item.code}</small>
                        </div>
                        {item.systemRole && <Tag tone="gold">Mặc định</Tag>}
                      </div>
                      <p>{item.permissions.length} quyền</p>
                      <div className="realm-tags compact">
                        {item.permissions.slice(0, 5).map((permission) => (
                          <Tag key={permission}>{permission}</Tag>
                        ))}
                        {item.permissions.length > 5 && (
                          <Tag>+{item.permissions.length - 5}</Tag>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </Panel>
              {canManageRoles && (
                <Panel
                  title="Tạo role tùy chỉnh"
                  subtitle="Chọn chính xác các năng lực cần thiết; phạm vi được ấn định khi gán role."
                >
                  <form className="realm-form" onSubmit={createRole}>
                    <Field label="Mã role">
                      <input
                        required
                        value={role.code}
                        onChange={(e) =>
                          setRole({
                            ...role,
                            code: e.target.value
                              .toUpperCase()
                              .replace(/\s+/g, "_"),
                          })
                        }
                        placeholder="COURSE_EDITOR"
                      />
                    </Field>
                    <Field label="Tên hiển thị">
                      <input
                        required
                        value={role.name}
                        onChange={(e) =>
                          setRole({ ...role, name: e.target.value })
                        }
                      />
                    </Field>
                    <div className="permission-matrix">
                      {Object.entries(catalog?.groups ?? {}).map(
                        ([group, permissions]) => (
                          <details key={group}>
                            <summary>
                              <span>{group}</span>
                              <small>
                                {
                                  permissions.filter((permission) =>
                                    role.permissions.has(permission),
                                  ).length
                                }
                                /{permissions.length}
                              </small>
                            </summary>
                            {permissions.map((permission) => (
                              <label key={permission}>
                                <input
                                  type="checkbox"
                                  checked={role.permissions.has(permission)}
                                  onChange={(e) =>
                                    setRole((current) => {
                                      const next = new Set(current.permissions);
                                      e.target.checked
                                        ? next.add(permission)
                                        : next.delete(permission);
                                      return { ...current, permissions: next };
                                    })
                                  }
                                />
                                <span>{permission}</span>
                              </label>
                            ))}
                          </details>
                        ),
                      )}
                    </div>
                    <Button
                      type="submit"
                      disabled={working || !role.permissions.size}
                    >
                      <Icon name="save" size={16} /> Tạo role
                    </Button>
                  </form>
                </Panel>
              )}
            </div>
          )}
          {tab === "bulk" && (
            <Panel
              title="Nhập nhiều tài khoản"
              subtitle="Dùng trình nhập Excel/CSV để ánh xạ cột, xem trước và nhận lỗi theo từng dòng; khung dán nhanh được giữ cho danh sách nhỏ."
            >
              <div className="realm-import-actions">
                <Link className="realm-button primary" href="/users/import">
                  <Icon name="upload" size={16} /> Mở trình nhập Excel/CSV
                </Link>
                <span>
                  Hỗ trợ XLSX, CSV, CREATE_ONLY/UPSERT và ATOMIC/PARTIAL.
                </span>
              </div>
              <form onSubmit={importBulk} className="realm-form">
                <Field
                  label="Dán CSV nhanh"
                  wide
                  hint="Mã,Tài khoản,Họ tên,Email,Mật khẩu,Vai trò"
                >
                  <textarea
                    className="code-textarea tall"
                    value={bulkText}
                    onChange={(e) => setBulkText(e.target.value)}
                  />
                </Field>
                <div className="realm-callout">
                  <Icon name="warning" size={18} />
                  <p>
                    Không gửi mật khẩu tạm qua kênh công khai. Sau khi nhập, lưu
                    kết quả ở nơi kiểm soát truy cập.
                  </p>
                </div>
                <Button type="submit" disabled={working}>
                  <Icon name="upload" size={16} />{" "}
                  {working ? "Đang nhập…" : "Tạo từ danh sách dán"}
                </Button>
              </form>
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
    userIds: "",
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
      .split(/[\s,;]+/)
      .map((v) => v.trim())
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
      setMembership({ ...membership, userIds: "" });
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
      <RealmHero
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
        <div className="realm-grid-three org-layout">
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
              <form className="realm-form" onSubmit={createUnit}>
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
              <form className="realm-form" onSubmit={grantMembers}>
                <Field
                  label="User ID"
                  wide
                  hint="Mỗi UUID cách nhau bằng dấu phẩy hoặc xuống dòng."
                >
                  <textarea
                    value={membership.userIds}
                    onChange={(e) =>
                      setMembership({ ...membership, userIds: e.target.value })
                    }
                  />
                </Field>
                <Field label="Loại membership">
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
                <label className="realm-check">
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
  const [tab, setTab] = useState<"brand" | "services">(
    canBrand ? "brand" : "services",
  );
  const [brand, setBrand] = useState<BrandingRow | null>(null);
  const [service, setService] = useState({
    serviceType: "REDIS",
    configKey: "default",
    enabled: true,
    configJson: '{\n  "host": "redis",\n  "port": 6379\n}',
    secret: "",
  });
  const [notice, setNotice] = useState<Notice>(null);
  const [working, setWorking] = useState(false);
  useEffect(() => {
    if (data?.branding) setBrand(data.branding);
  }, [data?.branding]);
  useEffect(() => {
    if (!canBrand && canServices) setTab("services");
  }, [canBrand, canServices]);
  const services = data?.services ?? [];

  async function saveBrand(event: FormEvent) {
    event.preventDefault();
    if (!canBrand || !brand) return;
    setWorking(true);
    try {
      const saved = await apiRequest<BrandingRow>("/api/v1/branding", {
        method: "PUT",
        body: JSON.stringify({
          systemName: brand.systemName,
          introduction: brand.introduction || null,
          logoFileId: brand.logoFileId || null,
          faviconFileId: brand.faviconFileId || null,
          backgroundFileId: brand.backgroundFileId || null,
          primaryColor: brand.primaryColor,
          secondaryColor: brand.secondaryColor,
          backgroundColor: brand.backgroundColor,
          textColor: brand.textColor,
          customDomain: brand.customDomain || null,
        }),
      });
      setBrand(saved);
      setNotice({
        tone: "success",
        message:
          "Đã cập nhật nhận diện. Giao diện mới áp dụng sau khi tải lại trang.",
      });
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

  async function addService(event: FormEvent) {
    event.preventDefault();
    if (!canServices) return;
    setWorking(true);
    try {
      const config = JSON.parse(service.configJson);
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
      <RealmHero
        eyebrow="ĐỊNH HÌNH THẾ GIỚI"
        title="Thương hiệu & dịch vụ bên thứ ba"
        description="Khách hàng tự đổi tên, logo, lời giới thiệu, bảng màu, hình nền, tên miền và chỉ bật Redis, AI, email, họp trực tuyến hay trình sửa tài liệu khi thực sự cần."
        icon={<Icon name="settings" size={31} />}
        stats={[
          { value: brand?.systemName ?? "—", label: "Tên hệ thống" },
          {
            value: services.filter((item) => item.enabled).length,
            label: "Dịch vụ đang bật",
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
      <nav className="realm-tabs">
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
      ) : tab === "brand" && canBrand && brand ? (
        <div className="realm-two-column wide-left">
          <Panel
            title="Bộ nhận diện"
            subtitle="Màu sắc được kiểm soát bằng mã HEX để giữ tính nhất quán."
          >
            <form className="realm-form branding-form" onSubmit={saveBrand}>
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
                    ["secondaryColor", "Màu phụ"],
                    ["backgroundColor", "Màu nền"],
                    ["textColor", "Màu chữ"],
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
                          })
                        }
                      />
                      <input
                        value={brand[key]}
                        onChange={(event) =>
                          setBrand({
                            ...brand,
                            [key]: event.target.value.toUpperCase(),
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
                    background: `linear-gradient(135deg, ${brand.primaryColor}, ${brand.secondaryColor})`,
                  }}
                >
                  L
                </span>
                <strong>{brand.systemName}</strong>
              </header>
              <h3>Mỗi hành trình là một chòm sao.</h3>
              <p>
                {brand.introduction || "Không gian học tập riêng của tổ chức."}
              </p>
              <button
                type="button"
                style={{
                  background: `linear-gradient(135deg, ${brand.primaryColor}, ${brand.secondaryColor})`,
                }}
              >
                Bắt đầu khám phá
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
        <div className="realm-two-column wide-left">
          <Panel
            title="Dịch vụ đã cấu hình"
            subtitle="Khóa bí mật không bao giờ được trả lại giao diện."
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
            subtitle="Redis là tùy chọn; cấu hình chỉ được dùng khi khách hàng bật dịch vụ."
          >
            <form className="realm-form" onSubmit={addService}>
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
              <Field label="JSON cấu hình" wide>
                <textarea
                  className="code-textarea"
                  value={service.configJson}
                  onChange={(event) =>
                    setService({ ...service, configJson: event.target.value })
                  }
                />
              </Field>
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
              <label className="realm-check">
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
        <RealmDenied />
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
      <RealmHero
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
        <div className={`realm-two-column ${manage ? "wide-left" : "single"}`}>
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
                        <div className="realm-tags">
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
                      <div className="realm-tags">
                        {item.attachmentFileIds.map((fileId, index) => (
                          <a
                            className="realm-button ghost"
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
              <form className="realm-form" onSubmit={createNews}>
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
                <label className="realm-check">
                  <input
                    type="checkbox"
                    checked={draft.pinned}
                    onChange={(e) =>
                      setDraft({ ...draft, pinned: e.target.checked })
                    }
                  />
                  <span>Ghim lên đầu</span>
                </label>
                <label className="realm-check">
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
      <RealmHero
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
      <nav className="realm-tabs">
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
        <div className="realm-two-column wide-left">
          <Panel
            title="Nguồn tri thức"
            subtitle="Có thể chọn file DOCX/PDF đã tải lên hoặc dán nội dung trực tiếp."
          >
            <form className="realm-form" onSubmit={runGeneration}>
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
            <form className="realm-form compact">
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
        <div className="realm-two-column wide-left">
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
            <form className="realm-form" onSubmit={saveProvider}>
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
              <label className="realm-check">
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
        <div className="realm-two-column ai-review-layout">
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
              <div className="realm-callout danger">
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
      <RealmHero
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
        <div className="realm-grid-three document-layout">
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
                        className="realm-button ghost"
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
      <RealmHero
        eyebrow="ĐẤU TRƯỜNG TRI THỨC"
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
          className={`realm-grid-three competition-layout ${manage ? "" : "participant"}`}
        >
          <Panel
            title="Các đấu trường"
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
                      <div className="realm-tags">
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
                  className="realm-button primary"
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
                <div className="realm-form">
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
                    <Icon name="plus" size={16} /> Tạo đấu trường
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
            <div className="realm-tags">
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
              className="realm-button primary"
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
      <RealmHero
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
        <div className="realm-grid-three live-layout">
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
                    <span className="class-rune">{item.code.slice(0, 2)}</span>
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
            <form className="realm-form" onSubmit={createLive}>
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
                <label className="realm-check">
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
        <div className="realm-grid-three live-layout participant">
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
      <RealmHero
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
        <div className="realm-grid-three">
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
            <form className="realm-form" onSubmit={selfAssess}>
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
      <RealmHero
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
      <div className="realm-grid-three">
        <Panel
          title="Đổi mật khẩu"
          subtitle="Mật khẩu mới phải tuân theo chính sách của hệ thống."
        >
          <form className="realm-form" onSubmit={changePassword}>
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
      <RealmHero
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
        <div className="realm-grid-three">
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
              <form className="realm-form" onSubmit={createAppeal}>
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
              <form className="realm-form" onSubmit={resolveAppeal}>
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
