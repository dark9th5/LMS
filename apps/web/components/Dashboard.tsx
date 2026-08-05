"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { IconName, PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { ProgressBar } from "./ProgressBar";

type UnknownRecord = Record<string, unknown>;
type NotificationItem = {
  id: string;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
};
type NotificationSummary = { unread: number; items: NotificationItem[] };
type ReportingDashboard = {
  enrolled: number;
  inProgress: number;
  completed: number;
  overdue: number;
  averageProgress: number;
  lastSynchronizedAt: string;
};
type DashboardState = {
  courses: UnknownRecord[];
  rows: UnknownRecord[];
  classes: UnknownRecord[];
  exams: UnknownRecord[];
  grades: UnknownRecord[];
  certificates: UnknownRecord[];
  users: UnknownRecord[];
  notifications: NotificationSummary;
  report: ReportingDashboard | null;
};

const EMPTY_STATE: DashboardState = {
  courses: [],
  rows: [],
  classes: [],
  exams: [],
  grades: [],
  certificates: [],
  users: [],
  notifications: { unread: 0, items: [] },
  report: null,
};

function asNumber(value: unknown): number {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function relativeTime(value: string): string {
  const timestamp = new Date(value).getTime();
  if (!Number.isFinite(timestamp)) return "";
  const minutes = Math.max(0, Math.round((Date.now() - timestamp) / 60000));
  if (minutes < 1) return "Vừa xong";
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.round(hours / 24)} ngày trước`;
}

async function safeApi<T>(path: string, fallback: T): Promise<T> {
  try {
    return await apiRequest<T>(path);
  } catch {
    return fallback;
  }
}

export function Dashboard({ user }: { user: PortalUser }) {
  const isAdmin =
    user.accountType === "SYSTEM_ADMIN" || user.permissions.includes("users:read");
  const isStudent = !user.permissions.some((permission) =>
    [
      "reports:read:scope",
      "courses:create",
      "classes:manage",
      "grading:manage",
      "assessments:grade",
    ].includes(permission),
  );
  const [state, setState] = useState<DashboardState>(EMPTY_STATE);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setWarning("");
    const coursesPayload = await safeApi<unknown>("/api/v1/courses?size=100", []);
    const courses = unwrapItems<UnknownRecord>(
      coursesPayload as UnknownRecord[] | { items?: UnknownRecord[] },
    );
    const notifications = await safeApi<NotificationSummary>(
      "/api/v1/notifications",
      { unread: 0, items: [] },
    );

    if (isStudent) {
      const [rowsRaw, examsRaw, gradesRaw, certificatesRaw] = await Promise.all([
        safeApi<unknown>("/api/v1/learning/me", []),
        safeApi<unknown>("/api/v1/exams", []),
        safeApi<unknown>("/api/v1/grades/me", []),
        safeApi<unknown>("/api/v1/certificates/me", []),
      ]);
      const rows = unwrapItems<UnknownRecord>(rowsRaw as any);
      setState({
        ...EMPTY_STATE,
        courses,
        rows,
        exams: unwrapItems<UnknownRecord>(examsRaw as any),
        grades: unwrapItems<UnknownRecord>(gradesRaw as any),
        certificates: unwrapItems<UnknownRecord>(certificatesRaw as any),
        notifications,
      });
      if (!courses.length && !rows.length) {
        setWarning("Chưa có khóa học được giao hoặc dịch vụ dữ liệu đang khởi động.");
      }
    } else {
      const [report, rowsRaw, classesRaw, gradesRaw, usersRaw] = await Promise.all([
        safeApi<ReportingDashboard | null>("/api/v1/reports/dashboard", null),
        safeApi<unknown>("/api/v1/reports/learning", []),
        safeApi<unknown>("/api/v1/classes", []),
        safeApi<unknown>("/api/v1/grades/queue", []),
        isAdmin ? safeApi<unknown>("/api/v1/users?size=100", []) : Promise.resolve([]),
      ]);
      setState({
        ...EMPTY_STATE,
        courses,
        report,
        rows: unwrapItems<UnknownRecord>(rowsRaw as any),
        classes: unwrapItems<UnknownRecord>(classesRaw as any),
        grades: unwrapItems<UnknownRecord>(gradesRaw as any),
        users: unwrapItems<UnknownRecord>(usersRaw as any),
        notifications,
      });
      if (!report) {
        setWarning("Báo cáo tổng hợp chưa sẵn sàng; dữ liệu từ các khu vực khác vẫn được hiển thị.");
      }
    }
    setLoading(false);
  }, [isAdmin, isStudent]);

  useEffect(() => {
    void load();
  }, [load]);

  const metrics = useMemo(() => {
    const published = state.courses.filter((course) => course.status === "PUBLISHED").length;
    const drafts = state.courses.filter((course) => course.status === "DRAFT").length;
    const activeClasses = state.classes.filter((item) => item.status === "OPEN").length;
    const pendingGrades = state.grades.filter((item) => item.status === "PENDING_MANUAL").length;
    const completedRows = state.rows.filter(
      (row) => row.completed === true || row.status === "COMPLETED",
    ).length;
    const overdueRows = state.rows.filter((row) => {
      if (row.completed === true || row.status === "COMPLETED") return false;
      if (row.status === "OVERDUE") return true;
      return Boolean(row.dueAt && new Date(String(row.dueAt)).getTime() < Date.now());
    }).length;
    const inProgressRows = state.rows.filter(
      (row) =>
        row.status !== "COMPLETED" &&
        row.status !== "OVERDUE" &&
        asNumber(row.progressPercent) > 0,
    ).length;
    const average =
      state.report?.averageProgress ??
      (state.rows.length
        ? state.rows.reduce((sum, row) => sum + asNumber(row.progressPercent), 0) /
          state.rows.length
        : 0);
    return {
      published,
      drafts,
      activeClasses,
      pendingGrades,
      completedRows,
      overdueRows,
      inProgressRows,
      average: Math.max(0, Math.min(100, average)),
    };
  }, [state]);

  const courseNames = useMemo(
    () =>
      new Map(
        state.courses.map((course) => [
          String(course.id),
          String(course.name ?? course.code ?? "Khóa học"),
        ]),
      ),
    [state.courses],
  );

  const totalRows = state.report?.enrolled ?? state.rows.length;
  const completed = state.report?.completed ?? metrics.completedRows;
  const overdue = state.report?.overdue ?? metrics.overdueRows;
  const inProgress = state.report?.inProgress ?? metrics.inProgressRows;

  const kpis = isStudent
    ? [
        {
          icon: "book" as IconName,
          value: state.rows.length,
          label: "Khóa học được giao",
          detail: `${metrics.completedRows} khóa đã hoàn thành`,
          tone: "primary",
        },
        {
          icon: "learn" as IconName,
          value: `${Math.round(metrics.average)}%`,
          label: "Tiến độ trung bình",
          detail: metrics.overdueRows ? `${metrics.overdueRows} khóa quá hạn` : "Đang đúng tiến độ",
          tone: metrics.overdueRows ? "warning" : "success",
        },
        {
          icon: "exam" as IconName,
          value: state.exams.length,
          label: "Bài thi đang mở",
          detail: `${state.grades.length} kết quả đã có`,
          tone: "violet",
        },
        {
          icon: "certificate" as IconName,
          value: state.certificates.length,
          label: "Chứng chỉ",
          detail: `${state.notifications.unread} thông báo mới`,
          tone: "teal",
        },
      ]
    : [
        {
          icon: "users" as IconName,
          value: isAdmin ? state.users.length : totalRows,
          label: isAdmin ? "Người dùng" : "Học viên trong phạm vi",
          detail: `${totalRows} lượt ghi danh`,
          tone: "primary",
        },
        {
          icon: "book" as IconName,
          value: metrics.published,
          label: "Khóa học đã xuất bản",
          detail: `${metrics.drafts} bản nháp`,
          tone: "success",
        },
        {
          icon: "class" as IconName,
          value: metrics.activeClasses,
          label: "Lớp đang mở",
          detail: `${state.classes.length} lớp trong phạm vi`,
          tone: "violet",
        },
        {
          icon: "grade" as IconName,
          value: metrics.pendingGrades,
          label: "Bài chờ chấm",
          detail: overdue ? `${overdue} lượt học quá hạn` : "Không có lượt quá hạn",
          tone: metrics.pendingGrades || overdue ? "warning" : "teal",
        },
      ];

  const quickActions = isStudent
    ? [
        { href: "/learning", icon: "play" as IconName, label: "Tiếp tục học", hint: "Mở khóa học gần nhất" },
        { href: "/exams", icon: "exam" as IconName, label: "Làm bài thi", hint: "Xem bài đang mở" },
        { href: "/results", icon: "grade" as IconName, label: "Xem kết quả", hint: "Điểm số và phản hồi" },
      ]
    : [
        { href: "/courses", icon: "plus" as IconName, label: "Tạo khóa học", hint: "Xây dựng nội dung mới" },
        { href: "/classes", icon: "class" as IconName, label: "Quản lý lớp", hint: "Lịch và học viên" },
        { href: "/grading", icon: "grade" as IconName, label: "Chấm điểm", hint: "Xử lý bài tự luận" },
      ];

  const progressRows = state.rows.slice(0, 5);
  const attentionItems = [
    {
      label: isStudent ? "Khóa học quá hạn" : "Lượt học quá hạn",
      value: overdue,
      href: isStudent ? "/learning" : "/reports",
      tone: overdue ? "danger" : "neutral",
    },
    {
      label: "Bài tự luận chờ chấm",
      value: metrics.pendingGrades,
      href: isStudent ? "/results" : "/grading",
      tone: metrics.pendingGrades ? "warning" : "neutral",
    },
    {
      label: "Thông báo chưa đọc",
      value: state.notifications.unread,
      href: "/dashboard",
      tone: state.notifications.unread ? "primary" : "neutral",
    },
  ];

  return (
    <div className="dashboard-page">
      <section className="dashboard-welcome">
        <div className="dashboard-welcome-copy">
          <span className="dashboard-kicker">Tổng quan hôm nay</span>
          <h1>Xin chào, {user.fullName}</h1>
          <p>
            {isStudent
              ? "Tiếp tục bài học, theo dõi tiến độ và hoàn thành các bài kiểm tra đang chờ bạn."
              : "Theo dõi hoạt động đào tạo và xử lý các công việc quan trọng trong phạm vi được cấp."}
          </p>
          <div className="dashboard-welcome-actions">
            {quickActions.slice(0, 2).map((action, index) => (
              <Link
                key={action.href}
                href={action.href}
                className={`button ${index === 0 ? "primary" : "secondary"}`}
              >
                <Icon name={action.icon} />
                {action.label}
              </Link>
            ))}
          </div>
        </div>
        <div className="dashboard-visual" aria-hidden="true">
          <div className="dashboard-progress-card">
            <span>Tiến độ trung bình</span>
            <strong>{Math.round(metrics.average)}%</strong>
            <div><i style={{ width: `${metrics.average}%` }} /></div>
          </div>
          <div className="dashboard-mini-card card-a">
            <Icon name="book" />
            <span><b>{metrics.published}</b><small>khóa đã xuất bản</small></span>
          </div>
          <div className="dashboard-mini-card card-b">
            <Icon name="check" />
            <span><b>{completed}</b><small>lượt hoàn thành</small></span>
          </div>
          <span className="dashboard-orb orb-a" />
          <span className="dashboard-orb orb-b" />
        </div>
      </section>

      {warning && (
        <div className="dashboard-warning" role="status">
          <Icon name="warning" />
          <span>{warning}</span>
          <button type="button" onClick={() => void load()}>Tải lại</button>
        </div>
      )}

      <section className="metric-grid" aria-label="Chỉ số tổng quan">
        {kpis.map((item) => (
          <article className={`metric-card tone-${item.tone}`} key={item.label}>
            <span className="metric-icon"><Icon name={item.icon} /></span>
            <div>
              <strong>{loading ? "…" : item.value}</strong>
              <span>{item.label}</span>
              <small>{loading ? "Đang cập nhật dữ liệu" : item.detail}</small>
            </div>
          </article>
        ))}
      </section>

      <section className="dashboard-layout">
        <div className="dashboard-column-main">
          <article className="dashboard-panel progress-overview-panel">
            <header className="panel-heading">
              <div>
                <span className="panel-kicker">Tiến độ học tập</span>
                <h2>{isStudent ? "Khóa học của bạn" : "Các lượt học gần đây"}</h2>
              </div>
              <Link href={isStudent ? "/learning" : "/reports"}>Xem tất cả <Icon name="arrow" size={15} /></Link>
            </header>
            {progressRows.length ? (
              <div className="progress-list">
                {progressRows.map((row, index) => {
                  const value = Math.max(0, Math.min(100, asNumber(row.progressPercent)));
                  const name = courseNames.get(String(row.courseId)) ?? `Khóa học ${index + 1}`;
                  return (
                    <div className="progress-list-item" key={String(row.enrollmentId ?? row.id ?? index)}>
                      <span className="progress-course-icon"><Icon name="book" size={18} /></span>
                      <div>
                        <div className="progress-list-head"><strong>{name}</strong><span>{Math.round(value)}%</span></div>
                        <ProgressBar value={value} label={`Tiến độ ${name}`} />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <EmptyDashboard text="Dữ liệu tiến độ sẽ xuất hiện khi có học viên được ghi danh." />
            )}
          </article>

          <article className="dashboard-panel attention-panel">
            <header className="panel-heading">
              <div>
                <span className="panel-kicker">Cần xử lý</span>
                <h2>Việc cần chú ý</h2>
              </div>
            </header>
            <div className="attention-list">
              {attentionItems.map((item) => (
                <Link href={item.href} key={item.label} className={`attention-item tone-${item.tone}`}>
                  <span>{item.value}</span>
                  <strong>{item.label}</strong>
                  <Icon name="arrow" size={16} />
                </Link>
              ))}
            </div>
          </article>
        </div>

        <aside className="dashboard-column-side">
          <article className="dashboard-panel quick-actions-panel">
            <header className="panel-heading">
              <div>
                <span className="panel-kicker">Truy cập nhanh</span>
                <h2>Công việc thường dùng</h2>
              </div>
            </header>
            <div className="quick-action-list">
              {quickActions.map((action) => (
                <Link href={action.href} key={action.href}>
                  <span className="quick-action-icon"><Icon name={action.icon} /></span>
                  <span><strong>{action.label}</strong><small>{action.hint}</small></span>
                  <Icon name="arrow" size={16} />
                </Link>
              ))}
            </div>
          </article>

          <article className="dashboard-panel notifications-panel">
            <header className="panel-heading">
              <div>
                <span className="panel-kicker">Cập nhật mới</span>
                <h2>Thông báo</h2>
              </div>
              <span className="panel-count">{state.notifications.unread} chưa đọc</span>
            </header>
            {state.notifications.items.length ? (
              <div className="dashboard-notification-list">
                {state.notifications.items.slice(0, 4).map((item) => (
                  <div key={item.id} className={item.read ? "" : "unread"}>
                    <span className="notification-avatar"><Icon name="bell" size={17} /></span>
                    <span><strong>{item.title}</strong><p>{item.body}</p><small>{relativeTime(item.createdAt)}</small></span>
                  </div>
                ))}
              </div>
            ) : (
              <EmptyDashboard text="Chưa có thông báo mới." />
            )}
          </article>
        </aside>
      </section>
    </div>
  );
}

function EmptyDashboard({ text }: { text: string }) {
  return (
    <div className="dashboard-empty">
      <Icon name="book" />
      <p>{text}</p>
    </div>
  );
}
