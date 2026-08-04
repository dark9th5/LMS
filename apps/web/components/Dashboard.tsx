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

function date(value: unknown): string {
  if (!value) return "Chưa có dữ liệu";
  const parsed = new Date(String(value));
  if (Number.isNaN(parsed.getTime())) return "Chưa có dữ liệu";
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(parsed);
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
          "Chưa có khóa học được ghi danh hoặc service dữ liệu đang khởi động.",
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
          "Read model báo cáo chưa sẵn sàng; các số liệu còn lại vẫn được hiển thị.",
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
  const completedPercent = totalRows
    ? Math.round((completed * 100) / totalRows)
    : 0;
  const inProgressPercent = totalRows
    ? Math.round((inProgress * 100) / totalRows)
    : 0;
  const overduePercent = totalRows
    ? Math.round((overdue * 100) / totalRows)
    : 0;
  const notStartedPercent = Math.max(
    0,
    100 - completedPercent - inProgressPercent - overduePercent,
  );
  const donutStyle = {
    background: `conic-gradient(#1d75df 0 ${completedPercent}%,#43b9e9 ${completedPercent}% ${completedPercent + inProgressPercent}%,#9bd9ee ${completedPercent + inProgressPercent}% ${100 - overduePercent}%,#f1a85d ${100 - overduePercent}% 100%)`,
  };

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
          label: "Chứng chỉ cá nhân",
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
          label: isAdmin ? "Tài khoản người dùng" : "Học viên trong phạm vi",
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
          label: "Lớp đang mở",
          delta: `${state.classes.length} lớp trong phạm vi`,
        },
        {
          icon: "grade" as IconName,
          value: String(metrics.pendingGrades),
          label: "Bài chờ chấm",
          delta: overdue ? `${overdue} lượt học quá hạn` : "Không có quá hạn",
          warning: metrics.pendingGrades > 0 || overdue > 0,
        },
      ];

  const quickActions = isStudent
    ? [
        {
          href: "/learning",
          icon: "learn" as IconName,
          label: "Tiếp tục học",
          hint: "Mở hành trình gần nhất",
        },
        {
          href: "/exams",
          icon: "exam" as IconName,
          label: "Kỳ thi",
          hint: "Xem thử thách đang mở",
        },
        {
          href: "/results",
          icon: "grade" as IconName,
          label: "Kết quả",
          hint: "Điểm số và phản hồi",
        },
      ]
    : [
        {
          href: "/courses",
          icon: "book" as IconName,
          label: "Kiến tạo khóa học",
          hint: "Quản lý nội dung đào tạo",
        },
        {
          href: "/classes",
          icon: "class" as IconName,
          label: "Điều phối lớp",
          hint: "Lịch, giảng viên, học viên",
        },
        {
          href: "/grading",
          icon: "grade" as IconName,
          label: "Mở điện chấm",
          hint: "Xử lý hàng chờ đánh giá",
        },
      ];

  return (
    <>
      <section className="welcome command-welcome">
        <div className="welcome-star-map" aria-hidden="true">
          <i />
          <i />
          <i />
          <i />
          <span>✦</span>
        </div>
        <div className="welcome-copy">
          <div className="welcome-coordinate">
            <span>REALM COMMAND / {isStudent ? "LEARNER" : "OPERATIONS"}</span>
            <b>LIVE</b>
          </div>
          <span className="eyebrow">TRUNG TÂM ĐIỀU HÀNH ĐÀO TẠO</span>
          <h1>
            Chào {user.fullName}
            <em>— mọi tín hiệu tri thức đã hội tụ.</em>
          </h1>
          <p>
            {isStudent
              ? "Tiếp tục khóa học được giao, theo dõi tiến độ và kết quả cá nhân trong một hành trình liền mạch."
              : "Quan sát toàn cảnh đào tạo, phát hiện điểm nghẽn và mở đúng công cụ trong phạm vi bạn được trao quyền."}
          </p>
          <div className="welcome-actions">
            {quickActions.slice(0, 2).map((item, index) => (
              <Link
                key={item.href}
                href={item.href}
                className={index === 0 ? "button primary" : "button secondary"}
              >
                <Icon name={item.icon} />
                {item.label}
                <Icon name="arrow" size={16} />
              </Link>
            ))}
          </div>
          <div className="welcome-signals">
            <span>
              <i />
              Dữ liệu nghiệp vụ trực tiếp
            </span>
            <span>
              <i />
              Phân quyền theo phạm vi
            </span>
            <span>
              <i />
              Không dùng số liệu giả lập
            </span>
          </div>
        </div>
        <div
          className="welcome-orbit-wrap"
          aria-label={`Tiến độ trung bình ${Math.round(metrics.average)}%`}
        >
          <div className="orbit-notations" aria-hidden="true">
            <span>α</span>
            <span>07</span>
            <span>∞</span>
          </div>
          <div
            className="welcome-orbit"
            style={{
              background: `conic-gradient(#72ead6 0 ${Math.round(metrics.average)}%,rgba(255,255,255,.07) ${Math.round(metrics.average)}% 100%)`,
            }}
          >
            <div>
              <small>TIẾN ĐỘ</small>
              <strong>
                {Math.round(metrics.average)}
                <b>%</b>
              </strong>
              <span>trung bình</span>
            </div>
          </div>
          <i className="orbit-ring ring-a" />
          <i className="orbit-ring ring-b" />
          <i className="orbit-ring ring-c" />
        </div>
        <aside className="welcome-telemetry">
          <span>
            <small>Ghi danh</small>
            <strong>{totalRows}</strong>
            <i
              style={{ width: `${Math.min(100, Math.max(8, totalRows * 4))}%` }}
            />
          </span>
          <span>
            <small>Hoàn thành</small>
            <strong>{completed}</strong>
            <i style={{ width: `${Math.max(8, completedPercent)}%` }} />
          </span>
          <span className={overdue ? "danger" : ""}>
            <small>Quá hạn</small>
            <strong>{overdue}</strong>
            <i style={{ width: `${Math.max(8, overduePercent)}%` }} />
          </span>
        </aside>
      </section>

      {warning && (
        <div className="dashboard-warning">
          {warning}
          <button onClick={() => void load()}>Tải lại</button>
        </div>
      )}
      <div className="dashboard-section-heading">
        <div>
          <span>01 · TÍN HIỆU CỐT LÕI</span>
          <h2>Nhịp vận hành hôm nay</h2>
        </div>
        <p>Số liệu tổng hợp theo quyền truy cập hiện tại</p>
      </div>
      <section className="kpi-grid">
        {kpis.map((item, index) => (
          <Kpi key={item.label} {...item} loading={loading} index={index + 1} />
        ))}
      </section>

      <section className="dashboard-route-deck">
        <div className="route-deck-copy">
          <span>02 · LỐI ĐI ƯU TIÊN</span>
          <h2>Mở nhanh không gian làm việc</h2>
          <p>
            Những tác vụ được dùng nhiều nhất, đặt ngay tại trung tâm điều hành.
          </p>
        </div>
        <div className="route-deck-links">
          {quickActions.map((item, index) => (
            <Link href={item.href} key={item.href}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <i>
                <Icon name={item.icon} />
              </i>
              <div>
                <strong>{item.label}</strong>
                <small>{item.hint}</small>
              </div>
              <Icon name="arrow" />
            </Link>
          ))}
        </div>
      </section>

      <div className="dashboard-section-heading">
        <div>
          <span>03 · TINH ĐỒ DỮ LIỆU</span>
          <h2>Diễn biến học tập</h2>
        </div>
        <p>Cập nhật từ Learning và Reporting Service</p>
      </div>

      <section className="dashboard-grid">
        <article className="panel chart-panel">
          <PanelTitle
            title="Tiến độ khóa học"
            subtitle={
              progressRows.length
                ? "Các ghi danh gần nhất"
                : "Chưa có dữ liệu tiến độ"
            }
          />
          {progressRows.length ? (
            <div className="bars">
              {progressRows.map((row, index) => {
                const value = Math.max(
                  0,
                  Math.min(100, number(row.progressPercent)),
                );
                return (
                  <div
                    className="bar-col"
                    key={String(row.enrollmentId ?? row.id ?? index)}
                    title={courseNames.get(String(row.courseId)) ?? "Khóa học"}
                  >
                    <div className="bar-track">
                      <div style={{ height: `${value}%` }} className="bar-fill">
                        <span>{Math.round(value)}%</span>
                      </div>
                    </div>
                    <small>
                      {(
                        courseNames.get(String(row.courseId)) ??
                        `KH ${index + 1}`
                      ).slice(0, 8)}
                    </small>
                  </div>
                );
              })}
            </div>
          ) : (
            <EmptyPanel text="Dữ liệu sẽ xuất hiện sau khi học viên được ghi danh." />
          )}
          <div className="legend">
            <span>
              <i className="legend-primary" />
              Tiến độ đã ghi nhận
            </span>
            <span>
              <i />
              Dữ liệu từ Learning/Reporting Service
            </span>
          </div>
        </article>

        <article className="panel">
          <PanelTitle
            title="Trạng thái học tập"
            subtitle={isStudent ? "Cá nhân" : "Trong phạm vi quyền"}
          />
          <div className="donut-wrap">
            <div className="donut" style={donutStyle}>
              <div>
                <b>{totalRows}</b>
                <small>Tổng ghi danh</small>
              </div>
            </div>
            <div className="donut-legend">
              <div>
                <i className="c1" />
                <span>Đã hoàn thành</span>
                <b>{completed}</b>
              </div>
              <div>
                <i className="c2" />
                <span>Đang học</span>
                <b>{inProgress}</b>
              </div>
              <div>
                <i className="c3" />
                <span>Chưa bắt đầu</span>
                <b>{notStarted}</b>
              </div>
              <div>
                <i className="c4" />
                <span>Quá hạn</span>
                <b>{overdue}</b>
              </div>
            </div>
          </div>
        </article>

        <article className="panel wide">
          <PanelTitle
            title="Thông báo gần đây"
            subtitle={`${state.notifications.unread} thông báo chưa đọc`}
          />
          {state.notifications.items.length ? (
            <div className="activity-list">
              {state.notifications.items.slice(0, 4).map((item, index) => (
                <div className="activity" key={item.id}>
                  <span className={`activity-icon a${(index % 4) + 1}`}>
                    <Icon
                      name={
                        item.title.includes("Chứng chỉ")
                          ? "certificate"
                          : item.title.includes("kiểm tra")
                            ? "exam"
                            : "bell"
                      }
                    />
                  </span>
                  <div>
                    <b>{item.title}</b>
                    <p>{item.body}</p>
                  </div>
                  <time>{relativeTime(item.createdAt)}</time>
                </div>
              ))}
            </div>
          ) : (
            <EmptyPanel text="Chưa có thông báo nghiệp vụ mới." />
          )}
        </article>

        <article className="panel">
          <PanelTitle
            title="Cần chú ý"
            subtitle={
              state.report?.lastSynchronizedAt
                ? `Đồng bộ ${date(state.report.lastSynchronizedAt)}`
                : "Dữ liệu hiện tại"
            }
          />
          <div className="attention">
            <div>
              <span className={`attention-num ${overdue ? "red" : "blue"}`}>
                {overdue}
              </span>
              <p>{isStudent ? "Khóa học quá hạn" : "Lượt học quá hạn"}</p>
              <Link href={isStudent ? "/learning" : "/reports"}>
                Xem chi tiết <Icon name="arrow" size={15} />
              </Link>
            </div>
            <div>
              <span
                className={`attention-num ${metrics.pendingGrades ? "orange" : "blue"}`}
              >
                {metrics.pendingGrades}
              </span>
              <p>Bài tự luận chờ chấm</p>
              {isStudent ? (
                <span className="attention-note">Theo dõi kết quả</span>
              ) : (
                <Link href="/grading">
                  Mở hàng chờ <Icon name="arrow" size={15} />
                </Link>
              )}
            </div>
            <div>
              <span className="attention-num blue">
                {state.notifications.unread}
              </span>
              <p>Thông báo chưa đọc</p>
              <span className="attention-note">Trên thanh công cụ</span>
            </div>
          </div>
        </article>
      </section>
    </>
  );
}

function Kpi({
  icon,
  value,
  label,
  delta,
  warning,
  loading,
  index,
}: {
  icon: IconName;
  value: string;
  label: string;
  delta: string;
  warning?: boolean;
  loading: boolean;
  index: number;
}) {
  return (
    <article className={`kpi ${warning ? "kpi-warning" : ""}`}>
      <span className="kpi-index">{String(index).padStart(2, "0")}</span>
      <div className={`kpi-icon ${warning ? "warn" : ""}`}>
        <Icon name={icon} />
        <i />
      </div>
      <div className="kpi-copy">
        <small>REALM METRIC</small>
        <strong>{loading ? "…" : value}</strong>
        <span>{label}</span>
        <em className={warning ? "warning" : ""}>
          {loading ? "Đang đồng bộ" : delta}
        </em>
      </div>
      <div className="kpi-signal" aria-hidden="true">
        <i />
        <i />
        <i />
        <i />
        <i />
      </div>
    </article>
  );
}
function PanelTitle({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="panel-title">
      <div>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
    </div>
  );
}
function EmptyPanel({ text }: { text: string }) {
  return (
    <div className="empty-panel">
      <Icon name="book" />
      <p>{text}</p>
    </div>
  );
}
