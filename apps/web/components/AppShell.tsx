"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import type { PublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import type { IconName, PortalUser } from "@/lib/types";
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
  items: NavItem[];
};

const NAVIGATION: NavGroup[] = [
  {
    label: "Học tập",
    items: [
      {
        href: "/dashboard",
        label: "Tổng quan",
        hint: "Việc cần làm hôm nay",
        icon: "dashboard",
        permissions: [
          "reports:read:scope",
          "reports:kpi:read",
          "courses:create",
          "courses:update",
          "classes:manage",
          "assessments:grade",
          "users:read",
        ],
      },
      {
        href: "/learning",
        label: "Học tập của tôi",
        hint: "Tiếp tục bài đang học",
        icon: "learn",
        permissions: ["courses:learn", "learning:read:self"],
      },
      {
        href: "/courses",
        label: "Khóa học",
        hint: "Nội dung và bài học",
        icon: "book",
        permissions: ["courses:create", "courses:update", "courses:learn"],
      },
      {
        href: "/classes",
        label: "Lớp học",
        hint: "Lịch và danh sách học viên",
        icon: "class",
        permissions: ["classes:read", "classes:manage"],
      },
    ],
  },
  {
    label: "Đánh giá",
    items: [
      {
        href: "/exams",
        label: "Bài kiểm tra & kỳ thi",
        hint: "Tạo đề hoặc làm bài",
        icon: "exam",
        permissions: [
          "assessments:read",
          "assessments:take",
          "assessment:take",
          "assessments:create",
          "assessments:update",
          "exams:manage",
        ],
      },
      {
        href: "/grading",
        label: "Chấm điểm",
        hint: "Tự luận và phản hồi",
        icon: "grade",
        permissions: ["assessments:grade", "grading:manage"],
      },
      {
        href: "/results",
        label: "Kết quả",
        hint: "Điểm số và phúc khảo",
        icon: "report",
        permissions: [
          "grades:read:self",
          "grade-appeals:create",
          "grade-appeals:manage",
        ],
      },
      {
        href: "/reports",
        label: "Báo cáo",
        hint: "Tiến độ và hiệu quả",
        icon: "report",
        permissions: [
          "reports:read:self",
          "reports:read:scope",
          "reports:kpi:read",
        ],
      },
    ],
  },
  {
    label: "Quản trị",
    items: [
      {
        href: "/users",
        label: "Người dùng & quyền",
        hint: "Tài khoản và gói công việc",
        icon: "users",
        permissions: [
          "users:read",
          "roles:read",
          "authorization:grant",
          "authorization:revoke",
        ],
      },
      {
        href: "/organization",
        label: "Tổ chức",
        hint: "Chi nhánh, phòng ban, nhóm",
        icon: "building",
        permissions: ["organization:read"],
      },
      {
        href: "/settings",
        label: "Cài đặt",
        hint: "Giao diện và kết nối",
        icon: "settings",
        permissions: [
          "branding:manage",
          "configuration:manage",
          "integrations:manage",
        ],
      },
    ],
  },
];

function matchesPath(path: string, href: string): boolean {
  return path === href || (href !== "/dashboard" && path.startsWith(`${href}/`));
}

