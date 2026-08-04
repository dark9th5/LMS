"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import type { IconName, PortalUser } from "@/lib/types";
import type { PublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import { Icon } from "./Icon";
import { NotificationBell } from "./NotificationBell";

type ExtendedPortalUser = PortalUser & {
  accountType?: "SYSTEM_ADMIN" | "USER";
  permissions?: string[];
  systemName?: string;
  logoUrl?: string;
};

type NavItem = {
  href: string;
  label: string;
  hint: string;
  icon: IconName;
  permissions?: string[];
};

type NavGroup = {
  label: string;
  summary: string;
  accent: string;
  icon: IconName;
  items: NavItem[];
};

function matchesPath(path: string, href: string) {
  return path === href || (href !== "/dashboard" && path.startsWith(`${href}/`));
}

const navGroups: NavGroup[] = [
  {
    label: "Học tập",
    summary: "Khóa học và lớp học",
    accent: "blue",
    icon: "book",
    items: [
      {
        href: "/dashboard",
        label: "Tổng quan",
        hint: "Việc cần làm và tiến độ",
        icon: "dashboard",
        permissions: ["reports:read:scope", "reports:kpi:read", "courses:create", "courses:update", "classes:manage", "assessments:grade", "users:read"],
      },
      {
        href: "/learning",
        label: "Học tập của tôi",
        hint: "Tiếp tục nội dung đang học",
        icon: "learn",
        permissions: ["courses:learn", "learning:read:self"],
      },
      {
        href: "/courses",
        label: "Khóa học",
        hint: "Nội dung và tài liệu học",
        icon: "book",
        permissions: ["courses:create", "courses:update", "courses:learn"],
      },
      {
        href: "/classes",
        label: "Lớp học",
        hint: "Ghi danh và lịch đào tạo",
        icon: "class",
        permissions: ["classes:read", "classes:manage"],
      },
    ],
  },
  {
    label: "Thi & đánh giá",
    summary: "Bài thi, điểm và kết quả",
    accent: "indigo",
    icon: "exam",
    items: [
      {
        href: "/exams",
        label: "Bài kiểm tra & kỳ thi",
        hint: "Tạo đề hoặc làm bài được giao",
        icon: "exam",
        permissions: ["assessments:read", "assessments:take", "assessment:take", "assessments:create", "assessments:update", "exams:manage"],
      },
      {
        href: "/grading",
        label: "Chấm điểm",
        hint: "Câu tự luận và phản hồi",
        icon: "grade",
        permissions: ["assessments:grade", "grading:manage"],
      },
      {
        href: "/results",
        label: "Kết quả",
        hint: "Điểm số và phúc khảo",
        icon: "report",
        permissions: ["grades:read:self", "grade-appeals:create", "grade-appeals:manage"],
      },
      {
        href: "/reports",
        label: "Báo cáo",
        hint: "Tiến độ và hiệu quả đào tạo",
        icon: "report",
        permissions: ["reports:read:self", "reports:read:scope", "reports:kpi:read"],
      },
    ],
  },
  {
    label: "Quản trị",
    summary: "Người dùng và cài đặt",
    accent: "slate",
    icon: "settings",
    items: [
      {
        href: "/users",
        label: "Người dùng & quyền",
        hint: "Tài khoản và gói công việc",
        icon: "users",
        permissions: ["users:read", "roles:read", "authorization:grant", "authorization:revoke"],
      },
      {
        href: "/organization",
        label: "Tổ chức",
        hint: "Chi nhánh, phòng ban và nhóm",
        icon: "building",
        permissions: ["organization:read"],
      },
      {
        href: "/settings",
        label: "Cài đặt",
        hint: "Giao diện, thương hiệu và kết nối",
        icon: "settings",
        permissions: ["branding:manage", "configuration:manage", "integrations:manage"],
      },
    ],
  },
];

export function CosmicShell({
  user,
  branding,
  children,
}: {
  user: PortalUser;
  branding?: PublicBranding;
  children: React.ReactNode;
}) {
  const path = usePathname();
  const [open, setOpen] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [commandQuery, setCommandQuery] = useState("");
  const [expandedGroup, setExpandedGroup] = useState<string | null>(() =>
    navGroups.find((group) =>
      group.items.some((item) => matchesPath(path, item.href)),
    )?.label ?? navGroups[0].label,
  );
  const extended = user as ExtendedPortalUser;
  const permissions = useMemo(
    () => new Set(extended.permissions ?? []),
    [extended.permissions],
  );
  const isSystemAdmin = extended.accountType === "SYSTEM_ADMIN";

  const visibleGroups = navGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        if (isSystemAdmin) return true;
        if (!item.permissions?.length) return true;
        return item.permissions.some((permission) => permissions.has(permission));
      }),
    }))
    .filter((group) => group.items.length > 0);

  const active = (href: string) => matchesPath(path, href);
  const current = visibleGroups
    .flatMap((group) => group.items)
    .find((item) => active(item.href));
  const currentGroup = visibleGroups.find((group) =>
    group.items.some((item) => active(item.href)),
  );
  const initials = user.fullName
    .split(" ")
    .filter(Boolean)
    .slice(-2)
    .map((value) => value[0])
    .join("")
    .toUpperCase();
  const systemName = branding?.systemName || extended.systemName || "LMSPilot";
  const logoUrl = branding?.logoUrl || extended.logoUrl;
  const commandItems = visibleGroups
    .flatMap((group) =>
      group.items.map((item) => ({ ...item, group: group.label })),
    )
    .filter((item) => {
      const query = commandQuery.trim().toLocaleLowerCase("vi-VN");
      return (
        !query ||
        `${item.label} ${item.hint} ${item.group}`
          .toLocaleLowerCase("vi-VN")
          .includes(query)
      );
    });

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setCommandOpen((value) => !value);
      }
      if (event.key === "Escape") setCommandOpen(false);
    };
    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, []);

  useEffect(() => {
    if (currentGroup) setExpandedGroup(currentGroup.label);
  }, [path, currentGroup?.label]);

  function closeCommand() {
    setCommandOpen(false);
    setCommandQuery("");
  }

  return (
    <div className="app-shell cosmic-shell">
      {open && (
        <button
          className="mobile-overlay"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        />
      )}
      <aside className={`cosmic-sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="sidebar-top">
          <Link
            className="cosmic-brand"
            href={landingForUser(user)}
            onClick={() => setOpen(false)}
          >
            {logoUrl ? (
              <img className="brand-image" src={logoUrl} alt="" />
            ) : (
              <span className="mission-mark" aria-hidden="true">
                <i />
                <b />
                <em />
              </span>
            )}
            <span>
              <strong>{systemName}</strong>
              <small>HỆ THỐNG QUẢN LÝ HỌC TẬP</small>
            </span>
          </Link>
          <button
            className="icon-button sidebar-close"
            onClick={() => setOpen(false)}
            aria-label="Đóng menu"
          >
            <Icon name="close" />
          </button>
        </div>


        <nav className="cosmic-nav" aria-label="Điều hướng chính">
          {visibleGroups.map((group, groupIndex) => {
            const expanded = expandedGroup === group.label;
            const panelId = `primary-nav-${groupIndex + 1}`;
            return (
            <div
              className={`nav-group accent-${group.accent} ${expanded ? "expanded" : ""}`}
              key={group.label}
            >
              <button
                type="button"
                className="nav-group-toggle"
                aria-expanded={expanded}
                aria-controls={panelId}
                onClick={() =>
                  setExpandedGroup((current) =>
                    current === group.label ? null : group.label,
                  )
                }
              >
                <span className="nav-group-main-icon">
                  <Icon name={group.icon} size={18} />
                </span>
                <span className="nav-group-copy">
                  <strong>{group.label}</strong>
                  <small>{group.summary}</small>
                </span>
                <span className="nav-group-count">{group.items.length}</span>
                <Icon
                  name="chevron"
                  size={16}
                  className="nav-group-chevron"
                />
              </button>
              <div
                id={panelId}
                className="nav-group-panel"
                aria-hidden={!expanded}
              >
                <div className="nav-group-items">
                  <div className="nav-group-items-inner">
                    {group.items.map((item) => (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={`nav-link ${active(item.href) ? "active" : ""}`}
                        onClick={() => setOpen(false)}
                        title={item.hint}
                      >
                        <span className="nav-icon">
                          <Icon name={item.icon} size={18} />
                        </span>
                        <span className="nav-copy">
                          <b>{item.label}</b>
                          <small>{item.hint}</small>
                        </span>
                        <span className="nav-arrow">
                          <Icon name="chevron" size={14} />
                        </span>
                      </Link>
                    ))}
                  </div>
                </div>
              </div>
            </div>
            );
          })}
        </nav>

        <div className="sidebar-bottom">
          <button
            className="sidebar-command"
            type="button"
            onClick={() => setCommandOpen(true)}
          >
            <Icon name="search" size={17} />
            <span>
              <strong>Tìm nhanh</strong>
              <small>Điều hướng theo quyền</small>
            </span>
            <kbd>⌘K</kbd>
          </button>
          <div className="sidebar-user">
            <span className="avatar cosmic-avatar">{initials || "LP"}</span>
            <div>
              <strong>{user.fullName}</strong>
              <small>
                {isSystemAdmin ? "Quản trị hệ thống" : "Tài khoản người dùng"}
              </small>
            </div>
            <form action="/api/auth/logout" method="post">
              <button
                className="icon-button"
                title="Đăng xuất"
                aria-label="Đăng xuất"
              >
                <Icon name="logout" size={19} />
              </button>
            </form>
          </div>
        </div>
      </aside>

      <div className="cosmic-workspace">
        <header className="cosmic-topbar">
          <div className="topbar-left">
            <button
              className="icon-button mobile-menu"
              onClick={() => setOpen(true)}
              aria-label="Mở menu"
            >
              <Icon name="menu" />
            </button>
            <div className="topbar-context">
              <small>{currentGroup?.label ?? "Không gian làm việc"}</small>
              <strong>{current?.label ?? systemName}</strong>
            </div>
          </div>
          <button
            className="topbar-command"
            type="button"
            onClick={() => setCommandOpen(true)}
            aria-label="Tìm kiếm và chuyển trang"
          >
            <Icon name="search" size={18} />
            <span>Tìm khóa học, lớp, báo cáo...</span>
            <kbd>⌘ K</kbd>
          </button>
          <div className="topbar-actions">
            <span className="topbar-date"><small>Xin chào</small><b>{user.fullName}</b></span>
            <NotificationBell />
            <span className="avatar cosmic-avatar">{initials || "LP"}</span>
          </div>
        </header>
        <main className="app-content cosmic-content">
          {children}
        </main>
      </div>

      {commandOpen && (
        <div
          className="command-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeCommand();
          }}
        >
          <section
            className="command-palette cosmic-command"
            role="dialog"
            aria-modal="true"
            aria-label="Tìm nhanh"
          >
            <div className="command-colorbar" aria-hidden="true" />
            <header>
              <span className="command-badge">NAV</span>
              <label>
                <Icon name="search" />
                <input
                  autoFocus
                  value={commandQuery}
                  onChange={(event) => setCommandQuery(event.target.value)}
                  placeholder="Bạn muốn mở khu vực nào?"
                />
              </label>
              <kbd>ESC</kbd>
            </header>
            <div className="command-caption">
              <span>ĐIỀU HƯỚNG THEO QUYỀN</span>
              <b>{commandItems.length} kết quả</b>
            </div>
            <div className="command-results">
              {commandItems.length ? (
                commandItems.map((item, index) => (
                  <Link
                    href={item.href}
                    key={item.href}
                    onClick={closeCommand}
                    className={active(item.href) ? "current" : ""}
                  >
                    <span className="command-index">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <span className="command-icon">
                      <Icon name={item.icon} />
                    </span>
                    <span className="command-copy">
                      <strong>{item.label}</strong>
                      <small>
                        {item.group} · {item.hint}
                      </small>
                    </span>
                    <Icon name="arrow" size={17} />
                  </Link>
                ))
              ) : (
                <div className="command-empty">
                  <span>?</span>
                  <strong>Không tìm thấy khu vực phù hợp</strong>
                  <p>Thử một từ khóa ngắn hơn.</p>
                </div>
              )}
            </div>
            <footer>
              <span>
                <kbd>↵</kbd> mở
              </span>
              <span>
                <kbd>ESC</kbd> đóng
              </span>
              <b>{systemName} · ĐIỀU HƯỚNG NHANH</b>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
