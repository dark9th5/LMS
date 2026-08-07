"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiRequest } from "@/lib/api";
import type { Course, CourseStatus, Lesson, LessonType } from "@/lib/models";
import { formatDate, formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { instructorCoursePath } from "@/lib/portal-paths";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Modal } from "./Modal";
import { LessonResource } from "./LessonResource";
import { CourseAssessmentsPanel } from "./CourseAssessmentsPanel";
import { CourseLearnersPanel } from "./CourseLearnersPanel";
import { StatusBadge } from "./StatusBadge";

const lessonTypeLabels: Record<LessonType, string> = {
  TEXT: "Nội dung văn bản",
  PDF: "Tài liệu PDF",
  DOCX: "Tài liệu DOCX",
  VIDEO: "Video",
  AUDIO: "Âm thanh",
  FILE: "Tệp đính kèm",
  ASSIGNMENT: "Bài thực hành",
  EXAM: "Bài kiểm tra",
};

function fileRequired(type: LessonType) {
  return !["TEXT", "ASSIGNMENT", "EXAM"].includes(type);
}

export function CourseDetail({
  courseId,
  user,
}: {
  courseId: string;
  user: PortalUser;
}) {
  const router = useRouter();
  const [course, setCourse] = useState<Course | null>(null);
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState<
    "content" | "assessments" | "learners" | "information" | "discussion"
  >("content");
  const [lessonModal, setLessonModal] = useState(false);
  const [editingLesson, setEditingLesson] = useState<Lesson | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState<{
    message: string;
    tone?: "success" | "error" | "info";
  } | null>(null);

  const canEdit = Boolean(
    course &&
    (user.permissions.includes("courses:update") ||
      user.permissions.includes("courses:write")),
  );
  const canPublish = Boolean(
    course && user.permissions.includes("courses:publish"),
  );
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const value = await apiRequest<Course>(`/api/v1/courses/${courseId}`);
      const sorted = {
        ...value,
        lessons: [...(value.lessons ?? [])].sort(
          (a, b) => a.sortOrder - b.sortOrder,
        ),
      };
      setCourse(sorted);
      setSelectedLessonId((current) =>
        current && sorted.lessons?.some((lesson) => lesson.id === current)
          ? current
          : (sorted.lessons?.[0]?.id ?? null),
      );
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể tải khóa học",
      );
    } finally {
      setLoading(false);
    }
  }, [courseId]);

  useEffect(() => {
    void load();
  }, [load]);
  const selectedLesson = useMemo(
    () => course?.lessons?.find((item) => item.id === selectedLessonId) ?? null,
    [course, selectedLessonId],
  );

  async function transition(target: CourseStatus) {
    if (!course || !canPublish) return;
    setSaving(true);
    try {
      await apiRequest(`/api/v1/courses/${course.id}/status/${target}`, {
        method: "POST",
      });
      setToast({
        message:
          target === "PUBLISHED"
            ? "Khóa học đã được xuất bản và có thể giao trực tiếp cho học viên."
            : "Đã cập nhật trạng thái khóa học.",
      });
      await load();
    } catch (caught) {
      setToast({
        message:
          caught instanceof Error ? caught.message : "Không thể đổi trạng thái",
        tone: "error",
      });
    } finally {
      setSaving(false);
    }
  }

  async function updateCourse(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!course || !canEdit) return;
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    try {
      await apiRequest(`/api/v1/courses/${course.id}`, {
        method: "PUT",
        body: JSON.stringify({
          code: course.code,
          name: String(data.get("name") ?? ""),
          description: String(data.get("description") ?? "") || null,
          objectives: String(data.get("objectives") ?? "") || null,
          targetAudience: String(data.get("targetAudience") ?? "") || null,
          durationMinutes: Number(data.get("durationMinutes") || 0),
          passingScore: Number(data.get("passingScore") || 70),
          completionPolicyJson:
            course.completionPolicyJson || '{"requiredLessonPercent":100}',
          categoryId: course.categoryId || null,
        }),
      });
      setToast({ message: "Đã lưu thông tin khóa học." });
      await load();
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể lưu khóa học",
      );
    } finally {
      setSaving(false);
    }
  }

  function openLessonModal(lesson: Lesson | null = null) {
    if (!canEdit) return;
    setFormError("");
    setEditingLesson(lesson);
    setLessonModal(true);
  }

  async function saveLesson(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!course || !canEdit) return;
    setSaving(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    const type = String(data.get("type") ?? "TEXT") as LessonType;
    try {
      let fileId: string | null = editingLesson?.fileId ?? null;
      const file = data.get("file");
      if (file instanceof File && file.size > 0) {
        const upload = new FormData();
        upload.set("file", file);
        const uploaded = await apiRequest<{ id: string }>(
          `/api/v1/files?purpose=COURSE_CONTENT`,
          { method: "POST", body: upload },
        );
        fileId = uploaded.id;
      }
      if (!fileRequired(type)) fileId = null;
      if (fileRequired(type) && !fileId)
        throw new Error("Vui lòng chọn tệp phù hợp cho loại bài học này.");

      const payload = {
        title: String(data.get("title") ?? ""),
        type,
        textContent: ["TEXT", "ASSIGNMENT", "EXAM"].includes(type)
          ? String(data.get("textContent") ?? "") || null
          : null,
        fileId,
        required: data.get("required") === "on",
        sortOrder:
          editingLesson?.sortOrder ??
          Math.max(
            0,
            ...(course.lessons ?? []).map((lesson) => lesson.sortOrder),
          ) + 1,
        estimatedMinutes: Number(data.get("estimatedMinutes") || 0),
      };
      const saved = editingLesson
        ? await apiRequest<Lesson>(
            `/api/v1/courses/${course.id}/lessons/${editingLesson.id}`,
            { method: "PUT", body: JSON.stringify(payload) },
          )
        : await apiRequest<Lesson>(`/api/v1/courses/${course.id}/lessons`, {
            method: "POST",
            body: JSON.stringify(payload),
          });

      setLessonModal(false);
      setEditingLesson(null);
      setToast({
        message: editingLesson
          ? "Đã cập nhật bài học."
          : "Đã thêm bài học vào khóa học.",
      });
      await load();
      setSelectedLessonId(saved.id);
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể lưu bài học",
      );
    } finally {
      setSaving(false);
    }
  }

  async function deleteLesson(lesson: Lesson) {
    if (!course || !canEdit) return;
    if (
      !window.confirm(
        `Xóa bài học “${lesson.title}”? Tiến độ liên quan có thể không còn được tính vào phiên bản mới của khóa học.`,
      )
    )
      return;
    setSaving(true);
    try {
      await apiRequest(`/api/v1/courses/${course.id}/lessons/${lesson.id}`, {
        method: "DELETE",
      });
      setToast({ message: "Đã xóa bài học khỏi khóa học." });
      setSelectedLessonId(null);
      await load();
    } catch (caught) {
      setToast({
        message:
          caught instanceof Error ? caught.message : "Không thể xóa bài học",
        tone: "error",
      });
    } finally {
      setSaving(false);
    }
  }

  async function archiveCourse() {
    if (!course || !canEdit) return;
    if (
      !window.confirm(
        "Xóa khóa học khỏi danh sách đang dùng? Hệ thống sẽ lưu trữ thay vì xóa vật lý để giữ nguyên tiến độ và điểm đã phát sinh.",
      )
    )
      return;
    setSaving(true);
    try {
      await apiRequest(`/api/v1/courses/${course.id}`, { method: "DELETE" });
      router.replace(instructorCoursePath());
      router.refresh();
    } catch (caught) {
      setToast({
        message:
          caught instanceof Error
            ? caught.message
            : "Không thể lưu trữ khóa học",
        tone: "error",
      });
      setSaving(false);
    }
  }

  if (loading)
    return <LoadingState label="Đang tải cấu trúc và nội dung khóa học..." />;
  if (error || !course)
    return (
      <ErrorState
        message={error || "Không tìm thấy khóa học"}
        onRetry={() => void load()}
      />
    );

  return (
    <>
      <PageHeader
        backHref={instructorCoursePath()}
        eyebrow={`${course.code} · PHIÊN BẢN ${course.contentVersion}`}
        title={course.name}
        description={course.description || "Khóa học chưa có mô tả."}
        actions={
          <>
            <StatusBadge value={course.status} />
            {canPublish && course.status === "DRAFT" && (
              <button
                className="button primary"
                onClick={() => void transition("PUBLISHED")}
                disabled={saving}
              >
                <Icon name="upload" />
                Xuất bản
              </button>
            )}
            {canPublish && course.status === "PUBLISHED" && (
              <button
                className="button secondary"
                onClick={() => void transition("HIDDEN")}
                disabled={saving}
              >
                <Icon name="lock" />
                Tạm ẩn
              </button>
            )}
            {canPublish && course.status === "HIDDEN" && (
              <button
                className="button primary"
                onClick={() => void transition("PUBLISHED")}
                disabled={saving}
              >
                <Icon name="unlock" />
                Hiển thị lại
              </button>
            )}
          </>
        }
      />

      <section className="course-detail-stats">
        <div>
          <Icon name="list" />
          <span>
            <strong>{course.lessons?.length ?? 0}</strong>Bài học
          </span>
        </div>
        <div>
          <Icon name="clock" />
          <span>
            <strong>{formatDuration(course.durationMinutes)}</strong>Thời lượng
          </span>
        </div>
        <div>
          <Icon name="grade" />
          <span>
            <strong>{course.passingScore}%</strong>Điểm đạt
          </span>
        </div>
        <div>
          <Icon name="calendar" />
          <span>
            <strong>{formatDate(course.publishedAt)}</strong>Ngày xuất bản
          </span>
        </div>
      </section>

      <div className="tabs">
        <button
          className={tab === "content" ? "active" : ""}
          onClick={() => setTab("content")}
        >
          Nội dung khóa học
        </button>
        <button
          className={tab === "assessments" ? "active" : ""}
          onClick={() => setTab("assessments")}
        >
          Bài kiểm tra
        </button>
        <button
          className={tab === "learners" ? "active" : ""}
          onClick={() => setTab("learners")}
        >
          Học viên
        </button>
        {user.permissions.includes("discussions:read") && (
          <button
            className={tab === "discussion" ? "active" : ""}
            onClick={() => setTab("discussion")}
          >
            Thảo luận
          </button>
        )}
        <button
          className={tab === "information" ? "active" : ""}
          onClick={() => setTab("information")}
        >
          Thông tin & cài đặt
        </button>
      </div>

      {tab === "content" ? (
        <section className="course-builder">
          <aside className="lesson-outline">
            <div className="lesson-outline-head">
              <div>
                <strong>Mục lục</strong>
                <small>{course.lessons?.length ?? 0} nội dung</small>
              </div>
              {canEdit && (
                <button
                  className="icon-button accent"
                  onClick={() => openLessonModal()}
                  aria-label="Thêm bài học"
                >
                  <Icon name="plus" />
                </button>
              )}
            </div>
            <div className="lesson-list">
              {course.lessons?.length ? (
                course.lessons.map((lesson, index) => (
                  <button
                    key={lesson.id}
                    className={`lesson-item ${lesson.id === selectedLessonId ? "active" : ""}`}
                    onClick={() => setSelectedLessonId(lesson.id)}
                  >
                    <span className="lesson-number">{index + 1}</span>
                    <span className="lesson-copy">
                      <strong>{lesson.title}</strong>
                      <small>
                        {lessonTypeLabels[lesson.type]} ·{" "}
                        {lesson.estimatedMinutes || 0} phút
                      </small>
                    </span>
                  </button>
                ))
              ) : (
                <div className="outline-empty">
                  <Icon name="book" />
                  <p>Chưa có bài học</p>
                </div>
              )}
            </div>
            {canEdit && (
              <button
                className="add-lesson-button"
                onClick={() => openLessonModal()}
              >
                <Icon name="plus" />
                Thêm bài học
              </button>
            )}
          </aside>

          <article className="lesson-preview">
            {selectedLesson ? (
              <>
                <div className="lesson-preview-head">
                  <div>
                    <span className="content-type">
                      {lessonTypeLabels[selectedLesson.type]}
                    </span>
                    <h2>{selectedLesson.title}</h2>
                    <p>
                      {selectedLesson.required
                        ? "Nội dung bắt buộc"
                        : "Nội dung tự chọn"}{" "}
                      · {selectedLesson.estimatedMinutes || 0} phút
                    </p>
                  </div>
                  {canEdit && (
                    <div className="page-actions">
                      <button
                        className="button secondary compact"
                        onClick={() => openLessonModal(selectedLesson)}
                      >
                        <Icon name="edit" />
                        Chỉnh sửa
                      </button>
                      <button
                        className="button danger compact"
                        disabled={saving}
                        onClick={() => void deleteLesson(selectedLesson)}
                      >
                        <Icon name="trash" />
                        Xóa
                      </button>
                    </div>
                  )}
                </div>
                <div className="lesson-content">
                  {selectedLesson.type === "TEXT" ? (
                    <div className="rich-text">
                      <p>
                        {selectedLesson.textContent ||
                          "Bài học chưa có nội dung văn bản."}
                      </p>
                    </div>
                  ) : selectedLesson.fileId ? (
                    <LessonResource
                      fileId={selectedLesson.fileId}
                      type={selectedLesson.type}
                      compact
                    />
                  ) : (
                    <EmptyState
                      title="Nội dung nghiệp vụ"
                      description={
                        selectedLesson.textContent ||
                        (selectedLesson.type === "ASSIGNMENT"
                          ? "Bài thực hành sẽ được học viên nộp trong luồng học tập."
                          : "Bài kiểm tra được quản lý ngay trong tab Bài kiểm tra của khóa học.")
                      }
                    />
                  )}
                </div>
              </>
            ) : (
              <EmptyState
                title="Bắt đầu xây dựng khóa học"
                description={
                  canEdit
                    ? "Thêm bài học đầu tiên. Sau khi có nội dung, bạn có thể xuất bản và giao khóa học cho học viên."
                    : "Khóa học chưa có nội dung được công bố."
                }
                action={
                  canEdit ? (
                    <button
                      className="button primary"
                      onClick={() => openLessonModal()}
                    >
                      <Icon name="plus" />
                      Thêm bài học
                    </button>
                  ) : undefined
                }
              />
            )}
          </article>
        </section>
      ) : tab === "assessments" ? (
        <CourseAssessmentsPanel course={course} />
      ) : tab === "learners" ? (
        <CourseLearnersPanel course={course} />
      ) : tab === "discussion" ? (
        <CourseDiscussion
          courseId={course.id}
          user={user}
          lessons={course.lessons ?? []}
        />
      ) : (
        <section className="settings-panel">
          <form className="form-stack" onSubmit={updateCourse}>
            <div className="section-title">
              <div>
                <h2>Thông tin khóa học</h2>
                <p>
                  {canEdit
                    ? "Các thay đổi được lưu trực tiếp vào Course Service."
                    : "Bạn đang xem khóa học ở chế độ chỉ đọc. Chỉ giảng viên sở hữu khóa học được chỉnh sửa."}
                </p>
              </div>
            </div>
            <div className="form-grid two">
              <label>
                Mã khóa học
                <input value={course.code} disabled />
              </label>
              <label>
                Thời lượng (phút)
                <input
                  name="durationMinutes"
                  type="number"
                  min="0"
                  defaultValue={course.durationMinutes ?? 0}
                  disabled={!canEdit}
                />
              </label>
            </div>
            <label>
              Tên khóa học <b>*</b>
              <input
                name="name"
                required
                defaultValue={course.name}
                disabled={!canEdit}
              />
            </label>
            <label>
              Mô tả
              <textarea
                name="description"
                rows={4}
                defaultValue={course.description ?? ""}
                disabled={!canEdit}
              />
            </label>
            <div className="form-grid two">
              <label>
                Đối tượng học
                <input
                  name="targetAudience"
                  defaultValue={course.targetAudience ?? ""}
                  disabled={!canEdit}
                />
              </label>
              <label>
                Điểm đạt (%)
                <input
                  name="passingScore"
                  type="number"
                  min="0"
                  max="100"
                  defaultValue={course.passingScore}
                  disabled={!canEdit}
                />
              </label>
            </div>
            <label>
              Mục tiêu khóa học
              <textarea
                name="objectives"
                rows={4}
                defaultValue={course.objectives ?? ""}
                disabled={!canEdit}
              />
            </label>
            {formError && (
              <div className="form-alert error">
                <Icon name="warning" />
                {formError}
              </div>
            )}
            {canEdit && (
              <div className="form-footer">
                <button className="button primary" disabled={saving}>
                  <Icon name="check" />
                  {saving ? "Đang lưu..." : "Lưu thay đổi"}
                </button>
              </div>
            )}
          </form>
          {canEdit && course.status !== "ARCHIVED" && (
            <div className="danger-zone">
              <div>
                <strong>Xóa khỏi danh sách đang dùng</strong>
                <p>
                  Đây là xóa an toàn: khóa học được chuyển sang lưu trữ, còn ghi
                  danh, tiến độ, điểm và lịch sử vẫn được giữ nguyên.
                </p>
              </div>
              <button
                className="button danger"
                disabled={saving}
                onClick={() => void archiveCourse()}
              >
                <Icon name="trash" />
                Xóa / lưu trữ
              </button>
            </div>
          )}
          {canPublish && course.status === "ARCHIVED" && (
            <div className="danger-zone">
              <div>
                <strong>Khôi phục khóa học</strong>
                <p>Xuất bản lại để khóa học quay về danh sách sử dụng.</p>
              </div>
              <button
                className="button primary"
                disabled={saving}
                onClick={() => void transition("PUBLISHED")}
              >
                <Icon name="unlock" />
                Khôi phục
              </button>
            </div>
          )}
        </section>
      )}

      <Modal
        open={lessonModal}
        onClose={() => {
          if (!saving) {
            setLessonModal(false);
            setEditingLesson(null);
          }
        }}
        title={editingLesson ? "Chỉnh sửa bài học" : "Thêm bài học"}
        description="Nội dung được lưu trực tiếp trong Course Service và File Storage Service."
      >
        <LessonForm
          key={editingLesson?.id ?? "new"}
          lesson={editingLesson}
          onSubmit={saveLesson}
          busy={saving}
          error={formError}
        />
      </Modal>
      {toast && (
        <Toast
          message={toast.message}
          tone={toast.tone}
          onClose={() => setToast(null)}
        />
      )}
    </>
  );
}

