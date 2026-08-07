"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest } from "@/lib/api";
import type { Course } from "@/lib/models";
import { formatDate } from "@/lib/models";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Icon } from "./Icon";
import { Modal } from "./Modal";
import { StatusBadge } from "./StatusBadge";

type StudentDirectoryItem = {
  id: string;
  code: string;
  fullName: string;
  email?: string | null;
};

type CourseAssignment = {
  id: string;
  courseId: string;
  assigneeType: "USER" | "GROUP" | "DEPARTMENT" | "BRANCH";
  assigneeId: string;
  assignedVersion: number;
  assignedAt: string;
  availableFrom?: string | null;
  dueAt?: string | null;
  required: boolean;
  status: "ACTIVE" | "CANCELLED" | "COMPLETED";
  enrolledUsers: number;
};

export function CourseLearnersPanel({ course }: { course: Course }) {
  const [students, setStudents] = useState<StudentDirectoryItem[]>([]);
  const [assignments, setAssignments] = useState<CourseAssignment[]>([]);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [directory, assignmentRows] = await Promise.all([
        apiRequest<StudentDirectoryItem[]>("/api/v1/directory/students"),
        apiRequest<CourseAssignment[]>(
          `/api/v1/course-assignments?courseId=${course.id}`,
        ),
      ]);
      setStudents(directory);
      setAssignments(assignmentRows);
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Không thể tải danh sách học viên",
      );
    } finally {
      setLoading(false);
    }
  }, [course.id]);

  useEffect(() => {
    void load();
  }, [load]);

  const studentMap = useMemo(
    () => new Map(students.map((item) => [item.id, item])),
    [students],
  );
  const activeStudentIds = useMemo(
    () =>
      new Set(
        assignments
          .filter(
            (item) => item.status === "ACTIVE" && item.assigneeType === "USER",
          )
          .map((item) => item.assigneeId),
      ),
    [assignments],
  );
  const available = students.filter((item) => {
    if (activeStudentIds.has(item.id)) return false;
    const normalized = query.trim().toLocaleLowerCase("vi-VN");
    return (
      !normalized ||
      `${item.fullName} ${item.code} ${item.email ?? ""}`
        .toLocaleLowerCase("vi-VN")
        .includes(normalized)
    );
  });

  async function assign(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected.length) return;
    const data = new FormData(event.currentTarget);
    const dueAt = String(data.get("dueAt") ?? "");
    setBusy(true);
    setFormError("");
    try {
      await Promise.all(
        selected.map((studentId) =>
          apiRequest("/api/v1/course-assignments", {
            method: "POST",
            body: JSON.stringify({
              courseId: course.id,
              assigneeType: "USER",
              assigneeId: studentId,
              availableFrom: null,
              dueAt: dueAt ? new Date(`${dueAt}T23:59:59`).toISOString() : null,
              gracePeriodMinutes: 0,
              required: true,
            }),
          }),
        ),
      );
      setOpen(false);
      setSelected([]);
      setToast(`Đã giao khóa học cho ${selected.length} học viên.`);
      await load();
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể giao khóa học",
      );
    } finally {
      setBusy(false);
    }
  }

  async function revoke(assignment: CourseAssignment) {
    const learner = studentMap.get(assignment.assigneeId);
    if (
      !window.confirm(
        `Thu hồi khóa học khỏi ${learner?.fullName ?? "học viên này"}?`,
      )
    )
      return;
    setBusy(true);
    try {
      await apiRequest(`/api/v1/course-assignments/${assignment.id}`, {
        method: "DELETE",
      });
      setToast("Đã thu hồi khóa học.");
      await load();
    } catch (caught) {
      setToast(
        caught instanceof Error ? caught.message : "Không thể thu hồi khóa học",
      );
    } finally {
      setBusy(false);
    }
  }

  if (loading)
    return <LoadingState label="Đang tải học viên của khóa học..." />;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;

  const active = assignments.filter((item) => item.status === "ACTIVE");
  return (
    <>
      <section className="section-card course-learners-panel">
        <div className="section-title">
          <div>
            <h2>Học viên được giao khóa học</h2>
            <p>
              Giao trực tiếp khóa học đã xuất bản. Hệ thống tự tạo ghi danh cho
              từng học viên.
            </p>
          </div>
          <button
            className="button primary"
            onClick={() => {
              setFormError("");
              setOpen(true);
            }}
            disabled={course.status !== "PUBLISHED"}
          >
            <Icon name="plus" /> Giao khóa học
          </button>
        </div>
        {course.status !== "PUBLISHED" && (
          <div className="form-alert info">
            <Icon name="warning" />
            Xuất bản khóa học trước khi giao cho học viên.
          </div>
        )}
        {active.length ? (
          <div className="responsive-table">
            <table>
              <thead>
                <tr>
                  <th>Học viên</th>
                  <th>Mã</th>
                  <th>Phiên bản</th>
                  <th>Ngày giao</th>
                  <th>Hạn hoàn thành</th>
                  <th>Trạng thái</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {active.map((item) => {
                  const learner = studentMap.get(item.assigneeId);
                  return (
                    <tr key={item.id}>
                      <td>
                        <div className="person-cell">
                          <span className="avatar small">
                            {(learner?.fullName ?? "HV")
                              .split(" ")
                              .slice(-2)
                              .map((part) => part[0])
                              .join("")
                              .toUpperCase()}
                          </span>
                          <span>
                            <strong>
                              {learner?.fullName ?? item.assigneeId}
                            </strong>
                            <small>{learner?.email ?? ""}</small>
                          </span>
                        </div>
                      </td>
                      <td>{learner?.code ?? "—"}</td>
                      <td>v{item.assignedVersion}</td>
                      <td>{formatDate(item.assignedAt)}</td>
                      <td>{formatDate(item.dueAt)}</td>
                      <td>
                        <StatusBadge value={item.status} />
                      </td>
                      <td>
                        <button
                          className="button danger compact"
                          disabled={busy}
                          onClick={() => void revoke(item)}
                        >
                          <Icon name="trash" />
                          Thu hồi
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            title="Chưa giao cho học viên"
            description="Khi được giao, khóa học sẽ xuất hiện ngay trong không gian Học viên."
          />
        )}
      </section>

      <Modal
        open={open}
        onClose={() => !busy && setOpen(false)}
        title="Giao khóa học"
        description="Chỉ tài khoản có vai trò Học viên mới xuất hiện trong danh sách."
      >
        <form className="form-stack" onSubmit={assign}>
          <label>
            Tìm học viên
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tên, mã hoặc email"
            />
          </label>
          <label>
            Hạn hoàn thành
            <input name="dueAt" type="date" />
          </label>
          <div className="selection-list course-student-picker">
            {available.length ? (
              available.map((item) => (
                <label className="select-person" key={item.id}>
                  <input
                    type="checkbox"
                    checked={selected.includes(item.id)}
                    onChange={(event) =>
                      setSelected((current) =>
                        event.target.checked
                          ? [...current, item.id]
                          : current.filter((id) => id !== item.id),
                      )
                    }
                  />
                  <span className="avatar small">
                    {item.fullName
                      .split(" ")
                      .slice(-2)
                      .map((part) => part[0])
                      .join("")
                      .toUpperCase()}
                  </span>
                  <span>
                    <strong>{item.fullName}</strong>
                    <small>
                      {item.code}
                      {item.email ? ` · ${item.email}` : ""}
                    </small>
                  </span>
                </label>
              ))
            ) : (
              <p className="selection-empty">Không còn học viên phù hợp.</p>
            )}
          </div>
          {formError && (
            <div className="form-alert error">
              <Icon name="warning" />
              {formError}
            </div>
          )}
          <div className="modal-actions">
            <button
              type="button"
              className="button secondary"
              onClick={() => setOpen(false)}
            >
              Hủy
            </button>
            <button
              className="button primary"
              disabled={busy || selected.length === 0}
            >
              {busy ? "Đang giao..." : `Giao cho ${selected.length} học viên`}
            </button>
          </div>
        </form>
      </Modal>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </>
  );
}
