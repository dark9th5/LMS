"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, createIdempotencyKey } from "@/lib/api";
import type {
  AssignmentSubmission,
  Course,
  CourseProgress,
  Exam,
  Lesson,
  StoredFile,
} from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { studentCoursePath, studentCourseQuizPath } from "@/lib/portal-paths";
import { Icon } from "./Icon";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { ProgressBar } from "./ProgressBar";
import { LessonResource } from "./LessonResource";

const typeLabel: Record<string, string> = {
  TEXT: "Bài đọc",
  PDF: "PDF",
  VIDEO: "Video",
  AUDIO: "Âm thanh",
  FILE: "Tài liệu",
  ASSIGNMENT: "Bài thực hành",
  EXAM: "Bài kiểm tra",
};

function recordLearningStatement(input: Record<string, unknown>): void {
  void apiRequest("/api/v1/xapi/statements", {
    method: "POST",
    body: JSON.stringify({
      id: crypto.randomUUID(),
      source: "WEB",
      timestamp: new Date().toISOString(),
      ...input,
    }),
  }).catch(() => undefined);
}

export function LearningPlayer({
  enrollmentId,
  user,
}: {
  enrollmentId: string;
  user: PortalUser;
}) {
  const [course, setCourse] = useState<Course | null>(null);
  const [progress, setProgress] = useState<CourseProgress | null>(null);
  const [courseExams, setCourseExams] = useState<Exam[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [outlineOpen, setOutlineOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [assignmentFile, setAssignmentFile] = useState<File | null>(null);
  const [assignmentAttempts, setAssignmentAttempts] = useState<
    AssignmentSubmission[]
  >([]);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const openedAt = useRef(Date.now());
  const lastStatementLesson = useRef<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const detail = await apiRequest<CourseProgress>(
        `/api/v1/learning/${enrollmentId}`,
      );
      const [value, exams] = await Promise.all([
        apiRequest<Course>(
          `/api/v1/courses/${detail.courseId}/versions/${detail.courseVersion}`,
        ),
        apiRequest<Exam[]>("/api/v1/exams"),
      ]);
      const sorted = {
        ...value,
        lessons: [...(value.lessons ?? [])].sort(
          (a, b) => a.sortOrder - b.sortOrder,
        ),
      };
      setProgress(detail);
      setCourse(sorted);
      setCourseExams(exams.filter((exam) => exam.courseId === detail.courseId));
      setSelectedId((current) =>
        current && sorted.lessons?.some((item) => item.id === current)
          ? current
          : (detail.lastLessonId ?? sorted.lessons?.[0]?.id ?? null),
      );
      openedAt.current = Date.now();
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể mở khóa học",
      );
    } finally {
      setLoading(false);
    }
  }, [enrollmentId]);
  useEffect(() => {
    void load();
  }, [load]);

  const selected = useMemo(
    () => course?.lessons?.find((lesson) => lesson.id === selectedId) ?? null,
    [course, selectedId],
  );
  const selectedExam = useMemo(
    () =>
      selected?.type === "EXAM"
        ? (courseExams.find(
            (exam) => exam.lessonId === selected.id && exam.status === "ACTIVE",
          ) ?? null)
        : null,
    [courseExams, selected],
  );
  useEffect(() => {
    if (
      !course ||
      !progress ||
      !selected ||
      lastStatementLesson.current === selected.id
    )
      return;
    lastStatementLesson.current = selected.id;
    openedAt.current = Date.now();
    recordLearningStatement({
      verb: "experienced",
      objectId: `urn:lmspilot:lesson:${selected.id}`,
      objectType: "LESSON",
      courseId: course.id,
      lessonId: selected.id,
      enrollmentId: progress.enrollmentId,
      context: {
        courseVersion: progress.courseVersion,
        lessonType: selected.type,
        required: selected.required,
      },
    });
  }, [course, progress, selected]);
  useEffect(() => {
    if (!selected || selected.type !== "ASSIGNMENT" || !progress) {
      setAssignmentAttempts([]);
      return;
    }
    void apiRequest<AssignmentSubmission[]>(
      `/api/v1/learning/assignments/${selected.id}/attempts?enrollmentId=${progress.enrollmentId}`,
    )
      .then(setAssignmentAttempts)
      .catch((caught) => {
        setAssignmentAttempts([]);
        setToast(
          caught instanceof Error
            ? caught.message
            : "Không thể tải lịch sử nộp bài",
        );
      });
  }, [selected, progress]);
  const completedIds = useMemo(
    () =>
      new Set(
        progress?.lessons
          ?.filter((item) => item.completed)
          .map((item) => item.lessonId) ?? [],
      ),
    [progress],
  );
  const index =
    course?.lessons?.findIndex((item) => item.id === selectedId) ?? -1;
  const previous = index > 0 ? course?.lessons?.[index - 1] : null;
  const next =
    index >= 0 && index < (course?.lessons?.length ?? 0) - 1
      ? course?.lessons?.[index + 1]
      : null;

  async function updateLesson(
    lesson: Lesson,
    completed: boolean,
    moveNext = false,
    position?: string,
  ) {
    if (!course || !progress) return;
    setUpdating(true);
    try {
      const elapsed = Math.max(
        1,
        Math.min(3600, Math.round((Date.now() - openedAt.current) / 1000)),
      );
      const updated = await apiRequest<CourseProgress>(
        "/api/v1/learning/progress",
        {
          method: "PUT",
          headers: { "Idempotency-Key": createIdempotencyKey() },
          body: JSON.stringify({
            enrollmentId,
            courseId: course.id,
            lessonId: lesson.id,
            completed,
            learningSecondsDelta: elapsed,
            position: position ?? (completed ? "completed" : "opened"),
          }),
        },
      );
      setProgress(updated);
      openedAt.current = Date.now();
      recordLearningStatement({
        verb: completed ? "completed" : "progressed",
        objectId: `urn:lmspilot:lesson:${lesson.id}`,
        objectType: "LESSON",
        courseId: course.id,
        lessonId: lesson.id,
        enrollmentId,
        completion: completed,
        durationSeconds: elapsed,
        context: {
          courseVersion: progress.courseVersion,
          progressPercent: updated.progressPercent,
          position: position ?? null,
        },
      });
      if (updated.status === "COMPLETED" && progress.status !== "COMPLETED") {
        recordLearningStatement({
          verb: "completed",
          objectId: `urn:lmspilot:course:${course.id}:version:${progress.courseVersion}`,
          objectType: "COURSE",
          courseId: course.id,
          enrollmentId,
          completion: true,
          success: true,
          context: {
            courseVersion: progress.courseVersion,
            progressPercent: updated.progressPercent,
          },
        });
      }
      setToast(
        completed
          ? "Đã hoàn thành bài học và lưu tiến độ."
          : "Đã cập nhật tiến độ bài học.",
      );
      if (moveNext && next) {
        setSelectedId(next.id);
        setOutlineOpen(false);
      }
    } catch (caught) {
      setToast(
        caught instanceof Error ? caught.message : "Không thể lưu tiến độ",
      );
    } finally {
      setUpdating(false);
    }
  }

  async function submitAssignment(lesson: Lesson) {
    if (!assignmentFile) {
      setToast("Vui lòng chọn tệp bài làm trước khi nộp.");
      return;
    }
    setUpdating(true);
    try {
      const form = new FormData();
      form.append("file", assignmentFile);
      const stored = await apiRequest<StoredFile>(
        `/api/v1/files?purpose=ASSIGNMENT_SUBMISSION`,
        { method: "POST", body: form },
      );
      const submission = await apiRequest<AssignmentSubmission>(
        `/api/v1/learning/assignments/${lesson.id}/submissions`,
        {
          method: "POST",
          headers: { "Idempotency-Key": createIdempotencyKey("assignment") },
          body: JSON.stringify({ enrollmentId, fileId: stored.id }),
        },
      );
      setAssignmentAttempts((current) => [
        submission,
        ...current.filter((item) => item.id !== submission.id),
      ]);
      setProgress(
        await apiRequest<CourseProgress>(`/api/v1/learning/${enrollmentId}`),
      );
      setAssignmentFile(null);
      setToast(`Đã ghi nhận lần nộp số ${submission.attemptNumber}.`);
    } catch (caught) {
      setToast(
        caught instanceof Error
          ? caught.message
          : "Không thể nộp bài thực hành",
      );
    } finally {
      setUpdating(false);
    }
  }

  if (loading)
    return <LoadingState label="Đang mở nội dung và tiến độ học tập..." />;
  if (error || !course || !progress)
    return (
      <ErrorState
        message={error || "Không tìm thấy khóa học"}
        onRetry={() => void load()}
      />
    );

  return (
    <div className="learning-player">
      <header className="player-header">
        <div className="player-title">
          <Link className="icon-button" href={studentCoursePath()} aria-label="Quay lại">
            <Icon name="back" />
          </Link>
          <div>
            <small>{course.code}</small>
            <strong>{course.name}</strong>
          </div>
        </div>
        <div className="player-progress">
          <ProgressBar value={progress.progressPercent} />
          <span>{progress.progressPercent}% hoàn thành</span>
        </div>
        <button
          className="button secondary compact player-outline-toggle"
          onClick={() => setOutlineOpen((value) => !value)}
        >
          <Icon name="list" />
          Mục lục
        </button>
      </header>
      <div className="player-layout">
        {outlineOpen && (
          <button
            className="player-overlay"
            onClick={() => setOutlineOpen(false)}
            aria-label="Đóng mục lục"
          />
        )}
        <aside className={`player-outline ${outlineOpen ? "open" : ""}`}>
          <div className="player-outline-header">
            <div>
              <strong>Nội dung khóa học</strong>
              <small>
                {completedIds.size}/{course.lessons?.length ?? 0} bài đã hoàn
                thành
              </small>
            </div>
            <button
              className="icon-button outline-close"
              onClick={() => setOutlineOpen(false)}
            >
              <Icon name="close" />
            </button>
          </div>
          <div className="player-lesson-list">
            {course.lessons?.map((lesson, lessonIndex) => (
              <button
                key={lesson.id}
                className={`player-lesson ${lesson.id === selectedId ? "active" : ""}`}
                onClick={() => {
                  setSelectedId(lesson.id);
                  setOutlineOpen(false);
                  openedAt.current = Date.now();
                }}
              >
                <span
                  className={`completion-dot ${completedIds.has(lesson.id) ? "done" : ""}`}
                >
                  {completedIds.has(lesson.id) ? (
                    <Icon name="check" size={14} />
                  ) : (
                    lessonIndex + 1
                  )}
                </span>
                <span>
                  <strong>{lesson.title}</strong>
                  <small>
                    {typeLabel[lesson.type] ?? lesson.type} ·{" "}
                    {lesson.estimatedMinutes || 0} phút
                  </small>
                </span>
              </button>
            ))}
          </div>
          <div className="outline-progress">
            <ProgressBar
              value={progress.progressPercent}
              label="Tiến độ khóa học"
            />
          </div>
        </aside>
        <main className="player-content">
          {selected ? (
            <article className="learning-lesson">
              <header>
                <span className="content-type">
                  {typeLabel[selected.type] ?? selected.type}
                </span>
                <h1>{selected.title}</h1>
                <p>
                  {selected.required ? "Nội dung bắt buộc" : "Nội dung tự chọn"}{" "}
                  · {selected.estimatedMinutes || 0} phút
                </p>
              </header>
              <section className="lesson-body">
                {selected.type === "TEXT" ? (
                  <div className="learning-rich-text">
                    <p>{selected.textContent || "Bài học chưa có nội dung."}</p>
                  </div>
                ) : selected.type === "EXAM" ? (
                  <div className="learning-callout">
                    <Icon name="exam" size={36} />
                    <div>
                      <h2>Bài kiểm tra của khóa học</h2>
                      {selectedExam ? (
                        <>
                          <p>
                            Phiên làm bài được gắn với đúng khóa học này; kết quả
                            đạt mới tự động hoàn thành bài học.
                          </p>
                          <Link
                            className="button primary"
                            href={studentCourseQuizPath(progress.enrollmentId, selectedExam.id)}
                          >
                            Mở bài kiểm tra
                          </Link>
                        </>
                      ) : (
                        <>
                          <p>
                            Chưa có bài kiểm tra đang hoạt động gắn với bài học
                            này.
                          </p>
                          <p className="form-alert info">Giảng viên cần xuất bản đề trong tab Bài kiểm tra của khóa học.</p>
                        </>
                      )}
                    </div>
                  </div>
                ) : selected.type === "ASSIGNMENT" ? (
                  <>
                    {selected.fileId && (
                      <LessonResource fileId={selected.fileId} type="FILE" />
                    )}
                    <AssignmentSubmissionPanel
                      lesson={selected}
                      attempts={assignmentAttempts}
                      file={assignmentFile}
                      busy={updating}
                      onFile={setAssignmentFile}
                      onSubmit={() => void submitAssignment(selected)}
                    />
                  </>
                ) : selected.fileId ? (
                  <LessonResource
                    fileId={selected.fileId}
                    type={selected.type}
                  />
                ) : (
                  <EmptyState
                    title="Nội dung chưa sẵn sàng"
                    description="Giảng viên cần bổ sung tài nguyên cho bài học này."
                  />
                )}
              </section>
              <footer className="lesson-navigation">
                <button
                  className="button secondary"
                  disabled={!previous}
                  onClick={() => previous && setSelectedId(previous.id)}
                >
                  <Icon name="back" />
                  Bài trước
                </button>
                <div>
                  {selected.type === "ASSIGNMENT" ||
                  selected.type === "EXAM" ? (
                    <>
                      {completedIds.has(selected.id) ? (
                        next ? (
                          <button
                            className="button primary"
                            onClick={() => setSelectedId(next.id)}
                          >
                            Bài tiếp theo
                            <Icon name="arrow" />
                          </button>
                        ) : (
                          <span className="content-type">
                            Đã xác minh hoàn thành
                          </span>
                        )
                      ) : (
                        <button
                          className="button secondary"
                          disabled={updating}
                          onClick={() =>
                            void apiRequest<CourseProgress>(
                              `/api/v1/learning/${enrollmentId}`,
                            )
                              .then(setProgress)
                              .catch((caught) =>
                                setToast(
                                  caught instanceof Error
                                    ? caught.message
                                    : "Không thể làm mới kết quả",
                                ),
                              )
                          }
                        >
                          <Icon name="refresh" />
                          Làm mới kết quả
                        </button>
                      )}
                    </>
                  ) : completedIds.has(selected.id) ? (
                    <button
                      className="button secondary"
                      disabled={updating}
                      onClick={() => void updateLesson(selected, false)}
                    >
                      <Icon name="refresh" />
                      Đánh dấu học lại
                    </button>
                  ) : (
                    <button
                      className="button primary"
                      disabled={updating}
                      onClick={() => void updateLesson(selected, true, true)}
                    >
                      <Icon name="check" />
                      {updating
                        ? "Đang lưu..."
                        : next
                          ? "Hoàn thành & tiếp tục"
                          : "Hoàn thành khóa học"}
                    </button>
                  )}
                </div>
              </footer>
            </article>
          ) : (
            <EmptyState
              title="Khóa học chưa có bài học"
              description="Vui lòng liên hệ giảng viên phụ trách."
            />
          )}
        </main>
      </div>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </div>
  );
}
function AssignmentSubmissionPanel({
  lesson,
  attempts,
  file,
  busy,
  onFile,
  onSubmit,
}: {
  lesson: Lesson;
  attempts: AssignmentSubmission[];
  file: File | null;
  busy: boolean;
  onFile: (file: File | null) => void;
  onSubmit: () => void;
}) {
  const latest = attempts[0];
  const canSubmit = !latest || latest.status === "RETURNED";
  return (
    <div className="assignment-box">
      <div className="assignment-head">
        <span>
          <Icon name="upload" size={30} />
        </span>
        <div>
          <h2>{lesson.title}</h2>
          <p>
            Tệp bài làm được lưu riêng; bài học chỉ hoàn thành khi giảng viên
            chấm và chấp nhận lần nộp.
          </p>
        </div>
      </div>
      {latest && (
        <div className="assignment-submitted">
          <Icon name={latest.status === "GRADED" ? "grade" : "check"} />
          <div>
            <strong>
              Lần nộp #{latest.attemptNumber} ·{" "}
              {latest.status === "GRADED"
                ? "Đã chấm"
                : latest.status === "RETURNED"
                  ? "Cần chỉnh sửa"
                  : "Đã nộp"}
            </strong>
            <span>
              {latest.late ? "Nộp sau hạn" : "Đúng hạn"}
              {latest.score != null
                ? ` · ${latest.score}/${latest.maxScore}`
                : ""}
            </span>
            {latest.feedback && <small>{latest.feedback}</small>}
          </div>
          <a
            className="button secondary compact"
            href={`/api/gateway/api/v1/files/${latest.fileId}/content`}
          >
            <Icon name="download" />
            Tải lại
          </a>
        </div>
      )}
      {canSubmit && (
        <>
          <label className="assignment-drop">
            <input
              type="file"
              onChange={(event) => onFile(event.target.files?.[0] ?? null)}
            />
            <Icon name="file" size={28} />
            <span>
              <strong>
                {file
                  ? file.name
                  : latest
                    ? "Chọn tệp để nộp phiên bản chỉnh sửa"
                    : "Chọn tệp bài làm"}
              </strong>
              <small>
                File Storage kiểm tra định dạng, dung lượng và giữ metadata tách
                biệt.
              </small>
            </span>
          </label>
          <button
            className="button primary"
            disabled={!file || busy}
            onClick={onSubmit}
          >
            {busy
              ? "Đang tải và lưu..."
              : latest
                ? "Nộp lại bài đã chỉnh sửa"
                : "Nộp bài thực hành"}
          </button>
        </>
      )}
      {attempts.length > 1 && (
        <details className="assignment-history">
          <summary>Lịch sử {attempts.length} lần nộp</summary>
          {attempts.map((item) => (
            <div key={item.id}>
              <span>#{item.attemptNumber}</span>
              <span>
                {new Intl.DateTimeFormat("vi-VN", {
                  dateStyle: "short",
                  timeStyle: "short",
                }).format(new Date(item.submittedAt))}
              </span>
              <span>{item.status}</span>
            </div>
          ))}
        </details>
      )}
    </div>
  );
}
