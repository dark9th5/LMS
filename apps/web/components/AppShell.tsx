"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import type { PublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import { resolvePortalRole, roleLabel, type PortalRole } from "@/lib/role";
import { PORTAL_PATHS } from "@/lib/portal-paths";
import type { IconName, PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { NotificationBell } from "./NotificationBell";

type ExtendedPortalUser = PortalUser & {
  systemName?: string;
  logoUrl?: string;
};

type NavItem = {
  href: string;
  label: string;
  hint: string;
  icon: IconName;
};

type RoleNavigation = {
  workspaceLabel: string;
  items: NavItem[];
};

const ROLE_NAVIGATION: Record<PortalRole, RoleNavigation> = {
  ADMIN: {
    workspaceLabel: "Không gian quản trị",
    items: [
      { href: "/admin", label: "Tổng quan", hint: "Vận hành và chỉ số hệ thống", icon: "dashboard" },
      { href: PORTAL_PATHS.ADMIN.users, label: "Người dùng", hint: "Tài khoản và vai trò", icon: "users" },
      { href: PORTAL_PATHS.ADMIN.organization, label: "Tổ chức", hint: "Cơ cấu và thành viên", icon: "building" },
      { href: PORTAL_PATHS.ADMIN.reports, label: "Báo cáo", hint: "Báo cáo quản trị", icon: "report" },
      { href: PORTAL_PATHS.ADMIN.settings, label: "Cài đặt", hint: "Thương hiệu và kết nối", icon: "settings" },
    ],
  },
  INSTRUCTOR: {
    workspaceLabel: "Không gian giảng viên",
    items: [
      { href: "/instructor", label: "Tổng quan", hint: "Công việc giảng dạy", icon: "dashboard" },
      { href: PORTAL_PATHS.INSTRUCTOR.courses, label: "Khóa học", hint: "Bài học và bài kiểm tra", icon: "book" },
      { href: PORTAL_PATHS.INSTRUCTOR.exams, label: "Bài thi", hint: "Kỳ thi độc lập", icon: "exam" },
      { href: PORTAL_PATHS.INSTRUCTOR.grading, label: "Chấm điểm", hint: "Bài thực hành và tự luận", icon: "grade" },
      { href: PORTAL_PATHS.INSTRUCTOR.reports, label: "Báo cáo", hint: "Tiến độ học viên", icon: "report" },
    ],
  },
  STUDENT: {
    workspaceLabel: "Không gian học viên",
    items: [
      { href: "/student", label: "Tổng quan", hint: "Lịch học và việc cần làm", icon: "dashboard" },
      { href: PORTAL_PATHS.STUDENT.courses, label: "Khóa học", hint: "Nội dung đang học", icon: "book" },
      { href: PORTAL_PATHS.STUDENT.exams, label: "Bài thi", hint: "Kỳ thi được giao", icon: "exam" },
      { href: PORTAL_PATHS.STUDENT.results, label: "Kết quả", hint: "Điểm và phản hồi", icon: "report" },
      { href: PORTAL_PATHS.STUDENT.certificates, label: "Chứng chỉ", hint: "Chứng nhận đã đạt", icon: "certificate" },
    ],
  },
};

function matchesPath(path: string, href: string): boolean {
  return path === href || path.startsWith(`${href}/`);
}

export function AppShell({
  user,
  branding,
  children,
}: {
  user: PortalUser;
  branding?: PublicBranding;
  children: ReactNode;
}) {
  const path = usePathname();
  const role = resolvePortalRole(user);
  const navigation = ROLE_NAVIGATION[role];
  const [mobileOpen, setMobileOpen] = useState(false);
  const [commandOpen, setCommandOpen] = useState(false);
  const [commandQuery, setCommandQuery] = useState("");
  const [darkMode, setDarkMode] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const extended = user as ExtendedPortalUser;
  const current = navigation.items.find((item) => matchesPath(path, item.href));
  const commandItems = useMemo(() => {
    const query = commandQuery.trim().toLocaleLowerCase("vi-VN");
    if (!query) return navigation.items;
    return navigation.items.filter((item) =>
      `${item.label} ${item.hint}`.toLocaleLowerCase("vi-VN").includes(query),
    );
  }, [commandQuery, navigation.items]);

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
    setDarkMode(document.documentElement.dataset.theme === "unified-dark");
    setSidebarCollapsed(window.localStorage.getItem("lms-sidebar-collapsed") === "true");
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

  function toggleSidebar() {
    const next = !sidebarCollapsed;
    setSidebarCollapsed(next);
    window.localStorage.setItem("lms-sidebar-collapsed", String(next));
  }

  function closeCommand() {
    setCommandOpen(false);
    setCommandQuery("");
  }

  return (
    <div
      className={`app-shell role-${role.toLowerCase()} ${sidebarCollapsed ? "sidebar-collapsed" : ""}`}
      data-portal-role={role}
    >
      <a className="skip-link" href="#main-content">Chuyển đến nội dung chính</a>

      {mobileOpen && (
        <button type="button" className="mobile-overlay" aria-label="Đóng menu" onClick={() => setMobileOpen(false)} />
      )}

      <aside className={`app-sidebar ${mobileOpen ? "is-open" : ""}`}>
        <div className="sidebar-brand-row">
          <Link href={landingForUser(user)} className="app-brand" onClick={() => setMobileOpen(false)}>
            {logoUrl ? <img className="brand-image" src={logoUrl} alt="" /> : <span className="brand-mark" aria-hidden="true">L</span>}
            <span className="brand-copy"><strong>{systemName}</strong></span>
          </Link>
          <button type="button" className="icon-button sidebar-collapse" onClick={toggleSidebar} aria-label={sidebarCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"} title={sidebarCollapsed ? "Mở rộng" : "Thu gọn"}>
            <Icon name="chevron" />
          </button>
          <button type="button" className="icon-button sidebar-close" onClick={() => setMobileOpen(false)} aria-label="Đóng menu"><Icon name="close" /></button>
        </div>

        <nav className="app-nav" aria-label={`Điều hướng ${roleLabel(role)}`}>
          <div className="sidebar-links">
            {navigation.items.map((item) => {
              const active = matchesPath(path, item.href);
              return (
                <Link key={item.href} href={item.href} className={`app-nav-link ${active ? "active" : ""}`} aria-current={active ? "page" : undefined} title={sidebarCollapsed ? item.label : undefined} onClick={() => setMobileOpen(false)}>
                  <span className="app-nav-icon"><Icon name={item.icon} size={19} /></span>
                  <span className="app-nav-copy"><strong>{item.label}</strong></span>
                </Link>
              );
            })}
          </div>
        </nav>

        <div className="sidebar-footer">
          <button type="button" className="sidebar-search" onClick={() => setCommandOpen(true)}>
            <Icon name="search" size={18} /><span>Tìm nhanh</span><kbd>Ctrl K</kbd>
          </button>
          <div className="sidebar-profile">
            <span className="avatar">{initials || "LP"}</span>
            <span><strong>{user.fullName}</strong><small>{roleLabel(role)}</small></span>
            <form action="/api/auth/logout" method="post"><button className="icon-button" title="Đăng xuất" aria-label="Đăng xuất"><Icon name="logout" size={18} /></button></form>
          </div>
        </div>
      </aside>

      <div className="app-workspace">
        <header className="app-topbar">
          <div className="topbar-leading">
            <button type="button" className="icon-button mobile-menu" onClick={() => setMobileOpen(true)} aria-label="Mở menu"><Icon name="menu" /></button>
            <div className="topbar-context"><small>{navigation.workspaceLabel}</small><strong>{current?.label ?? systemName}</strong></div>
          </div>
          <button type="button" className="topbar-search" onClick={() => setCommandOpen(true)} aria-label="Tìm kiếm và chuyển trang">
            <Icon name="search" size={18} /><span>Tìm trong chức năng của {roleLabel(role).toLocaleLowerCase("vi-VN")}</span><kbd>Ctrl K</kbd>
          </button>
          <div className="topbar-actions">
            <button type="button" className="icon-button theme-toggle" onClick={toggleTheme} aria-label={darkMode ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối"} title={darkMode ? "Giao diện sáng" : "Giao diện tối"}><span aria-hidden="true">{darkMode ? "☀" : "☾"}</span></button>
            <NotificationBell />
            <span className="topbar-profile"><span className="avatar">{initials || "LP"}</span><span><strong>{user.fullName}</strong><small>{roleLabel(role)}</small></span></span>
          </div>
        </header>

        <main className="app-content" id="main-content" tabIndex={-1}>{children}</main>
      </div>

      {commandOpen && (
        <div className="command-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeCommand(); }}>
          <section className="command-dialog" role="dialog" aria-modal="true" aria-label="Tìm nhanh">
            <header className="command-header"><Icon name="search" size={20} /><input autoFocus value={commandQuery} onChange={(event) => setCommandQuery(event.target.value)} placeholder="Nhập tên chức năng" /><button type="button" className="icon-button" onClick={closeCommand} aria-label="Đóng"><Icon name="close" /></button></header>
            <div className="command-results">
              {commandItems.length ? commandItems.map((item) => (
                <Link key={item.href} href={item.href} onClick={closeCommand}><span className="command-icon"><Icon name={item.icon} /></span><span><strong>{item.label}</strong><span className="command-hint">{item.hint}</span></span><Icon name="arrow" size={17} /></Link>
              )) : <div className="command-empty"><Icon name="search" size={28} /><strong>Không tìm thấy kết quả</strong><p>Thử một từ khóa ngắn hơn.</p></div>}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
