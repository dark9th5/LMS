"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import type { IconName, PortalUser } from "@/lib/types";
import type { PublicBranding } from "@/lib/branding";
import { landingForUser } from "@/lib/authorization";
import { CosmicField } from "./CosmicField";
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
    summary: "Khóa học, lớp và lịch",
    accent: "cyan",
    icon: "book",
    items: [
      {
        href: "/dashboard",
        label: "Tổng quan",
        hint: "Nhịp vận hành hôm nay",
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
        href: "/learning-paths",
        label: "Lộ trình phát triển",
        hint: "Mục tiêu và các chặng học",
        icon: "target",
        permissions: [
          "learning-paths:read",
          "learning-paths:manage",
          "learning-paths:assign",
        ],
      },
      {
        href: "/courses",
        label: "Khóa học",
        hint: "Nội dung đào tạo",
        icon: "book",
        permissions: ["courses:create", "courses:update", "courses:learn"],
      },
      {
        href: "/classes",
        label: "Lớp đào tạo",
        hint: "Nhóm học và lịch trình",
        icon: "class",
        permissions: ["classes:read", "classes:manage"],
      },
      {
        href: "/live-sessions",
        label: "Lịch trực tuyến",
        hint: "Phòng học và sự kiện live",
        icon: "play",
        permissions: [
          "live-sessions:join",
          "live-sessions:manage",
          "courses:assign",
        ],
      },
      {
        href: "/news",
        label: "Bản tin",
        hint: "Thông báo của tổ chức",
        icon: "bell",
        permissions: ["news:read", "news:manage"],
      },
      {
        href: "/account-security",
        label: "Bảo mật tài khoản",
        hint: "Mật khẩu và phiên đăng nhập",
        icon: "lock",
      },
    ],
  },
  {
    label: "Đánh giá",
    summary: "Thi, điểm và chứng chỉ",
    accent: "indigo",
    icon: "exam",
    items: [
      {
        href: "/exams",
        label: "Kỳ thi",
        hint: "Bài kiểm tra và đề độc lập",
        icon: "exam",
        permissions: ["assessments:read", "assessments:take", "assessment:take", "assessments:create", "assessments:update", "exams:manage"],
      },
      {
        href: "/competitions",
        label: "Cuộc thi",
        hint: "Thử thách, xếp hạng, phần thưởng",
        icon: "target",
        permissions: ["competitions:participate", "competitions:manage"],
      },
      {
        href: "/ai-lab",
        label: "AI Studio",
        hint: "Sinh và duyệt câu hỏi",
        icon: "question",
        permissions: ["questions:generate:ai", "questions:approve:ai"],
      },
      {
        href: "/documents",
        label: "Tài liệu",
        hint: "DOCX, PDF và phiên bản",
        icon: "file",
        permissions: ["files:read", "files:download", "files:edit"],
      },
      {
        href: "/grading",
        label: "Chấm điểm",
        hint: "Hàng chờ và phản hồi",
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
        href: "/competencies",
        label: "Năng lực",
        hint: "Khoảng cách và mức sẵn sàng",
        icon: "target",
        permissions: [
          "competencies:read:self",
          "competencies:read:scope",
          "competencies:manage",
        ],
      },
      {
        href: "/certificates",
        label: "Chứng chỉ",
        hint: "Thành tựu đã được xác minh",
        icon: "certificate",
        permissions: ["certificates:read:self", "certificates:manage"],
      },
      {
        href: "/reports",
        label: "Báo cáo",
        hint: "Hiệu quả và tiến độ",
        icon: "report",
        permissions: ["reports:read:self", "reports:read:scope", "reports:kpi:read"],
      },
    ],
  },
  {
    label: "Quản trị",
    summary: "Tổ chức, người dùng, hệ thống",
    accent: "violet",
    icon: "settings",
    items: [
      {
        href: "/users",
        label: "Người dùng & quyền",
        hint: "Tài khoản, role và phạm vi",
        icon: "users",
        permissions: ["users:read", "roles:read", "authorization:grant", "authorization:revoke"],
      },
      {
        href: "/organization",
        label: "Tổ chức",
        hint: "Chi nhánh và phòng ban",
        icon: "building",
        permissions: ["organization:read"],
      },
      {
        href: "/notification-automation",
        label: "Tự động thông báo",
        hint: "Mẫu, email và nhắc hạn",
        icon: "bell",
        permissions: [
          "notification-templates:manage",
          "notification-reminders:manage",
        ],
      },
      {
        href: "/operations",
        label: "Vận hành hệ thống",
        hint: "Dịch vụ và sức khỏe",
        icon: "operations",
        permissions: ["audit:read", "operations:manage", "license:manage", "configuration:manage"],
      },
      {
        href: "/settings",
        label: "Thiết lập thương hiệu",
        hint: "Giao diện và tích hợp",
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
      <CosmicField compact />
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
              <small>LEARNING MANAGEMENT PLATFORM</small>
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

        <div className="sidebar-live-card">
          <div>
            <span className="live-pulse" />
            <b>Hệ thống hoạt động bình thường</b>
          </div>
          <small>KẾT NỐI DỊCH VỤ AN TOÀN</small>
          <i aria-hidden="true" />
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
                {isSystemAdmin ? "Quản trị hệ thống" : `${user.roles.length} gói quyền`}
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
            <span className="topbar-date">
              <small>HỆ THỐNG</small>
              <b>Dữ liệu đang đồng bộ</b>
            </span>
            <NotificationBell />
            <span className="avatar cosmic-avatar">{initials || "LP"}</span>
          </div>
        </header>
        <main className="app-content cosmic-content">
          <div className="content-ribbon" aria-hidden="true">
            <span>LEARN</span>
            <i />
            <span>CREATE</span>
            <i />
            <span>GROW</span>
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
