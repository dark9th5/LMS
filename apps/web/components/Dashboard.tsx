"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { IconName, PortalUser } from "@/lib/types";
import { Icon } from "./Icon";

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

const emptyState: DashboardState = {
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

function number(value: unknown): number {
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
    user.accountType === "SYSTEM_ADMIN" ||
    user.permissions.includes("users:read");
  const isStudent = !user.permissions.some((permission) =>
    [
      "reports:read:scope",
      "courses:create",
      "classes:manage",
      "grading:manage",
      "assessments:grade",
    ].includes(permission),
  );
  const [state, setState] = useState<DashboardState>(emptyState);
  const [loading, setLoading] = useState(true);
  const [warning, setWarning] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setWarning("");
    const coursesPayload = await safeApi<unknown>(
      "/api/v1/courses?size=100",
      [],
    );
    const courses = unwrapItems<UnknownRecord>(
      coursesPayload as UnknownRecord[] | { items?: UnknownRecord[] },
    );
    const notifications = await safeApi<NotificationSummary>(
      "/api/v1/notifications",
      { unread: 0, items: [] },
    );

    if (isStudent) {
      const [rowsRaw, examsRaw, gradesRaw, certificatesRaw] = await Promise.all(
        [
          safeApi<unknown>("/api/v1/learning/me", []),
          safeApi<unknown>("/api/v1/exams", []),
          safeApi<unknown>("/api/v1/grades/me", []),
          safeApi<unknown>("/api/v1/certificates/me", []),
        ],
      );
      const rows = unwrapItems<UnknownRecord>(rowsRaw as any);
      const exams = unwrapItems<UnknownRecord>(examsRaw as any);
      const grades = unwrapItems<UnknownRecord>(gradesRaw as any);
      const certificates = unwrapItems<UnknownRecord>(certificatesRaw as any);
      setState({
        ...emptyState,
        courses,
        rows,
        exams,
        grades,
        certificates,
        notifications,
      });
      if (courses.length === 0 && rows.length === 0)
        setWarning(
          "Chưa có khóa học được ghi danh hoặc hệ thống đang khởi tạo dữ liệu.",
        );
    } else {
      const [report, rowsRaw, classesRaw, gradesRaw, usersRaw] =
        await Promise.all([
          safeApi<ReportingDashboard | null>("/api/v1/reports/dashboard", null),
          safeApi<unknown>("/api/v1/reports/learning", []),
          safeApi<unknown>("/api/v1/classes", []),
          safeApi<unknown>("/api/v1/grades/queue", []),
          isAdmin
            ? safeApi<unknown>("/api/v1/users?size=100", [])
            : Promise.resolve([]),
        ]);
      const rows = unwrapItems<UnknownRecord>(rowsRaw as any);
      const classes = unwrapItems<UnknownRecord>(classesRaw as any);
      const grades = unwrapItems<UnknownRecord>(gradesRaw as any);
      const users = unwrapItems<UnknownRecord>(usersRaw as any);
      setState({
        ...emptyState,
        courses,
        report,
        rows,
        classes,
        grades,
        users,
        notifications,
      });
      if (!report)
        setWarning(
          "Dữ liệu báo cáo chưa sẵn sàng; các chỉ số nghiệp vụ khác vẫn được hiển thị đầy đủ.",
        );
    }
    setLoading(false);
  }, [isAdmin, isStudent]);

  useEffect(() => {
    void load();
  }, [load]);

  const metrics = useMemo(() => {
    const coursesList = Array.isArray(state.courses) ? state.courses : [];
    const classesList = Array.isArray(state.classes) ? state.classes : [];
    const gradesList = Array.isArray(state.grades) ? state.grades : [];
    const rowsList = Array.isArray(state.rows) ? state.rows : [];

    const published = coursesList.filter(
      (course) => course.status === "PUBLISHED",
    ).length;
    const drafts = coursesList.filter(
      (course) => course.status === "DRAFT",
    ).length;
    const activeClasses = classesList.filter(
      (item) => item.status === "OPEN",
    ).length;
    const pendingGrades = gradesList.filter(
      (item) => item.status === "PENDING_MANUAL",
    ).length;
    const isCompleted = (row: UnknownRecord) =>
      row.completed === true || row.status === "COMPLETED";
    const isOverdue = (row: UnknownRecord) =>
      !isCompleted(row) &&
      (row.status === "OVERDUE" ||
        Boolean(
          row.dueAt && new Date(String(row.dueAt)).getTime() < Date.now(),
        ));
    const completedRows = rowsList.filter(isCompleted).length;
    const overdueRows = rowsList.filter(isOverdue).length;
    const inProgressRows = rowsList.filter(
      (row) =>
        !isCompleted(row) && !isOverdue(row) && number(row.progressPercent) > 0,
    ).length;
    const average =
      state.report?.averageProgress ??
      (rowsList.length
        ? rowsList.reduce((sum, row) => sum + number(row.progressPercent), 0) /
          rowsList.length
        : 0);
    return {
      published,
      drafts,
      activeClasses,
      pendingGrades,
      completedRows,
      overdueRows,
      inProgressRows,
      average,
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
  const progressRows = state.rows.slice(0, 6);
  const totalRows = state.report?.enrolled ?? state.rows.length;
  const completed = state.report?.completed ?? metrics.completedRows;
  const overdue = state.report?.overdue ?? metrics.overdueRows;
  const inProgress = Math.max(
    0,
    state.report?.inProgress ?? metrics.inProgressRows,
  );
  const notStarted = Math.max(0, totalRows - completed - overdue - inProgress);

  const kpis = isStudent
    ? [
        {
          icon: "book" as IconName,
          value: String(state.rows.length),
          label: "Khóa học được giao",
          delta: `${metrics.completedRows} đã hoàn thành`,
        },
        {
          icon: "learn" as IconName,
          value: `${Math.round(metrics.average)}%`,
          label: "Tiến độ trung bình",
          delta: metrics.overdueRows
            ? `${metrics.overdueRows} khóa quá hạn`
            : "Đúng tiến độ",
          warning: metrics.overdueRows > 0,
        },
        {
          icon: "exam" as IconName,
          value: String(state.exams.length),
          label: "Bài kiểm tra đang mở",
          delta: `${state.grades.length} kết quả đã có`,
        },
        {
          icon: "certificate" as IconName,
          value: String(state.certificates.length),
          label: "Chứng chỉ đã nhận",
          delta: `${state.notifications.unread} thông báo mới`,
        },
      ]
    : [
        {
          icon: "users" as IconName,
          value: String(
            isAdmin
              ? state.users.length
              : (state.report?.enrolled ?? state.rows.length),
          ),
          label: isAdmin ? "Tài khoản hệ thống" : "Học viên trong phạm vi",
          delta: `${state.report?.enrolled ?? state.rows.length} lượt ghi danh`,
        },
        {
          icon: "book" as IconName,
          value: String(metrics.published),
          label: "Khóa học đã xuất bản",
          delta: `${metrics.drafts} bản nháp`,
        },
        {
          icon: "class" as IconName,
          value: String(metrics.activeClasses),
          label: "Lớp học đang mở",
          delta: `${state.classes.length} lớp trong phạm vi`,
        },
        {
          icon: "grade" as IconName,
          value: String(metrics.pendingGrades),
          label: "Bài chờ chấm điểm",
          delta: overdue ? `${overdue} lượt học quá hạn` : "Không có quá hạn",
          warning: metrics.pendingGrades > 0 || overdue > 0,
        },
      ];

  const primaryActions = isStudent
    ? [
        { href: "/learning", label: "Học tập của tôi", icon: "learn" as IconName },
        { href: "/exams", label: "Bài kiểm tra", icon: "exam" as IconName },
      ]
    : [
        { href: "/courses", label: "Quản lý khóa học", icon: "book" as IconName },
        { href: "/grading", label: "Chấm điểm bài thi", icon: "grade" as IconName },
      ];

  return (
    <div className="unified-dashboard">
      {/* Header Section */}
      <header className="unified-dashboard-header">
        <div className="unified-dashboard-header-text">
          <h1>Tổng quan</h1>
          <h2>Xin chào, {user.fullName}</h2>
          <p>
            {isStudent
              ? "Theo dõi tiến độ học tập, tham gia bài kiểm tra và cập nhật thông báo mới nhất."
              : "Quản lý hoạt động đào tạo, theo dõi các chỉ số vận hành và xử lý công việc ưu tiên."}
          </p>
        </div>
        <div className="unified-dashboard-header-actions">
          {primaryActions.slice(0, 2).map((action, index) => (
            <Link
              key={action.href}
              href={action.href}
              className={
                index === 0
                  ? "unified-dashboard-btn unified-dashboard-btn-primary"
                  : "unified-dashboard-btn unified-dashboard-btn-secondary"
              }
            >
              <Icon name={action.icon} size={18} />
              <span>{action.label}</span>
            </Link>
          ))}
        </div>
      </header>

      {/* Warning Notice if read model is initializing */}
      {warning && (
        <div className="unified-dashboard-warning">
          <span>{warning}</span>
          <button type="button" onClick={() => void load()}>
            Tải lại
          </button>
        </div>
      )}

      {/* KPI Cards Grid */}
      <section className="unified-dashboard-kpis">
        {kpis.map((kpi) => (
          <div
            key={kpi.label}
            className={`unified-dashboard-kpi-card ${kpi.warning ? "is-warning" : ""}`}
          >
            <div className="unified-dashboard-kpi-header">
              <span className="unified-dashboard-kpi-icon">
                <Icon name={kpi.icon} size={20} />
              </span>
              <span className="unified-dashboard-kpi-value">
                {loading ? "…" : kpi.value}
              </span>
            </div>
            <div className="unified-dashboard-kpi-body">
              <span className="unified-dashboard-kpi-label">{kpi.label}</span>
              <span className="unified-dashboard-kpi-delta">
                {loading ? "Đang cập nhật…" : kpi.delta}
              </span>
            </div>
          </div>
        ))}
      </section>

      {/* Grid of Main Dashboard Panels */}
      <section className="unified-dashboard-grid">
        {/* Panel 1: Course Progress */}
        <article className="unified-dashboard-card">
          <div className="unified-dashboard-card-header">
            <h3>Tiến độ khóa học gần đây</h3>
            <p>
              {progressRows.length
                ? "Tiến độ học tập ghi nhận từ hệ thống"
                : "Chưa có tiến độ học tập nào"}
            </p>
          </div>
          <div className="unified-dashboard-card-body">
            {progressRows.length ? (
              <div className="unified-dashboard-progress-list">
                {progressRows.map((row, index) => {
                  const pct = Math.max(
                    0,
                    Math.min(100, number(row.progressPercent)),
                  );
                  const courseName =
                    courseNames.get(String(row.courseId)) ??
                    `Khóa học ${index + 1}`;
                  return (
                    <div
                      key={String(row.enrollmentId ?? row.id ?? index)}
                      className="unified-dashboard-progress-item"
                    >
                      <div className="unified-dashboard-progress-info">
                        <span className="unified-dashboard-progress-name">
                          {courseName}
                        </span>
                        <span className="unified-dashboard-progress-pct">
                          {Math.round(pct)}%
                        </span>
                      </div>
                      <div className="unified-dashboard-progress-bar">
                        <div
                          className="unified-dashboard-progress-fill"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="unified-dashboard-empty">
                Chưa có dữ liệu tiến độ.
              </p>
            )}
          </div>
        </article>

        {/* Panel 2: Training Status Breakdown */}
        <article className="unified-dashboard-card">
          <div className="unified-dashboard-card-header">
            <h3>Trạng thái đào tạo</h3>
            <p>Thống kê lượt học trong phạm vi</p>
          </div>
          <div className="unified-dashboard-card-body">
            <div className="unified-dashboard-stat-summary">
              <div className="unified-dashboard-stat-row">
                <span className="unified-dashboard-stat-label">Tổng số lượt ghi danh</span>
                <span className="unified-dashboard-stat-val">{totalRows}</span>
              </div>
              <div className="unified-dashboard-stat-row">
                <span className="unified-dashboard-stat-label">Đã hoàn thành</span>
                <span className="unified-dashboard-stat-val is-success">{completed}</span>
              </div>
              <div className="unified-dashboard-stat-row">
                <span className="unified-dashboard-stat-label">Đang học</span>
                <span className="unified-dashboard-stat-val is-primary">{inProgress}</span>
              </div>
              <div className="unified-dashboard-stat-row">
                <span className="unified-dashboard-stat-label">Chưa bắt đầu</span>
                <span className="unified-dashboard-stat-val">{notStarted}</span>
              </div>
              <div className="unified-dashboard-stat-row">
                <span className="unified-dashboard-stat-label">Quá hạn</span>
                <span className="unified-dashboard-stat-val is-danger">{overdue}</span>
              </div>
            </div>
          </div>
        </article>

        {/* Panel 3: Notifications */}
        <article className="unified-dashboard-card">
          <div className="unified-dashboard-card-header">
            <h3>Thông báo gần đây</h3>
            <p>{state.notifications.unread} thông báo chưa đọc</p>
          </div>
          <div className="unified-dashboard-card-body">
            {state.notifications.items.length ? (
              <div className="unified-dashboard-notification-list">
                {state.notifications.items.slice(0, 5).map((item) => (
                  <div key={item.id} className="unified-dashboard-notification-item">
                    <div className="unified-dashboard-notification-icon">
                      <Icon
                        name={
                          item.title.includes("Chứng chỉ")
                            ? "certificate"
                            : item.title.includes("kiểm tra")
                              ? "exam"
                              : "bell"
                        }
                        size={16}
                      />
                    </div>
                    <div className="unified-dashboard-notification-content">
                      <div className="unified-dashboard-notification-title">
                        {item.title}
                      </div>
                      <div className="unified-dashboard-notification-body">
                        {item.body}
                      </div>
                      <div className="unified-dashboard-notification-time">
                        {relativeTime(item.createdAt)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="unified-dashboard-empty">
                Không có thông báo mới.
              </p>
            )}
          </div>
        </article>

        {/* Panel 4: Pending Tasks / Items requiring action */}
        <article className="unified-dashboard-card">
          <div className="unified-dashboard-card-header">
            <h3>Nhiệm vụ cần xử lý</h3>
            <p>Các công việc và phản hồi ưu tiên</p>
          </div>
          <div className="unified-dashboard-card-body">
            <div className="unified-dashboard-task-list">
              <div className="unified-dashboard-task-item">
                <div className="unified-dashboard-task-info">
                  <strong>{overdue}</strong>
                  <span>{isStudent ? "Khóa học quá hạn" : "Lượt học quá hạn"}</span>
                </div>
                <Link
                  href={isStudent ? "/learning" : "/reports"}
                  className="unified-dashboard-link"
                >
                  Xem danh sách
                </Link>
              </div>

              <div className="unified-dashboard-task-item">
                <div className="unified-dashboard-task-info">
                  <strong>{metrics.pendingGrades}</strong>
                  <span>Bài thi tự luận chờ chấm</span>
                </div>
                {isStudent ? (
                  <span className="unified-dashboard-tag">Theo dõi kết quả</span>
                ) : (
                  <Link href="/grading" className="unified-dashboard-link">
                    Mở hàng chờ
                  </Link>
                )}
              </div>

              <div className="unified-dashboard-task-item">
                <div className="unified-dashboard-task-info">
                  <strong>{state.notifications.unread}</strong>
                  <span>Thông báo chưa đọc</span>
                </div>
                <span className="unified-dashboard-tag">Xem ở thanh công cụ</span>
              </div>
            </div>
          </div>
        </article>
      </section>
    </div>
  );
}

