"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { PageHeader } from "./PageHeader";
import { StatusBadge } from "./StatusBadge";

type TrainingClass = {
  id: string;
  code: string;
  name: string;
  courseId: string;
  courseVersion: number;
  status: string;
};
type Course = { id: string; code: string; name: string };
type PathItem = {
  id: string;
  classId: string;
  courseId: string;
  courseVersion: number;
  sortOrder: number;
  required: boolean;
  unlockMode: string;
  dueOffsetDays: number;
};
type LearningPath = {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  status: string;
  ownerId: string;
  publishedAt?: string | null;
  updatedAt: string;
  items: PathItem[];
};
type MyPathItem = {
  itemId: string;
  classId: string;
  courseId: string;
  courseVersion: number;
  enrollmentId?: string | null;
  sortOrder: number;
  required: boolean;
  unlocked: boolean;
  progressPercent: number;
  learningStatus: string;
  dueAt?: string | null;
};
type MyPath = {
  assignmentId: string;
  pathId: string;
  code: string;
  name: string;
  description?: string | null;
  status: string;
  progressPercent: number;
  dueAt?: string | null;
  assignedAt: string;
  completedAt?: string | null;
  items: MyPathItem[];
};
type PathAssignment = {
  id: string;
  pathId: string;
  assigneeType: string;
  assigneeId: string;
  dueAt?: string | null;
  assignedAt: string;
  status: string;
  expandedUsers: number;
};
type Participant = {
  userId: string;
  status: string;
  dueAt?: string | null;
  assignedAt: string;
  completedAt?: string | null;
};

function can(user: PortalUser, permission: string) {
  return (
    user.accountType === "SYSTEM_ADMIN" || user.permissions.includes(permission)
  );
}
function when(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(date);
}

