"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import type { IconName, PortalUser } from "@/lib/types";
import type { PublicBranding } from "@/lib/branding";
import { roleLabel } from "@/lib/models";
import { Icon } from "./Icon";
import { MysticBackdrop } from "./MysticBackdrop";
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
  roles?: string[];
  permissions?: string[];
};

type NavGroup = { label: string; items: NavItem[] };

const navGroups: NavGroup[] = [
  {
    label: "Trung tâm",
    items: [
      {
        href: "/dashboard",
        label: "Bảng chỉ huy",
        hint: "Toàn cảnh hệ thống",
        icon: "dashboard",
        roles: ["ADMIN", "INSTRUCTOR"],
        permissions: ["reports:read:scope"],
      },
      {
        href: "/learning",
        label: "Hành trình học",
        hint: "Khóa học được giao",
        icon: "learn",
        roles: ["STUDENT", "LEARNER"],
        permissions: ["courses:learn"],
      },
      {
        href: "/learning-paths",
        label: "Lộ trình phát triển",
        hint: "Chuỗi lớp và mục tiêu năng lực",
        icon: "target",
        permissions: [
          "learning-paths:read",
          "learning-paths:manage",
          "learning-paths:assign",
        ],
      },
      {
        href: "/courses",
        label: "Thư viện khóa học",
        hint: "Kiến tạo và quản lý",
        icon: "book",
        roles: ["ADMIN", "INSTRUCTOR"],
        permissions: ["courses:create", "courses:update", "courses:learn"],
      },
      {
        href: "/classes",
        label: "Lớp học",
        hint: "Nhóm học và lịch đào tạo",
        icon: "class",
        roles: ["ADMIN", "INSTRUCTOR"],
        permissions: ["classes:read", "classes:manage"],
      },
      {
        href: "/live-sessions",
        label: "Phòng học liên kết",
        hint: "Trực tuyến và giao khóa",
        icon: "play",
        permissions: [
          "live-sessions:join",
          "live-sessions:manage",
          "courses:assign",
        ],
      },
      {
        href: "/news",
        label: "Bản tin học viện",
        hint: "Thông báo và tin nội bộ",
        icon: "bell",
        permissions: ["news:read", "news:manage"],
      },
      {
        href: "/account-security",
        label: "Bảo mật tài khoản",
        hint: "Mật khẩu và phiên đăng nhập",
        icon: "settings",
      },
    ],
  },
  {
    label: "Thử thách",
    items: [
      {
        href: "/exams",
        label: "Kỳ thi độc lập",
        hint: "Đề thi ngoài khóa học",
        icon: "exam",
        roles: ["ADMIN", "INSTRUCTOR", "STUDENT", "LEARNER"],
        permissions: ["assessments:take", "assessment:take", "exams:manage"],
      },
      {
        href: "/competitions",
        label: "Đấu trường tri thức",
        hint: "Xếp hạng và phần thưởng",
        icon: "target",
        permissions: ["competitions:participate", "competitions:manage"],
      },
      {
        href: "/ai-lab",
        label: "Xưởng AI câu hỏi",
        hint: "Sinh, duyệt và nhập đề",
        icon: "question",
        permissions: ["questions:generate:ai", "questions:approve:ai"],
      },
      {
        href: "/documents",
        label: "Thư viện sống",
        hint: "Biên tập DOCX và PDF",
        icon: "file",
        permissions: ["files:read", "files:download", "files:edit"],
      },
      {
        href: "/grading",
        label: "Điện chấm điểm",
        hint: "Đánh giá và phản hồi",
        icon: "grade",
        roles: ["ADMIN", "INSTRUCTOR"],
        permissions: ["assessments:grade"],
      },
      {
        href: "/results",
        label: "Kết quả & phúc khảo",
        hint: "Điểm số, lịch sử và phản hồi",
        icon: "grade",
        permissions: [
          "grades:read:self",
          "grade-appeals:create",
          "grade-appeals:manage",
        ],
      },

      {
        href: "/competencies",
        label: "Bản đồ năng lực",
        hint: "Khoảng cách và lộ trình phát triển",
        icon: "target",
        permissions: [
          "competencies:read:self",
          "competencies:read:scope",
          "competencies:manage",
        ],
      },
      {
        href: "/certificates",
        label: "Thành tựu",
        hint: "Chứng chỉ và phần thưởng",
        icon: "certificate",
        roles: ["ADMIN", "STUDENT", "LEARNER"],
        permissions: ["reports:read:self", "reports:read:scope"],
      },
      {
        href: "/reports",
        label: "Tinh đồ báo cáo",
        hint: "Tiến độ và hiệu quả",
        icon: "report",
        roles: ["ADMIN", "INSTRUCTOR"],
        permissions: ["reports:read:self", "reports:read:scope"],
      },
    ],
  },
  {
    label: "Quản trị",
    items: [
      {
        href: "/users",
        label: "Người dùng & quyền",
        hint: "Tài khoản, role, phạm vi",
        icon: "users",
        roles: ["ADMIN"],
        permissions: ["users:read", "roles:read"],
      },
      {
        href: "/organization",
        label: "Cơ cấu tổ chức",
        hint: "Chi nhánh và phòng ban",
        icon: "building",
        roles: ["ADMIN"],
        permissions: ["organization:read"],
      },
      {
        href: "/notification-automation",
        label: "Tự động thông báo",
        hint: "Mẫu, email và nhắc hạn",
        icon: "bell",
        roles: ["ADMIN"],
        permissions: [
          "notification-templates:manage",
          "notification-reminders:manage",
        ],
      },
      {
        href: "/operations",
        label: "Vận hành",
        hint: "Dịch vụ và sức khỏe",
        icon: "operations",
        roles: ["ADMIN"],
        permissions: ["audit:read", "configuration:manage"],
      },
      {
        href: "/settings",
        label: "Định hình thế giới",
        hint: "Thương hiệu và tích hợp",
        icon: "settings",
        roles: ["ADMIN"],
        permissions: [
          "branding:manage",
          "configuration:manage",
          "integrations:manage",
        ],
      },
    ],
  },
];

