"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Course, PageResponse } from "@/lib/models";
import { formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { instructorCoursePath } from "@/lib/portal-paths";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Modal } from "./Modal";
import { StatusBadge } from "./StatusBadge";

const statusOptions = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "PUBLISHED", label: "Đã xuất bản" },
  { value: "DRAFT", label: "Bản nháp" },
  { value: "HIDDEN", label: "Tạm ẩn" },
  { value: "ARCHIVED", label: "Lưu trữ" },
];

export function CoursesPage({ user }: { user: PortalUser }) {
  const canCreate = user.permissions.some((permission) => ["courses:create", "courses:write"].includes(permission));
  const [courses, setCourses] = useState<Course[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ size: "100" });
      if (query.trim()) params.set("query", query.trim());
      if (status) params.set("status", status);
      const payload = await apiRequest<PageResponse<Course> | Course[]>(
        `/api/v1/courses?${params}`,
      );
      setCourses(unwrapItems(payload));
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Không thể tải danh sách khóa học",
      );
    } finally {
      setLoading(false);
    }
  }, [query, status]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 180);
    return () => window.clearTimeout(timer);
  }, [load]);

  const courseList = useMemo(
    () =>
      Array.isArray(courses) ? courses : unwrapItems<Course>(courses as any),
    [courses],
  );

  const counts = useMemo(
    () => ({
      all: courseList.length,
      published: courseList.filter((item) => item.status === "PUBLISHED")
        .length,
      draft: courseList.filter((item) => item.status === "DRAFT").length,
    }),
    [courseList],
  );

  async function createCourse(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    try {
      const created = await apiRequest<Course>("/api/v1/courses", {
        method: "POST",
        body: JSON.stringify({
          code: String(data.get("code") ?? ""),
          name: String(data.get("name") ?? ""),
          description: String(data.get("description") ?? "") || null,
          objectives: String(data.get("objectives") ?? "") || null,
          targetAudience: String(data.get("targetAudience") ?? "") || null,
          durationMinutes: Number(data.get("durationMinutes") || 0),
          passingScore: Number(data.get("passingScore") || 70),
          completionPolicyJson: '{"requiredLessonPercent":100}',
          categoryId: null,
        }),
      });
      setOpen(false);
      setToast(
        "Đã tạo khóa học. Bạn có thể mở khóa học để thêm bài học và xuất bản.",
      );
      await load();
      window.setTimeout(
        () => window.location.assign(instructorCoursePath(created.id)),
        450,
      );
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể tạo khóa học",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Nội dung đào tạo"
        title="Khóa học"
        description="Tạo nội dung, tổ chức bài học và xuất bản khóa học theo đúng luồng vận hành."
        icon="book"
        actions={
          canCreate ? (
            <button className="button primary" onClick={() => setOpen(true)}>
              <Icon name="plus" />
              Tạo khóa học
            </button>
          ) : undefined
        }
      />

      <section className="summary-row">
        <div className="summary-card">
          <span className="summary-index">01</span>
          <i className="summary-glyph">
            <Icon name="book" />
          </i>
          <div>
            <span>Tổng khóa học</span>
            <small>Trong phạm vi bạn quản lý</small>
          </div>
          <strong>{counts.all}</strong>
          <em>Danh mục</em>
        </div>
        <div className="summary-card">
          <span className="summary-index">02</span>
          <i className="summary-glyph">
            <Icon name="check" />
          </i>
          <div>
            <span>Đã xuất bản</span>
            <small>Sẵn sàng giao học viên</small>
          </div>
          <strong>{counts.published}</strong>
          <em>Sẵn sàng</em>
        </div>
        <div className="summary-card">
          <span className="summary-index">03</span>
          <i className="summary-glyph">
            <Icon name="edit" />
          </i>
          <div>
            <span>Bản nháp</span>
            <small>Đang xây dựng nội dung</small>
          </div>
          <strong>{counts.draft}</strong>
          <em>Đang soạn</em>
        </div>
      </section>

      <section className="toolbar-card">
        <label className="search-field">
          <Icon name="search" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm theo tên hoặc mã khóa học"
          />
        </label>
        <label className="select-field">
          <Icon name="filter" />
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            {statusOptions.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>
        <button
          className="button secondary"
          onClick={() => void load()}
          disabled={loading}
        >
          <Icon name="refresh" />
          Làm mới
        </button>
      </section>

      {loading ? (
        <LoadingState />
      ) : error ? (
        <ErrorState message={error} onRetry={() => void load()} />
      ) : courses.length === 0 ? (
        <EmptyState
          title="Chưa có khóa học"
          description="Tạo khóa học đầu tiên để thêm bài học, bài kiểm tra, xuất bản và giao cho học viên."
          action={
            canCreate ? (
              <button className="button primary" onClick={() => setOpen(true)}>
                <Icon name="plus" />
                Tạo khóa học
              </button>
            ) : undefined
          }
        />
      ) : (
        <section className="course-grid">
          {courses.map((course, index) => (
            <Link
              href={instructorCoursePath(course.id)}
              className="course-card"
              key={course.id}
            >
              <div className={`course-cover cover-${index % 5}`}>
                <div className="course-color-shapes" aria-hidden="true">
                  <i />
                  <i />
                  <i />
                  <span>+</span>
                </div>
                <span className="course-code">{course.code}</span>
                <span className="course-volume">
                  KHÓA {String(index + 1).padStart(2, "0")}
                </span>
                <div className="course-symbol">
                  <Icon name="book" size={34} />
                  <i />
                </div>
                <small>Bộ nội dung · phiên bản {course.contentVersion}</small>
              </div>
              <div className="course-card-body">
                <span className="course-card-index">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <div className="course-card-top">
                  <StatusBadge value={course.status} />
                  <span>Phiên bản {course.contentVersion}</span>
                </div>
                <h2>{course.name}</h2>
                <p>
                  {course.description ||
                    "Chưa có mô tả. Mở khóa học để bổ sung nội dung chi tiết."}
                </p>
                <div className="course-meta">
                  <span>
                    <Icon name="clock" />
                    {formatDuration(course.durationMinutes)}
                  </span>
                  <span>
                    <Icon name="grade" />
                    {course.passingScore}% điểm đạt
                  </span>
                </div>
                <div className="course-card-footer">
                  <span>
                    <small>Chi tiết</small>Mở nội dung khóa học
                  </span>
                  <i>
                    <Icon name="arrow" />
                  </i>
                </div>
              </div>
            </Link>
          ))}
        </section>
      )}

      <Modal
        open={open}
        onClose={() => !saving && setOpen(false)}
        title="Tạo khóa học mới"
        description="Khóa học được lưu ở trạng thái bản nháp. Sau đó hãy thêm bài học và xuất bản."
      >
        <form className="form-stack" onSubmit={createCourse}>
          <div className="form-grid two">
            <label>
              Mã khóa học <b>*</b>
              <input
                name="code"
                required
                maxLength={80}
                placeholder="VD: ATTT-101"
              />
            </label>
            <label>
              Thời lượng (phút)
              <input
                name="durationMinutes"
                type="number"
                min="0"
                defaultValue="45"
              />
            </label>
          </div>
          <label>
            Tên khóa học <b>*</b>
            <input
              name="name"
              required
              maxLength={240}
              placeholder="Nhập tên khóa học rõ ràng"
            />
          </label>
          <label>
            Mô tả
            <textarea
              name="description"
              rows={3}
              placeholder="Giới thiệu ngắn gọn nội dung khóa học"
            />
          </label>
          <div className="form-grid two">
            <label>
              Đối tượng học
              <input
                name="targetAudience"
                placeholder="Nhân viên mới, quản lý..."
              />
            </label>
            <label>
              Điểm đạt (%)
              <input
                name="passingScore"
                type="number"
                min="0"
                max="100"
                defaultValue="70"
              />
            </label>
          </div>
          <label>
            Mục tiêu khóa học
            <textarea
              name="objectives"
              rows={3}
              placeholder="Sau khóa học, học viên có thể..."
            />
          </label>
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
              disabled={saving}
            >
              Hủy
            </button>
            <button className="button primary" disabled={saving}>
              {saving ? "Đang tạo..." : "Tạo và mở khóa học"}
            </button>
          </div>
        </form>
      </Modal>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </>
  );
}