export function LearningPathCenter({ user }: { user: PortalUser }) {
  const canManage = can(user, "learning-paths:manage");
  const canAssign = can(user, "learning-paths:assign");
  const canRead = can(user, "learning-paths:read");
  const [paths, setPaths] = useState<LearningPath[]>([]);
  const [mine, setMine] = useState<MyPath[]>([]);
  const [classes, setClasses] = useState<TrainingClass[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [busy, setBusy] = useState(false);
  const [tab, setTab] = useState<"mine" | "manage">(
    canManage || canAssign ? "manage" : "mine",
  );
  const [selectedPathId, setSelectedPathId] = useState("");
  const [assignments, setAssignments] = useState<PathAssignment[]>([]);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [draft, setDraft] = useState({
    code: "",
    name: "",
    description: "",
    classIds: [] as string[],
  });
  const [assignment, setAssignment] = useState({
    assigneeType: "USER",
    assigneeId: "",
    dueAt: "",
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [pathData, myData, classData, courseData] = await Promise.all([
        canManage || canAssign
          ? apiRequest<LearningPath[]>("/api/v1/learning-paths")
          : Promise.resolve([]),
        canRead
          ? apiRequest<MyPath[]>("/api/v1/learning-paths/me/assigned")
          : Promise.resolve([]),
        canManage || canAssign
          ? apiRequest<TrainingClass[] | { items?: TrainingClass[] }>(
              "/api/v1/classes",
            )
          : Promise.resolve([]),
        apiRequest<Course[] | { items?: Course[] }>("/api/v1/courses?size=500"),
      ]);
      setPaths(pathData);
      setMine(myData);
      setClasses(unwrapItems(classData));
      setCourses(unwrapItems(courseData));
      if (!selectedPathId && pathData[0]) setSelectedPathId(pathData[0].id);
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Không thể tải lộ trình đào tạo",
      );
    } finally {
      setLoading(false);
    }
  }, [canManage, canAssign, canRead, selectedPathId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!selectedPathId || (!canManage && !canAssign)) {
      setAssignments([]);
      setParticipants([]);
      return;
    }
    void Promise.all([
      apiRequest<PathAssignment[]>(
        `/api/v1/learning-paths/${selectedPathId}/assignments`,
      ),
      apiRequest<Participant[]>(
        `/api/v1/learning-paths/${selectedPathId}/participants`,
      ),
    ])
      .then(([assignmentData, participantData]) => {
        setAssignments(assignmentData);
        setParticipants(participantData);
      })
      .catch(() => {
        setAssignments([]);
        setParticipants([]);
      });
  }, [selectedPathId, canManage, canAssign]);

  const courseById = useMemo(
    () => new Map(courses.map((item) => [item.id, item])),
    [courses],
  );
  const classById = useMemo(
    () => new Map(classes.map((item) => [item.id, item])),
    [classes],
  );
  const selectedPath = paths.find((item) => item.id === selectedPathId);

  async function createPath(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft.classIds.length) {
      setToast("Hãy chọn ít nhất một lớp để tạo lộ trình.");
      return;
    }
    setBusy(true);
    try {
      await apiRequest("/api/v1/learning-paths", {
        method: "POST",
        body: JSON.stringify({
          code: draft.code,
          name: draft.name,
          description: draft.description || null,
          items: draft.classIds.map((classId, index) => ({
            classId,
            required: true,
            unlockMode: index === 0 ? "IMMEDIATE" : "AFTER_PREVIOUS",
            dueOffsetDays: 0,
          })),
        }),
      });
      setDraft({ code: "", name: "", description: "", classIds: [] });
      setToast("Đã tạo lộ trình ở trạng thái bản nháp.");
      await load();
    } catch (cause) {
      setToast(
        cause instanceof Error ? cause.message : "Không thể tạo lộ trình",
      );
    } finally {
      setBusy(false);
    }
  }

  async function act(pathId: string, action: "publish" | "clone" | "archive") {
    setBusy(true);
    try {
      await apiRequest(`/api/v1/learning-paths/${pathId}/${action}`, {
        method: "POST",
      });
      setToast(
        action === "publish"
          ? "Đã xuất bản lộ trình."
          : action === "clone"
            ? "Đã tạo bản sao để chỉnh sửa."
            : "Đã lưu trữ lộ trình.",
      );
      await load();
    } catch (cause) {
      setToast(
        cause instanceof Error ? cause.message : "Không thể cập nhật lộ trình",
      );
    } finally {
      setBusy(false);
    }
  }

  async function assignPath(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedPathId) return;
    setBusy(true);
    try {
      await apiRequest(`/api/v1/learning-paths/${selectedPathId}/assignments`, {
        method: "POST",
        body: JSON.stringify({
          assigneeType: assignment.assigneeType,
          assigneeId: assignment.assigneeId,
          dueAt: assignment.dueAt
            ? new Date(assignment.dueAt).toISOString()
            : null,
        }),
      });
      setAssignment({ ...assignment, assigneeId: "", dueAt: "" });
      setToast("Đã giao lộ trình và tự động ghi danh các lớp thành phần.");
      await load();
    } catch (cause) {
      setToast(
        cause instanceof Error ? cause.message : "Không thể giao lộ trình",
      );
    } finally {
      setBusy(false);
    }
  }

  if (loading)
    return <LoadingState label="Đang kết nối các tinh tuyến học tập..." />;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <>
      <PageHeader
        eyebrow="LEARNING PATHS · TINH TUYẾN PHÁT TRIỂN"
        title="Lộ trình phát triển"
        description="Kết nối nhiều lớp thành một hành trình tuần tự, giao theo cá nhân hoặc cơ cấu tổ chức và theo dõi tiến độ tập trung."
        icon="target"
        actions={
          <button className="button secondary" onClick={() => void load()}>
            <Icon name="refresh" />
            Làm mới
          </button>
        }
      />
      <div className="workspace-tabs path-tabs">
        {canRead && (
          <button
            className={tab === "mine" ? "active" : ""}
            onClick={() => setTab("mine")}
          >
            Lộ trình của tôi
          </button>
        )}
        {(canManage || canAssign) && (
          <button
            className={tab === "manage" ? "active" : ""}
            onClick={() => setTab("manage")}
          >
            Thiết kế & phân phối
          </button>
        )}
      </div>

      {tab === "mine" ? (
        <section className="learning-path-grid">
          {mine.length ? (
            mine.map((path) => (
              <article className="learning-path-card" key={path.assignmentId}>
                <header>
                  <div>
                    <small>{path.code}</small>
                    <h2>{path.name}</h2>
                    <p>{path.description || "Lộ trình đào tạo được giao."}</p>
                  </div>
                  <StatusBadge value={path.status} />
                </header>
                <div className="path-progress">
                  <span>
                    <i style={{ width: `${path.progressPercent}%` }} />
                  </span>
                  <strong>{path.progressPercent}%</strong>
                </div>
                <div className="path-meta">
                  <span>
                    <Icon name="calendar" size={15} />
                    Hạn {when(path.dueAt)}
                  </span>
                  <span>{path.items.length} chặng</span>
                </div>
                <ol className="path-steps">
                  {path.items.map((item) => {
                    const course = courseById.get(item.courseId);
                    return (
                      <li
                        key={item.itemId}
                        className={`${item.unlocked ? "unlocked" : "locked"} ${item.progressPercent >= 100 ? "done" : ""}`}
                      >
                        <span>
                          {item.progressPercent >= 100 ? (
                            <Icon name="check" size={15} />
                          ) : (
                            item.sortOrder + 1
                          )}
                        </span>
                        <div>
                          <strong>
                            {course?.name ??
                              `Khóa ${item.courseId.slice(0, 8)}`}
                          </strong>
                          <small>
                            {item.progressPercent}% · {item.learningStatus}
                            {item.dueAt ? ` · hạn ${when(item.dueAt)}` : ""}
                          </small>
                        </div>
                        {item.unlocked && item.enrollmentId ? (
                          <Link
                            className="button secondary compact"
                            href={`/learning/${item.enrollmentId}`}
                          >
                            {item.progressPercent ? "Tiếp tục" : "Bắt đầu"}
                          </Link>
                        ) : (
                          <Icon name="lock" size={17} />
                        )}
                      </li>
                    );
                  })}
                </ol>
              </article>
            ))
          ) : (
            <EmptyState
              title="Chưa có lộ trình"
              description="Lộ trình được giao theo cá nhân, nhóm, phòng ban hoặc chi nhánh sẽ xuất hiện tại đây."
            />
          )}
        </section>
      ) : (
        <div className="advanced-grid learning-path-admin">
          {canManage && (
            <section className="section-card advanced-panel">
              <div className="section-title">
                <div>
                  <h2>Tạo lộ trình</h2>
                  <p>
                    Các lớp được chọn theo thứ tự; chặng sau mặc định mở khi
                    hoàn thành chặng trước.
                  </p>
                </div>
              </div>
              <form className="form-stack" onSubmit={createPath}>
                <div className="form-grid two">
                  <label>
                    Mã lộ trình
                    <input
                      required
                      maxLength={80}
                      value={draft.code}
                      onChange={(e) =>
                        setDraft({ ...draft, code: e.target.value })
                      }
                    />
                  </label>
                  <label>
                    Tên lộ trình
                    <input
                      required
                      maxLength={220}
                      value={draft.name}
                      onChange={(e) =>
                        setDraft({ ...draft, name: e.target.value })
                      }
                    />
                  </label>
                </div>
                <label>
                  Mô tả
                  <textarea
                    value={draft.description}
                    onChange={(e) =>
                      setDraft({ ...draft, description: e.target.value })
                    }
                  />
                </label>
                <label>
                  Chọn lớp theo thứ tự
                  <select
                    multiple
                    size={Math.min(10, Math.max(5, classes.length))}
                    value={draft.classIds}
                    onChange={(e) =>
                      setDraft({
                        ...draft,
                        classIds: Array.from(
                          e.currentTarget.selectedOptions,
                          (option) => option.value,
                        ),
                      })
                    }
                  >
                    {classes
                      .filter((item) => item.status === "OPEN")
                      .map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.code} · {item.name} · v{item.courseVersion}
                        </option>
                      ))}
                  </select>
                  <small>
                    Giữ Ctrl/Cmd để chọn nhiều lớp. Thứ tự hiện tại theo danh
                    sách lớp.
                  </small>
                </label>
                <button className="button primary" disabled={busy}>
                  <Icon name="plus" />
                  {busy ? "Đang lưu..." : "Tạo bản nháp"}
                </button>
              </form>
            </section>
          )}
          <section className="section-card advanced-panel">
            <div className="section-title">
              <div>
                <h2>Danh mục lộ trình</h2>
                <p>
                  Đã xuất bản là bất biến; hãy nhân bản để tạo phiên bản mới.
                </p>
              </div>
            </div>
            <div className="compact-list path-list">
              {paths.length ? (
                paths.map((path) => (
                  <article
                    key={path.id}
                    className={selectedPathId === path.id ? "selected" : ""}
                    onClick={() => setSelectedPathId(path.id)}
                  >
                    <div>
                      <strong>{path.name}</strong>
                      <small>
                        {path.code} · {path.items.length} chặng · cập nhật{" "}
                        {when(path.updatedAt)}
                      </small>
                    </div>
                    <StatusBadge value={path.status} />
                    <div className="inline-actions">
                      {canManage && path.status === "DRAFT" && (
                        <button
                          className="button primary compact"
                          disabled={busy}
                          onClick={(e) => {
                            e.stopPropagation();
                            void act(path.id, "publish");
                          }}
                        >
                          Xuất bản
                        </button>
                      )}
                      {canManage && (
                        <button
                          className="button secondary compact"
                          disabled={busy}
                          onClick={(e) => {
                            e.stopPropagation();
                            void act(path.id, "clone");
                          }}
                        >
                          Nhân bản
                        </button>
                      )}
                      {canManage && path.status !== "ARCHIVED" && (
                        <button
                          className="icon-button danger"
                          disabled={busy}
                          onClick={(e) => {
                            e.stopPropagation();
                            void act(path.id, "archive");
                          }}
                          aria-label="Lưu trữ"
                        >
                          <Icon name="trash" />
                        </button>
                      )}
                    </div>
                  </article>
                ))
              ) : (
                <EmptyState
                  title="Chưa có lộ trình"
                  description="Tạo lộ trình đầu tiên từ các lớp đã mở."
                />
              )}
            </div>
          </section>
          {selectedPath && (
            <section className="section-card advanced-panel wide-panel">
              <div className="section-title">
                <div>
                  <h2>{selectedPath.name}</h2>
                  <p>{selectedPath.description || "Không có mô tả"}</p>
                </div>
                <StatusBadge value={selectedPath.status} />
              </div>
              <div className="path-manage-layout">
                <div>
                  <h3>Các chặng</h3>
                  <ol className="path-steps manager">
                    {selectedPath.items.map((item) => {
                      const klass = classById.get(item.classId);
                      const course = courseById.get(item.courseId);
                      return (
                        <li key={item.id}>
                          <span>{item.sortOrder + 1}</span>
                          <div>
                            <strong>{course?.name ?? item.courseId}</strong>
                            <small>
                              {klass?.code} · {klass?.name} · v
                              {item.courseVersion} · {item.unlockMode}
                            </small>
                          </div>
                          {item.required && <em>Bắt buộc</em>}
                        </li>
                      );
                    })}
                  </ol>
                </div>
                {canAssign && selectedPath.status === "PUBLISHED" && (
                  <form
                    className="form-stack assignment-form"
                    onSubmit={assignPath}
                  >
                    <h3>Giao lộ trình</h3>
                    <label>
                      Loại đối tượng
                      <select
                        value={assignment.assigneeType}
                        onChange={(e) =>
                          setAssignment({
                            ...assignment,
                            assigneeType: e.target.value,
                          })
                        }
                      >
                        <option value="USER">Tài khoản</option>
                        <option value="GROUP">Nhóm</option>
                        <option value="DEPARTMENT">Phòng ban</option>
                        <option value="BRANCH">Chi nhánh</option>
                      </select>
                    </label>
                    <label>
                      ID đối tượng
                      <input
                        required
                        value={assignment.assigneeId}
                        onChange={(e) =>
                          setAssignment({
                            ...assignment,
                            assigneeId: e.target.value,
                          })
                        }
                        placeholder="UUID người dùng hoặc đơn vị"
                      />
                    </label>
                    <label>
                      Hạn hoàn thành
                      <input
                        type="datetime-local"
                        value={assignment.dueAt}
                        onChange={(e) =>
                          setAssignment({
                            ...assignment,
                            dueAt: e.target.value,
                          })
                        }
                      />
                    </label>
                    <button className="button primary" disabled={busy}>
                      <Icon name="users" />
                      Giao và ghi danh
                    </button>
                  </form>
                )}
              </div>
              <div className="path-stat-row">
                <span>
                  <strong>
                    {
                      assignments.filter((item) => item.status === "ACTIVE")
                        .length
                    }
                  </strong>
                  Lần giao đang hiệu lực
                </span>
                <span>
                  <strong>{participants.length}</strong>Người tham gia
                </span>
                <span>
                  <strong>
                    {
                      participants.filter((item) => item.status === "COMPLETED")
                        .length
                    }
                  </strong>
                  Đã hoàn thành
                </span>
                <span>
                  <strong>
                    {
                      participants.filter((item) => item.status === "OVERDUE")
                        .length
                    }
                  </strong>
                  Quá hạn
                </span>
              </div>
            </section>
          )}
        </div>
      )}
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </>
  );
}
