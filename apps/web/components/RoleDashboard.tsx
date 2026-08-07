"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import { resolvePortalRole, roleLabel, type PortalRole } from "@/lib/role";
import { PORTAL_PATHS } from "@/lib/portal-paths";
import type { IconName, PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { ProgressBar } from "./ProgressBar";

type Row = Record<string, unknown>;
type State = {
  primary: Row[];
  secondary: Row[];
  tertiary: Row[];
  notifications: { unread: number; items: Row[] };
};

const EMPTY: State = {
  primary: [],
  secondary: [],
  tertiary: [],
  notifications: { unread: 0, items: [] },
};

async function safe<T>(path: string, fallback: T): Promise<T> {
  try {
    return await apiRequest<T>(path);
  } catch {
    return fallback;
  }
}

function list(payload: unknown): Row[] {
  return unwrapItems<Row>(payload as Row[] | { items?: Row[] });
}

function number(value: unknown): number {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function title(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim() ? value : fallback;
}

const ROLE_COPY: Record<PortalRole, { kicker: string; intro: string }> = {
  ADMIN: {
    kicker: "Vận hành hệ thống",
    intro:
      "Quản trị tài khoản, cơ cấu tổ chức, báo cáo và cấu hình nền tảng. Chức năng giảng dạy được tách riêng cho tài khoản giảng viên.",
  },
  INSTRUCTOR: {
    kicker: "Không gian giảng dạy",
    intro:
      "Biên soạn khóa học, tạo bài kiểm tra từ tài liệu, quản lý kỳ thi độc lập và chấm bài trong một không gian riêng.",
  },
  STUDENT: {
    kicker: "Không gian học tập",
    intro:
      "Tiếp tục khóa học, làm bài kiểm tra ngay trong bài học và tham gia các kỳ thi được giao.",
  },
};

export function RoleDashboard({ user }: { user: PortalUser }) {
  const role = resolvePortalRole(user);
  const [state, setState] = useState<State>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setWarning("");
    const notificationRequest = safe<{ unread: number; items: Row[] }>(
      "/api/v1/notifications",
      { unread: 0, items: [] },
    );
    if (role === "ADMIN") {
      const [users, units, reports, notifications] = await Promise.all([
        safe<unknown>("/api/v1/users?size=100", []),
        safe<unknown>("/api/v1/organization/units", []),
        safe<unknown>("/api/v1/reports/learning", []),
        notificationRequest,
      ]);
      setState({
        primary: list(users),
        secondary: list(units),
        tertiary: list(reports),
        notifications,
      });
    } else if (role === "INSTRUCTOR") {
      const [courses, exams, grades, notifications] = await Promise.all([
        safe<unknown>("/api/v1/courses?size=100", []),
        safe<unknown>("/api/v1/exams?scope=standalone", []),
        safe<unknown>("/api/v1/grades/queue", []),
        notificationRequest,
      ]);
      setState({
        primary: list(courses),
        secondary: list(exams),
        tertiary: list(grades),
        notifications,
      });
    } else {
      const [learning, exams, grades, notifications] = await Promise.all([
        safe<unknown>("/api/v1/learning/me", []),
        safe<unknown>("/api/v1/exams?scope=standalone", []),
        safe<unknown>("/api/v1/grades/me", []),
        notificationRequest,
      ]);
      setState({
        primary: list(learning),
        secondary: list(exams),
        tertiary: list(grades),
        notifications,
      });
    }
    setLoading(false);
  }, [role]);

  useEffect(() => {
    void load();
  }, [load]);

  const metrics = useMemo(() => {
    if (role === "ADMIN")
      return [
        {
          icon: "users" as IconName,
          value: state.primary.length,
          label: "Tài khoản",
          detail: "Ba vai trò tách biệt",
          tone: "primary",
        },
        {
          icon: "building" as IconName,
          value: state.secondary.length,
          label: "Đơn vị",
          detail: "Cơ cấu đang quản lý",
          tone: "violet",
        },
        {
          icon: "report" as IconName,
          value: state.tertiary.length,
          label: "Dòng báo cáo",
          detail: "Dữ liệu tổng hợp",
          tone: "teal",
        },
        {
          icon: "bell" as IconName,
          value: state.notifications.unread,
          label: "Thông báo",
          detail: "Chưa đọc",
          tone: "warning",
        },
      ];
    if (role === "INSTRUCTOR")
      return [
        {
          icon: "book" as IconName,
          value: state.primary.length,
          label: "Khóa học",
          detail: `${state.primary.filter((row) => row.status === "PUBLISHED").length} đã xuất bản`,
          tone: "primary",
        },
        {
          icon: "exam" as IconName,
          value: state.secondary.length,
          label: "Bài thi độc lập",
          detail: "Không thuộc khóa học",
          tone: "violet",
        },
        {
          icon: "grade" as IconName,
          value: state.tertiary.length,
          label: "Bài chờ chấm",
          detail: "Tự luận và thực hành",
          tone: "warning",
        },
        {
          icon: "bell" as IconName,
          value: state.notifications.unread,
          label: "Thông báo",
          detail: "Cập nhật giảng dạy",
          tone: "teal",
        },
      ];
    const average = state.primary.length
      ? state.primary.reduce(
          (sum, row) => sum + number(row.progressPercent),
          0,
        ) / state.primary.length
      : 0;
    return [
      {
        icon: "book" as IconName,
        value: state.primary.length,
        label: "Khóa học",
        detail: "Đang được giao",
        tone: "primary",
      },
      {
        icon: "learn" as IconName,
        value: `${Math.round(average)}%`,
        label: "Tiến độ",
        detail: "Trung bình",
        tone: "success",
      },
      {
        icon: "exam" as IconName,
        value: state.secondary.length,
        label: "Bài thi",
        detail: "Kỳ thi độc lập",
        tone: "violet",
      },
      {
        icon: "grade" as IconName,
        value: state.tertiary.length,
        label: "Kết quả",
        detail: "Điểm đã ghi nhận",
        tone: "teal",
      },
    ];
  }, [role, state]);

  const actions =
    role === "ADMIN"
      ? [
          {
            href: PORTAL_PATHS.ADMIN.users,
            icon: "users" as IconName,
            label: "Quản lý người dùng",
            hint: "Tạo tài khoản với một vai trò duy nhất",
          },
          {
            href: PORTAL_PATHS.ADMIN.organization,
            icon: "building" as IconName,
            label: "Cơ cấu tổ chức",
            hint: "Đơn vị và thành viên",
          },
          {
            href: PORTAL_PATHS.ADMIN.settings,
            icon: "settings" as IconName,
            label: "Cấu hình hệ thống",
            hint: "Logo, nền đăng nhập và dịch vụ",
          },
        ]
      : role === "INSTRUCTOR"
        ? [
            {
              href: PORTAL_PATHS.INSTRUCTOR.courses,
              icon: "book" as IconName,
              label: "Mở khóa học",
              hint: "Biên soạn bài học và bài kiểm tra",
            },
            {
              href: PORTAL_PATHS.INSTRUCTOR.exams,
              icon: "exam" as IconName,
              label: "Quản lý bài thi",
              hint: "Chỉ kỳ thi độc lập",
            },
            {
              href: PORTAL_PATHS.INSTRUCTOR.grading,
              icon: "grade" as IconName,
              label: "Chấm bài",
              hint: "Bài thực hành và tự luận",
            },
          ]
        : [
            {
              href: PORTAL_PATHS.STUDENT.courses,
              icon: "play" as IconName,
              label: "Tiếp tục học",
              hint: "Video, DOCX, PDF và bài thực hành",
            },
            {
              href: PORTAL_PATHS.STUDENT.exams,
              icon: "exam" as IconName,
              label: "Vào bài thi",
              hint: "Kỳ thi độc lập được giao",
            },
            {
              href: PORTAL_PATHS.STUDENT.results,
              icon: "report" as IconName,
              label: "Xem kết quả",
              hint: "Điểm và phản hồi",
            },
          ];

  return (
    <div className="dashboard-page role-dashboard">
      <section className="dashboard-welcome">
        <div className="dashboard-welcome-copy">
          <span className="dashboard-kicker">{ROLE_COPY[role].kicker}</span>
          <h1>Xin chào, {user.fullName}</h1>
          <p>{ROLE_COPY[role].intro}</p>
          <div className="dashboard-welcome-actions">
            <Link href={actions[0].href} className="button primary">
              <Icon name={actions[0].icon} />
              {actions[0].label}
            </Link>
            <Link href={actions[1].href} className="button secondary">
              <Icon name={actions[1].icon} />
              {actions[1].label}
            </Link>
          </div>
        </div>
        <div className="dashboard-visual" aria-hidden="true">
          <div className="dashboard-progress-card">
            <span>Vai trò hiện tại</span>
            <strong className="role-dashboard-label">{roleLabel(role)}</strong>
            <div>
              <i style={{ width: "100%" }} />
            </div>
          </div>
          <div className="dashboard-mini-card card-a">
            <Icon name={metrics[0].icon} />
            <span>
              <b>{loading ? "…" : metrics[0].value}</b>
              <small>{metrics[0].label}</small>
            </span>
          </div>
          <div className="dashboard-mini-card card-b">
            <Icon name={metrics[1].icon} />
            <span>
              <b>{loading ? "…" : metrics[1].value}</b>
              <small>{metrics[1].label}</small>
            </span>
          </div>
          <span className="dashboard-orb orb-a" />
          <span className="dashboard-orb orb-b" />
        </div>
      </section>

      {warning && (
        <div className="dashboard-warning" role="status">
          <Icon name="warning" />
          <span>{warning}</span>
          <button type="button" onClick={() => void load()}>
            Tải lại
          </button>
        </div>
      )}

      <section className="metric-grid" aria-label="Chỉ số tổng quan">
        {metrics.map((item) => (
          <article className={`metric-card tone-${item.tone}`} key={item.label}>
            <span className="metric-icon">
              <Icon name={item.icon} />
            </span>
            <div>
              <strong>{loading ? "…" : item.value}</strong>
              <span>{item.label}</span>
              <small>{item.detail}</small>
            </div>
          </article>
        ))}
      </section>

      <section className="dashboard-layout">
        <div className="dashboard-column-main">
          <article className="dashboard-panel progress-overview-panel">
            <header className="panel-heading">
              <div>
                <span className="panel-kicker">Dữ liệu gần đây</span>
                <h2>
                  {role === "ADMIN"
                    ? "Tài khoản mới"
                    : role === "INSTRUCTOR"
                      ? "Khóa học của bạn"
                      : "Tiến độ khóa học"}
                </h2>
              </div>
              <Link href={actions[0].href}>
                Xem tất cả <Icon name="arrow" size={15} />
              </Link>
            </header>
            {state.primary.length ? (
              <div className="progress-list">
                {state.primary.slice(0, 6).map((row, index) => {
                  const value =
                    role === "STUDENT"
                      ? Math.max(0, Math.min(100, number(row.progressPercent)))
                      : 100;
                  return (
                    <div
                      className="progress-list-item"
                      key={String(row.id ?? row.enrollmentId ?? index)}
                    >
                      <span className="progress-course-icon">
                        <Icon
                          name={role === "ADMIN" ? "users" : "book"}
                          size={18}
                        />
                      </span>
                      <div>
                        <div className="progress-list-head">
                          <strong>
                            {title(
                              row.fullName ?? row.name ?? row.courseName,
                              `Mục ${index + 1}`,
                            )}
                          </strong>
                          <span>
                            {role === "STUDENT"
                              ? `${Math.round(value)}%`
                              : title(row.status, "Đang hoạt động")}
                          </span>
                        </div>
                        <ProgressBar value={value} label="Tiến độ" />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="dashboard-empty">
                <Icon name="book" />
                <p>Dữ liệu sẽ xuất hiện khi hệ thống được sử dụng.</p>
              </div>
            )}
          </article>
        </div>
        <aside className="dashboard-column-side">
          <article className="dashboard-panel quick-actions-panel">
            <header className="panel-heading">
              <div>
                <span className="panel-kicker">Truy cập nhanh</span>
                <h2>
                  Chức năng của {roleLabel(role).toLocaleLowerCase("vi-VN")}
                </h2>
              </div>
            </header>
            <div className="quick-action-list">
              {actions.map((action) => (
                <Link href={action.href} key={action.href}>
                  <span className="quick-action-icon">
                    <Icon name={action.icon} />
                  </span>
                  <span>
                    <strong>{action.label}</strong>
                    <small>{action.hint}</small>
                  </span>
                  <Icon name="arrow" size={16} />
                </Link>
              ))}
            </div>
          </article>
        </aside>
      </section>
    </div>
  );
}