export function PortalShell({
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
  const extended = user as ExtendedPortalUser;
  const roles = useMemo(() => new Set(user?.roles ?? []), [user?.roles]);
  const permissions = useMemo(
    () => new Set(extended.permissions ?? []),
    [extended.permissions],
  );
  const isSystemAdmin = extended.accountType === "SYSTEM_ADMIN";
  const hasPermissionData = permissions.size > 0;

  const canSee = (item: NavItem) => {
    if (isSystemAdmin) return true;
    if (hasPermissionData && item.permissions?.length)
      return item.permissions.some((permission) => permissions.has(permission));
    return !item.roles || item.roles.some((role) => roles.has(role));
  };

  const visibleGroups = navGroups
    .map((group) => ({ ...group, items: group.items.filter(canSee) }))
    .filter((group) => group.items.length > 0);
  const initials = (user?.fullName ?? "")
    .split(" ")
    .filter(Boolean)
    .slice(-2)
    .map((value) => value[0])
    .join("")
    .toUpperCase();
  const active = (href: string) =>
    path === href || (href !== "/dashboard" && path.startsWith(`${href}/`));
  const current = visibleGroups
    .flatMap((group) => group.items)
    .find((item) => active(item.href));
  const currentGroup = visibleGroups.find((group) =>
    group.items.some((item) => active(item.href)),
  );
  const visibleItems = visibleGroups.flatMap((group) =>
    group.items.map((item) => ({ ...item, group: group.label })),
  );
  const normalizedCommand = commandQuery.trim().toLocaleLowerCase("vi-VN");
  const commandItems = visibleItems.filter(
    (item) =>
      !normalizedCommand ||
      `${item.label} ${item.hint} ${item.group}`
        .toLocaleLowerCase("vi-VN")
        .includes(normalizedCommand),
  );
  const systemName = branding?.systemName || extended.systemName || "LMSPilot";
  const logoUrl = branding?.logoUrl || extended.logoUrl;
  const learnerOnly =
    (roles.has("STUDENT") || roles.has("LEARNER")) && roles.size === 1;

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

  function closeCommand() {
    setCommandOpen(false);
    setCommandQuery("");
  }

  return (
    <div className="app-shell immersive-shell">
      <MysticBackdrop compact />
      {open && (
        <button
          className="mobile-overlay"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        />
      )}
      <aside className={`sidebar arcane-sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="sidebar-crown" aria-hidden="true">
          <i />
          <span>✦</span>
          <i />
        </div>
        <div className="sidebar-top">
          <Link
            className="brand arcane-brand"
            href={learnerOnly ? "/learning" : "/dashboard"}
            onClick={() => setOpen(false)}
          >
            {logoUrl ? (
              <img className="brand-image" src={logoUrl} alt="" />
            ) : (
              <span className="brand-logo rune-mark">
                <i>L</i>
                <b aria-hidden="true" />
              </span>
            )}
            <span className="brand-copy">
              <strong>{systemName}</strong>
              <small>HỌC VIỆN HUYỀN TRI</small>
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
        <div className="sidebar-context realm-status">
          <span className="context-dot" />
          <div>
            <strong>Realm network</strong>
            <small>On-Premise · Mã hóa nội bộ</small>
          </div>
          <span className="realm-status-code">01</span>
        </div>
        <nav className="sidebar-nav" aria-label="Điều hướng chính">
          {visibleGroups.map((group, groupIndex) => (
            <div className="nav-group" key={group.label}>
              <span className="nav-group-label">
                <i>{String(groupIndex + 1).padStart(2, "0")}</i>
                {group.label}
                <b />
              </span>
              {group.items.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`nav-link ${active(item.href) ? "active" : ""}`}
                  onClick={() => setOpen(false)}
                  title={item.hint}
                >
                  <span className="nav-icon">
                    <Icon name={item.icon} size={20} />
                  </span>
                  <span className="nav-copy">
                    <b>{item.label}</b>
                    <small>{item.hint}</small>
                  </span>
                  {active(item.href) && <i />}
                </Link>
              ))}
            </div>
          ))}
        </nav>
        <button
          className="sidebar-command"
          type="button"
          onClick={() => setCommandOpen(true)}
        >
          <span>
            <Icon name="search" size={17} />
          </span>
          <div>
            <strong>Dịch chuyển nhanh</strong>
            <small>Tìm mọi khu vực</small>
          </div>
          <kbd>⌘ K</kbd>
        </button>
        <div className="sidebar-user">
          <span className="avatar avatar-rune">{initials || "LP"}</span>
          <div>
            <strong>{user.fullName}</strong>
            <small>
              {isSystemAdmin ? "Quản trị tối cao" : roleLabel(user.roles)}
            </small>
          </div>
          <form action="/api/auth/logout" method="post">
            <button
              className="icon-button"
              title="Đăng xuất"
              aria-label="Đăng xuất"
            >
              <Icon name="logout" size={20} />
            </button>
          </form>
        </div>
      </aside>
      <div className="app-main">
        <header className="topbar arcane-topbar">
          <div className="topbar-left">
            <button
              className="icon-button mobile-menu"
              onClick={() => setOpen(true)}
              aria-label="Mở menu"
            >
              <Icon name="menu" />
            </button>
            <div className="topbar-orb" aria-hidden="true">
              <span />
            </div>
            <div className="topbar-title">
              <small>
                {currentGroup?.label ?? "Không gian chính"} / Đang khám phá
              </small>
              <strong>{current?.label ?? systemName}</strong>
              <span>{current?.hint ?? roleLabel(user.roles)}</span>
            </div>
          </div>
          <button
            className="topbar-command"
            type="button"
            onClick={() => setCommandOpen(true)}
            aria-label="Tìm kiếm và chuyển trang"
          >
            <Icon name="search" size={18} />
            <span>Tìm học phần, lớp học, công cụ…</span>
            <kbd>⌘ K</kbd>
          </button>
          <div className="topbar-actions">
            <div className="system-status">
              <span />
              <div>
                <b>Hệ thống ổn định</b>
                <small>Đồng bộ thời gian thực</small>
              </div>
            </div>
            <NotificationBell />
            <div className="top-profile">
              <span className="avatar avatar-rune">{initials || "LP"}</span>
              <div>
                <strong>{user.fullName}</strong>
                <small>
                  {isSystemAdmin ? "SYSTEM ADMIN" : roleLabel(user.roles)}
                </small>
              </div>
            </div>
          </div>
        </header>
        <main className="app-content arcane-content">
          <div className="content-glow" aria-hidden="true" />
          <div className="content-coordinate" aria-hidden="true">
            <span>REALM // CLS</span>
            <i />
          </div>
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
            className="command-palette"
            role="dialog"
            aria-modal="true"
            aria-label="Dịch chuyển nhanh"
          >
            <header>
              <span className="command-sigil" aria-hidden="true">
                ✦
              </span>
              <label>
                <Icon name="search" />
                <input
                  autoFocus
                  value={commandQuery}
                  onChange={(event) => setCommandQuery(event.target.value)}
                  placeholder="Bạn muốn đi đến đâu?"
                />
              </label>
              <kbd>ESC</kbd>
            </header>
            <div className="command-caption">
              <span>ĐIỀU HƯỚNG HỌC VIỆN</span>
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
                  <span>◇</span>
                  <strong>Không tìm thấy cánh cổng</strong>
                  <p>Thử tên tính năng hoặc khu vực khác.</p>
                </div>
              )}
            </div>
            <footer>
              <span>
                <kbd>↵</kbd> mở khu vực
              </span>
              <span>
                <kbd>ESC</kbd> đóng
              </span>
              <b>{systemName} Command Atlas</b>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