function LessonForm({
  lesson,
  onSubmit,
  busy,
  error,
}: {
  lesson: Lesson | null;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  error: string;
}) {
  const [type, setType] = useState<LessonType>(lesson?.type ?? "TEXT");
  const hasStoredFile = Boolean(lesson?.fileId && fileRequired(type));
  return (
    <form className="form-stack" onSubmit={onSubmit}>
      <label>
        Tên bài học <b>*</b>
        <input
          name="title"
          required
          maxLength={220}
          defaultValue={lesson?.title ?? ""}
          placeholder="VD: Tổng quan và mục tiêu"
        />
      </label>
      <div className="form-grid two">
        <label>
          Loại nội dung
          <select
            name="type"
            value={type}
            onChange={(event) => setType(event.target.value as LessonType)}
          >
            {Object.entries(lessonTypeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Thời lượng dự kiến (phút)
          <input
            name="estimatedMinutes"
            type="number"
            min="0"
            defaultValue={lesson?.estimatedMinutes ?? 10}
          />
        </label>
      </div>
      {["TEXT", "ASSIGNMENT", "EXAM"].includes(type) && (
        <label>
          {type === "TEXT" ? "Nội dung văn bản" : "Hướng dẫn hiển thị"}
          <textarea
            name="textContent"
            rows={type === "TEXT" ? 8 : 4}
            required={type === "TEXT"}
            defaultValue={
              ["TEXT", "ASSIGNMENT", "EXAM"].includes(lesson?.type ?? "")
                ? (lesson?.textContent ?? "")
                : ""
            }
            placeholder={
              type === "TEXT"
                ? "Nhập nội dung bài học..."
                : "Mô tả cách thực hiện hoặc tên bài kiểm tra liên quan..."
            }
          />
        </label>
      )}
      {fileRequired(type) && (
        <label>
          Tệp nội dung {hasStoredFile ? "" : <b>*</b>}
          <input
            name="file"
            type="file"
            required={!hasStoredFile}
            accept={
              type === "PDF"
                ? ".pdf"
                : type === "DOCX"
                  ? ".docx"
                  : type === "VIDEO"
                    ? ".mp4"
                    : type === "AUDIO"
                      ? ".mp3"
                      : ".pdf,.docx,.txt,.csv,.png,.jpg,.jpeg"
            }
          />
          <small>
            {hasStoredFile
              ? "Để trống để giữ tệp hiện tại, hoặc chọn tệp mới để thay thế."
              : "Tệp được kiểm tra định dạng, kích thước và lưu trong hệ thống nội bộ."}
          </small>
        </label>
      )}
      {["ASSIGNMENT", "EXAM"].includes(type) && (
        <div className="form-alert info">
          <Icon name="warning" />
          Loại nội dung này tạo điểm neo trong khóa học. Học viên thao tác trong
          luồng Bài thực hành hoặc Bài kiểm tra tương ứng.
        </div>
      )}
      <label className="check-row">
        <input
          name="required"
          type="checkbox"
          defaultChecked={lesson?.required ?? true}
        />
        <span>
          <strong>Nội dung bắt buộc</strong>
          <small>Tính vào điều kiện hoàn thành khóa học</small>
        </span>
      </label>
      {error && (
        <div className="form-alert error">
          <Icon name="warning" />
          {error}
        </div>
      )}
      <div className="modal-actions">
        <button type="submit" className="button primary" disabled={busy}>
          <Icon name={lesson ? "check" : "plus"} />
          {busy ? "Đang lưu..." : lesson ? "Lưu thay đổi" : "Thêm bài học"}
        </button>
      </div>
    </form>
  );
}

type DiscussionThread = {
  id: string;
  courseId: string;
  lessonId?: string;
  title: string;
  authorId: string;
  status: string;
  pinned: boolean;
  postCount: number;
  createdAt: string;
  updatedAt: string;
  posts?: DiscussionPost[];
};
type DiscussionPost = {
  id: string;
  authorId: string;
  parentPostId?: string;
  content: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

function CourseDiscussion({
  courseId,
  user,
  lessons,
}: {
  courseId: string;
  user: PortalUser;
  lessons: Lesson[];
}) {
  const canWrite = user.permissions.includes("discussions:write");
  const canModerate = user.permissions.includes("discussions:moderate");
  const [threads, setThreads] = useState<DiscussionThread[]>([]);
  const [active, setActive] = useState<DiscussionThread | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [working, setWorking] = useState(false);
  const [newThread, setNewThread] = useState({
    title: "",
    lessonId: "",
    content: "",
  });
  const [reply, setReply] = useState("");

  const loadThreads = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const values = await apiRequest<DiscussionThread[]>(
        `/api/v1/discussions/courses/${courseId}/threads`,
      );
      setThreads(values);
      if (active) {
        const refreshed = await apiRequest<DiscussionThread>(
          `/api/v1/discussions/threads/${active.id}`,
        );
        setActive(refreshed);
      } else if (values[0]) {
        setActive(
          await apiRequest<DiscussionThread>(
            `/api/v1/discussions/threads/${values[0].id}`,
          ),
        );
      }
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Không thể tải thảo luận",
      );
    } finally {
      setLoading(false);
    }
  }, [courseId, active?.id]);

  useEffect(() => {
    void loadThreads();
  }, [courseId]);

  async function openThread(id: string) {
    try {
      setActive(
        await apiRequest<DiscussionThread>(`/api/v1/discussions/threads/${id}`),
      );
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể mở chủ đề");
    }
  }

  async function createThread(event: React.FormEvent) {
    event.preventDefault();
    setWorking(true);
    try {
      const created = await apiRequest<DiscussionThread>(
        `/api/v1/discussions/courses/${courseId}/threads`,
        {
          method: "POST",
          body: JSON.stringify({
            title: newThread.title,
            lessonId: newThread.lessonId || null,
            content: newThread.content,
          }),
        },
      );
      setNewThread({ title: "", lessonId: "", content: "" });
      setActive(
        await apiRequest<DiscussionThread>(
          `/api/v1/discussions/threads/${created.id}`,
        ),
      );
      setThreads(
        await apiRequest<DiscussionThread[]>(
          `/api/v1/discussions/courses/${courseId}/threads`,
        ),
      );
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tạo chủ đề");
    } finally {
      setWorking(false);
    }
  }

  async function sendReply(event: React.FormEvent) {
    event.preventDefault();
    if (!active) return;
    setWorking(true);
    try {
      await apiRequest(`/api/v1/discussions/threads/${active.id}/posts`, {
        method: "POST",
        body: JSON.stringify({ content: reply }),
      });
      setReply("");
      await openThread(active.id);
      setThreads(
        await apiRequest<DiscussionThread[]>(
          `/api/v1/discussions/courses/${courseId}/threads`,
        ),
      );
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Không thể gửi phản hồi",
      );
    } finally {
      setWorking(false);
    }
  }

  async function moderate(
    status: "OPEN" | "LOCKED" | "HIDDEN",
    pinned?: boolean,
  ) {
    if (!active) return;
    try {
      await apiRequest(`/api/v1/discussions/threads/${active.id}`, {
        method: "PATCH",
        body: JSON.stringify({ status, pinned }),
      });
      await openThread(active.id);
      setThreads(
        await apiRequest<DiscussionThread[]>(
          `/api/v1/discussions/courses/${courseId}/threads`,
        ),
      );
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Không thể kiểm duyệt chủ đề",
      );
    }
  }

  if (loading && !threads.length)
    return <LoadingState label="Đang mở không gian thảo luận..." />;
  return (
    <section className="discussion-workspace">
      <aside className="discussion-sidebar">
        <header>
          <div>
            <strong>Chủ đề</strong>
            <small>{threads.length} cuộc trao đổi</small>
          </div>
          <button
            className="icon-button"
            onClick={() => void loadThreads()}
            aria-label="Làm mới"
          >
            <Icon name="refresh" />
          </button>
        </header>
        <div className="discussion-thread-list">
          {threads.map((thread) => (
            <button
              key={thread.id}
              className={active?.id === thread.id ? "active" : ""}
              onClick={() => void openThread(thread.id)}
            >
              <span>{thread.pinned ? "✦" : "◇"}</span>
              <div>
                <strong>{thread.title}</strong>
                <small>
                  {thread.postCount} phản hồi · {thread.status}
                </small>
              </div>
            </button>
          ))}
        </div>
        {canWrite && (
          <form className="discussion-create" onSubmit={createThread}>
            <h3>Mở chủ đề mới</h3>
            <input
              required
              minLength={3}
              maxLength={240}
              placeholder="Tiêu đề"
              value={newThread.title}
              onChange={(event) =>
                setNewThread({ ...newThread, title: event.target.value })
              }
            />
            <select
              value={newThread.lessonId}
              onChange={(event) =>
                setNewThread({ ...newThread, lessonId: event.target.value })
              }
            >
              <option value="">Toàn khóa học</option>
              {lessons.map((lesson) => (
                <option key={lesson.id} value={lesson.id}>
                  {lesson.title}
                </option>
              ))}
            </select>
            <textarea
              required
              maxLength={20000}
              placeholder="Nội dung trao đổi..."
              value={newThread.content}
              onChange={(event) =>
                setNewThread({ ...newThread, content: event.target.value })
              }
            />
            <button className="button primary" disabled={working}>
              Đăng chủ đề
            </button>
          </form>
        )}
      </aside>
      <article className="discussion-main">
        {error && (
          <div className="form-alert error">
            <Icon name="warning" />
            {error}
          </div>
        )}
        {active ? (
          <>
            <header>
              <div>
                <span className="content-type">
                  {active.pinned ? "Đã ghim" : "Thảo luận"}
                </span>
                <h2>{active.title}</h2>
                <p>
                  {active.postCount} bài viết · cập nhật{" "}
                  {formatDate(active.updatedAt)}
                </p>
              </div>
              {canModerate && (
                <div className="page-actions">
                  <button
                    className="button secondary compact"
                    onClick={() =>
                      void moderate(
                        active.status === "LOCKED" ? "OPEN" : "LOCKED",
                      )
                    }
                  >
                    {active.status === "LOCKED" ? "Mở khóa" : "Khóa"}
                  </button>
                  <button
                    className="button secondary compact"
                    onClick={() =>
                      void moderate(
                        active.status as "OPEN" | "LOCKED",
                        !active.pinned,
                      )
                    }
                  >
                    {active.pinned ? "Bỏ ghim" : "Ghim"}
                  </button>
                </div>
              )}
            </header>
            <div className="discussion-post-list">
              {active.posts?.map((post) => (
                <article key={post.id}>
                  <span className="avatar discussion-avatar">
                    {post.authorId.slice(0, 2).toUpperCase()}
                  </span>
                  <div>
                    <header>
                      <strong>Người dùng #{post.authorId.slice(0, 8)}</strong>
                      <time>{formatDate(post.createdAt)}</time>
                    </header>
                    <p>{post.content}</p>
                  </div>
                </article>
              ))}
            </div>
            {canWrite && active.status === "OPEN" && (
              <form className="discussion-reply" onSubmit={sendReply}>
                <textarea
                  required
                  maxLength={20000}
                  placeholder="Viết phản hồi..."
                  value={reply}
                  onChange={(event) => setReply(event.target.value)}
                />
                <button
                  className="button primary"
                  disabled={working || !reply.trim()}
                >
                  <Icon name="upload" />
                  Gửi phản hồi
                </button>
              </form>
            )}
          </>
        ) : (
          <EmptyState
            title="Chưa có chủ đề"
            description="Mở chủ đề đầu tiên để trao đổi về nội dung khóa học."
          />
        )}
      </article>
    </section>
  );
}