export function AppShell({
  user,
  branding,
  children,
}: {
  user: PortalUser;
  branding?: PublicBranding;
  children: React.ReactNode;
}) {
  const path = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [commandQuery, setCommandQuery] = useState("");
  const [darkMode, setDarkMode] = useState(false);
  const extended = user as ExtendedPortalUser;
  const permissionSet = useMemo(
    () => new Set(extended.permissions ?? []),
    [extended.permissions],
  );
  const isSystemAdmin = extended.accountType === "SYSTEM_ADMIN";

  const groups = useMemo(
    () =>
      NAVIGATION.map((group) => ({
        ...group,
        items: group.items.filter((item) => {
          if (isSystemAdmin || !item.permissions?.length) return true;
          return item.permissions.some((permission) => permissionSet.has(permission));
        }),
      })).filter((group) => group.items.length > 0),
    [isSystemAdmin, permissionSet],
  );

  const allItems = groups.flatMap((group) =>
    group.items.map((item) => ({ ...item, group: group.label })),
  );
  const current = allItems.find((item) => matchesPath(path, item.href));
  const commandItems = allItems.filter((item) => {
    const query = commandQuery.trim().toLocaleLowerCase("vi-VN");
    if (!query) return true;
    return `${item.label} ${item.hint} ${item.group}`
      .toLocaleLowerCase("vi-VN")
      .includes(query);
  });

  const initials = user.fullName
    .split(" ")
    .filter(Boolean)
    .slice(-2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  const systemName = branding?.systemName || extended.systemName || "LMSPilot";
  const logoUrl = branding?.logoUrl || extended.logoUrl;

  useEffect(() => {
    const theme = document.documentElement.dataset.theme;
    setDarkMode(theme === "unified-dark");
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setCommandOpen((value) => !value);
      }
      if (event.key === "Escape") {
        setCommandOpen(false);
        setMobileOpen(false);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  function toggleTheme() {
    const next = darkMode ? "unified-light" : "unified-dark";
    document.documentElement.dataset.theme = next;
    window.localStorage.setItem("lms-theme", next);
    setDarkMode(!darkMode);
  }

  function closeCommand() {
    setCommandOpen(false);
    setCommandQuery("");
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Chuyển đến nội dung chính
      </a>

      {mobileOpen && (
        <button
          type="button"
          className="mobile-overlay"
          aria-label="Đóng menu"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside className={`app-sidebar ${mobileOpen ? "is-open" : ""}`}>
        <div className="sidebar-brand-row">
          <Link
            href={landingForUser(user)}
            className="app-brand"
            onClick={() => setMobileOpen(false)}
          >
            {logoUrl ? (
              <img className="brand-image" src={logoUrl} alt="" />
            ) : (
              <span className="brand-mark" aria-hidden="true">
                L
              </span>
            )}
            <span className="brand-copy">
              <strong>{systemName}</strong>
              <small>Không gian học tập</small>
            </span>
          </Link>
          <button
            type="button"
            className="icon-button sidebar-close"
            onClick={() => setMobileOpen(false)}
            aria-label="Đóng menu"
          >
            <Icon name="close" />
          </button>
        </div>

        <nav className="app-nav" aria-label="Điều hướng chính">
          {groups.map((group) => (
            <section className="sidebar-section" key={group.label}>
              <h2>{group.label}</h2>
              <div className="sidebar-links">
                {group.items.map((item) => {
                  const active = matchesPath(path, item.href);
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`app-nav-link ${active ? "active" : ""}`}
                      aria-current={active ? "page" : undefined}
                      onClick={() => setMobileOpen(false)}
                    >
                      <span className="app-nav-icon">
                        <Icon name={item.icon} size={19} />
                      </span>
                      <span className="app-nav-copy">
                        <strong>{item.label}</strong>
                        <small>{item.hint}</small>
                      </span>
                      <Icon name="chevron" size={15} className="app-nav-arrow" />
                    </Link>
                  );
                })}
              </div>
            </section>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button
            type="button"
            className="sidebar-search"
            onClick={() => setCommandOpen(true)}
          >
            <Icon name="search" size={18} />
            <span>Tìm nhanh</span>
            <kbd>Ctrl K</kbd>
          </button>
          <div className="sidebar-profile">
            <span className="avatar">{initials || "LP"}</span>
            <span>
              <strong>{user.fullName}</strong>
              <small>{isSystemAdmin ? "Quản trị hệ thống" : "Người dùng"}</small>
            </span>
            <form action="/api/auth/logout" method="post">
              <button className="icon-button" title="Đăng xuất" aria-label="Đăng xuất">
                <Icon name="logout" size={18} />
              </button>
            </form>
          </div>
        </div>
      </aside>

      <div className="app-workspace">
        <header className="app-topbar">
          <div className="topbar-leading">
            <button
              type="button"
              className="icon-button mobile-menu"
              onClick={() => setMobileOpen(true)}
              aria-label="Mở menu"
            >
              <Icon name="menu" />
            </button>
            <div className="topbar-context">
              <small>{current?.group ?? "Không gian làm việc"}</small>
              <strong>{current?.label ?? systemName}</strong>
            </div>
          </div>

          <button
            type="button"
            className="topbar-search"
            onClick={() => setCommandOpen(true)}
            aria-label="Tìm kiếm và chuyển trang"
          >
            <Icon name="search" size={18} />
            <span>Tìm khóa học, lớp học, báo cáo...</span>
            <kbd>Ctrl K</kbd>
          </button>

          <div className="topbar-actions">
            <button
              type="button"
              className="icon-button theme-toggle"
              onClick={toggleTheme}
              aria-label={darkMode ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối"}
              title={darkMode ? "Giao diện sáng" : "Giao diện tối"}
            >
              <span aria-hidden="true">{darkMode ? "☀" : "☾"}</span>
            </button>
            <NotificationBell />
            <span className="topbar-profile">
              <span className="avatar">{initials || "LP"}</span>
              <span>
                <strong>{user.fullName}</strong>
                <small>{isSystemAdmin ? "Quản trị hệ thống" : "Tài khoản của tôi"}</small>
              </span>
            </span>
          </div>
        </header>

        <main className="app-content" id="main-content" tabIndex={-1}>
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
            className="command-dialog"
            role="dialog"
            aria-modal="true"
            aria-label="Tìm nhanh"
          >
            <header className="command-header">
              <Icon name="search" size={20} />
              <input
                autoFocus
                value={commandQuery}
                onChange={(event) => setCommandQuery(event.target.value)}
                placeholder="Nhập tên khu vực cần mở"
              />
              <button type="button" className="icon-button" onClick={closeCommand} aria-label="Đóng">
                <Icon name="close" />
              </button>
            </header>
            <div className="command-results">
              {commandItems.length ? (
                commandItems.map((item) => (
                  <Link key={item.href} href={item.href} onClick={closeCommand}>
                    <span className="command-icon">
                      <Icon name={item.icon} />
                    </span>
                    <span>
                      <strong>{item.label}</strong>
                      <small>{item.group} · {item.hint}</small>
                    </span>
                    <Icon name="arrow" size={17} />
                  </Link>
                ))
              ) : (
                <div className="command-empty">
                  <Icon name="search" size={28} />
                  <strong>Không tìm thấy kết quả</strong>
                  <p>Thử một từ khóa ngắn hơn.</p>
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
