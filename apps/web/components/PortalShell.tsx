"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useMemo, useState } from "react";
import type { IconName, PortalUser } from "@/lib/types";
import { roleLabel } from "@/lib/models";
import { Icon } from "./Icon";
import { NotificationBell } from "./NotificationBell";

const nav: { href: string; label: string; icon: IconName; roles?: string[] }[] = [
  { href: "/dashboard", label: "Tổng quan", icon: "dashboard", roles: ["ADMIN", "INSTRUCTOR"] },
  { href: "/learning", label: "Học tập của tôi", icon: "learn", roles: ["STUDENT"] },
  { href: "/courses", label: "Khóa học", icon: "book", roles: ["ADMIN", "INSTRUCTOR"] },
  { href: "/classes", label: "Lớp đào tạo", icon: "class", roles: ["ADMIN", "INSTRUCTOR"] },
  { href: "/exams", label: "Bài kiểm tra", icon: "exam", roles: ["ADMIN", "INSTRUCTOR", "STUDENT"] },
  { href: "/grading", label: "Chấm điểm", icon: "grade", roles: ["ADMIN", "INSTRUCTOR"] },
  { href: "/reports", label: "Báo cáo", icon: "report", roles: ["ADMIN", "INSTRUCTOR"] },
  { href: "/users", label: "Người dùng", icon: "users", roles: ["ADMIN"] },
  { href: "/organization", label: "Cơ cấu tổ chức", icon: "building", roles: ["ADMIN"] },
  { href: "/certificates", label: "Chứng chỉ", icon: "certificate", roles: ["ADMIN", "STUDENT"] },
  { href: "/operations", label: "Vận hành", icon: "operations", roles: ["ADMIN"] },
  { href: "/settings", label: "Cấu hình", icon: "settings", roles: ["ADMIN"] },
];

export function PortalShell({ user, children }: { user: PortalUser; children: React.ReactNode }) {
  const path = usePathname();
  const [open, setOpen] = useState(false);
  const roles = useMemo(() => new Set(user?.roles ?? []), [user?.roles]);
  const visible = nav.filter((item) => !item.roles || item.roles.some((role) => roles.has(role)));
  const initials = (user?.fullName ?? "").split(" ").filter(Boolean).slice(-2).map((value) => value[0]).join("").toUpperCase();
  const active = (href: string) => path === href || (href !== "/dashboard" && path.startsWith(`${href}/`));

  return <div className="app-shell">
    {open && <button className="mobile-overlay" aria-label="Đóng menu" onClick={() => setOpen(false)} />}
    <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
      <div className="sidebar-top">
        <Link className="brand" href={roles.has("STUDENT") && roles.size === 1 ? "/learning" : "/dashboard"} onClick={() => setOpen(false)}>
          <span className="brand-logo">L</span>
          <span className="brand-copy"><strong>LMSPilot</strong><small>Learning Management</small></span>
        </Link>
        <button className="icon-button sidebar-close" onClick={() => setOpen(false)} aria-label="Đóng menu"><Icon name="close"/></button>
      </div>

      <div className="sidebar-context">
        <span className="context-dot"/>
        <div><strong>Hệ thống nội bộ</strong><small>On-Premise · LAN</small></div>
      </div>

      <nav className="sidebar-nav" aria-label="Điều hướng chính">
        {visible.map((item) => <Link key={item.href} href={item.href} className={`nav-link ${active(item.href) ? "active" : ""}`} onClick={() => setOpen(false)}>
          <Icon name={item.icon} size={21}/><span>{item.label}</span>{active(item.href) && <i/>}
        </Link>)}
      </nav>

      <div className="sidebar-user">
        <span className="avatar">{initials || "LP"}</span>
        <div><strong>{user.fullName}</strong><small>{roleLabel(user.roles)}</small></div>
        <form action="/api/auth/logout" method="post"><button className="icon-button" title="Đăng xuất" aria-label="Đăng xuất"><Icon name="logout" size={20}/></button></form>
      </div>
    </aside>

    <div className="app-main">
      <header className="topbar">
        <div className="topbar-left">
          <button className="icon-button mobile-menu" onClick={() => setOpen(true)} aria-label="Mở menu"><Icon name="menu"/></button>
          <div className="topbar-title"><strong>LMSPilot</strong><span>{roleLabel(user.roles)}</span></div>
        </div>
        <div className="topbar-actions">
          <div className="system-status"><span/>Hệ thống trực tuyến</div>
          <NotificationBell />
          <div className="top-profile"><span className="avatar">{initials || "LP"}</span><div><strong>{user.fullName}</strong><small>{roleLabel(user.roles)}</small></div></div>
        </div>
      </header>
      <main className="app-content">{children}</main>
    </div>
  </div>;
}
