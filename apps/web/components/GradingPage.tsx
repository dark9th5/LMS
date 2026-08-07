"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { AssignmentSubmission, Course, Exam, Grade } from "@/lib/models";
import { formatDate } from "@/lib/models";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Icon } from "./Icon";
import { Modal } from "./Modal";
import { PageHeader } from "./PageHeader";
import { StatusBadge } from "./StatusBadge";

type StudentDirectoryItem = {
  id: string;
  fullName: string;
  email?: string | null;
};

export function GradingPage() {
  const [grades, setGrades] = useState<Grade[]>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [students, setStudents] = useState<StudentDirectoryItem[]>([]);
  const [assignments, setAssignments] = useState<AssignmentSubmission[]>([]);
  const [selected, setSelected] = useState<Grade | null>(null);
  const [selectedAssignment, setSelectedAssignment] =
    useState<AssignmentSubmission | null>(null);
  const [view, setView] = useState<"EXAMS" | "ASSIGNMENTS">("EXAMS");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [toast, setToast] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [queue, examPayload, coursePayload, studentPayload] =
        await Promise.all([
          apiRequest<unknown>("/api/v1/grades/queue"),
          apiRequest<unknown>("/api/v1/exams"),
          apiRequest<unknown>("/api/v1/courses?size=100"),
          apiRequest<StudentDirectoryItem[]>("/api/v1/directory/students"),
        ]);
      const courseItems = unwrapItems<Course>(coursePayload as never).slice(
        0,
        100,
      );
      setGrades(unwrapItems<Grade>(queue as never));
      setExams(unwrapItems<Exam>(examPayload as never));
      setCourses(courseItems);
      setStudents(studentPayload);
      if (courseItems.length) {
        const params = new URLSearchParams();
        courseItems.forEach((item) => params.append("courseId", item.id));
        setAssignments(
          await apiRequest<AssignmentSubmission[]>(
            `/api/v1/learning/assignments/queue-by-course?${params.toString()}`,
          ),
        );
      } else {
        setAssignments([]);
      }
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Không thể tải hàng chờ chấm",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const examMap = useMemo(
    () => new Map(exams.map((item) => [item.id, item])),
    [exams],
  );
  const studentMap = useMemo(
    () => new Map(students.map((item) => [item.id, item])),
    [students],
  );
  const courseMap = useMemo(
    () => new Map(courses.map((item) => [item.id, item])),
    [courses],
  );
  const normalizedQuery = query.trim().toLowerCase();
  const filtered = grades.filter((grade) =>
    `${examMap.get(grade.examId)?.title ?? ""} ${studentMap.get(grade.userId)?.fullName ?? grade.userId}`
      .toLowerCase()
      .includes(normalizedQuery),
  );
  const filteredAssignments = assignments.filter((item) =>
    `${courseMap.get(item.courseId)?.name ?? ""} ${studentMap.get(item.userId)?.fullName ?? item.userId} ${item.lessonId}`
      .toLowerCase()
      .includes(normalizedQuery),
  );

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setFormError("");
    try {
      await apiRequest(`/api/v1/grades/${selected.id}`, {
        method: "PUT",
        body: JSON.stringify({
          score: Number(data.get("score")),
          feedback: String(data.get("feedback") ?? "") || null,
        }),
      });
      setSelected(null);
      setToast("Đã lưu điểm và hoàn tất chấm bài.");
      await load();
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể lưu điểm",
      );
    } finally {
      setBusy(false);
    }
  }

  async function submitAssignmentGrade(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();
    if (!selectedAssignment) return;
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setFormError("");
    try {
      await apiRequest(
        `/api/v1/learning/assignments/submissions/${selectedAssignment.id}/grade`,
        {
          method: "PUT",
          body: JSON.stringify({
            score: Number(data.get("score")),
            maxScore: Number(data.get("maxScore")),
            feedback: String(data.get("feedback") ?? "") || null,
            returnForRevision: data.get("returnForRevision") === "on",
          }),
        },
      );
      setSelectedAssignment(null);
      setToast("Đã lưu kết quả bài thực hành và cập nhật tiến độ học viên.");
      await load();
    } catch (caught) {
      setFormError(
        caught instanceof Error
          ? caught.message
          : "Không thể chấm bài thực hành",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="ĐÁNH GIÁ THỦ CÔNG"
        title="Hàng chờ chấm điểm"
        description="Chấm câu tự luận và bài thực hành trong các khóa học do bạn phụ trách."
        icon="grade"
      />
      <section className="toolbar-card">
        <div className="segmented-control">
          <button
            className={view === "EXAMS" ? "active" : ""}
            onClick={() => setView("EXAMS")}
          >
            Bài kiểm tra & bài thi ({grades.length})
          </button>
          <button
            className={view === "ASSIGNMENTS" ? "active" : ""}
            onClick={() => setView("ASSIGNMENTS")}
          >
            Bài thực hành ({assignments.length})
          </button>
        </div>
        <label className="search-field">
          <Icon name="search" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm theo bài, khóa học hoặc học viên"
          />
        </label>
        <span className="result-count">
          {view === "EXAMS" ? filtered.length : filteredAssignments.length} bài
          đang chờ
        </span>
        <button className="button secondary" onClick={() => void load()}>
          <Icon name="refresh" />
          Làm mới
        </button>
      </section>

      {loading ? (
        <LoadingState />
      ) : error ? (
        <ErrorState message={error} onRetry={() => void load()} />
      ) : view === "EXAMS" ? (
        filtered.length === 0 ? (
          <EmptyState
            title="Không còn bài kiểm tra hoặc bài thi chờ chấm"
            description="Các câu tự luận mới sẽ xuất hiện tại đây sau khi học viên nộp bài."
          />
        ) : (
          <section className="grading-list">
            {filtered.map((grade) => {
              const exam = examMap.get(grade.examId);
              const learner = studentMap.get(grade.userId);
              return (
                <article className="grading-card" key={grade.id}>
                  <div className="grading-card-main">
                    <span className="list-icon">
                      <Icon name="grade" />
                    </span>
                    <div>
                      <div className="grading-heading">
                        <h2>{exam?.title ?? "Bài đánh giá"}</h2>
                        <StatusBadge value={grade.status} />
                      </div>
                      <p>
                        {learner?.fullName ??
                          `Học viên ${grade.userId.slice(0, 8)}`}{" "}
                        · Nộp bài cần chấm thủ công
                      </p>
                      <div className="grading-meta">
                        <span>
                          {
                            grade.details.filter((item) => item.requiresManual)
                              .length
                          }{" "}
                          câu cần chấm
                        </span>
                        <span>
                          Điểm tự động: {grade.score}/{grade.maxScore}
                        </span>
                        <span>
                          Cập nhật {formatDate(grade.updatedAt, true)}
                        </span>
                      </div>
                    </div>
                  </div>
                  <button
                    className="button primary"
                    onClick={() => {
                      setFormError("");
                      setSelected(grade);
                    }}
                  >
                    <Icon name="edit" />
                    Chấm bài
                  </button>
                </article>
              );
            })}
          </section>
        )
      ) : filteredAssignments.length === 0 ? (
        <EmptyState
          title="Không còn bài thực hành chờ chấm"
          description="Bài nộp mới từ các khóa học do bạn phụ trách sẽ xuất hiện tại đây."
        />
      ) : (
        <section className="grading-list">
          {filteredAssignments.map((item) => {
            const learner = studentMap.get(item.userId);
            const course = courseMap.get(item.courseId);
            return (
              <article className="grading-card" key={item.id}>
                <div className="grading-card-main">
                  <span className="list-icon">
                    <Icon name="file" />
                  </span>
                  <div>
                    <div className="grading-heading">
                      <h2>{course?.name ?? "Bài thực hành"}</h2>
                      <StatusBadge value={item.status} />
                    </div>
                    <p>
                      {learner?.fullName ??
                        `Học viên ${item.userId.slice(0, 8)}`}{" "}
                      · Lần nộp #{item.attemptNumber}
                      {item.late ? " · nộp muộn" : ""}
                    </p>
                    <div className="grading-meta">
                      <span>Bài học {item.lessonId.slice(0, 8)}</span>
                      <span>Nộp {formatDate(item.submittedAt, true)}</span>
                      <a
                        href={`/api/gateway/api/v1/files/${item.fileId}/content`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Tải bài làm
                      </a>
                    </div>
                  </div>
                </div>
                <button
                  className="button primary"
                  onClick={() => {
                    setFormError("");
                    setSelectedAssignment(item);
                  }}
                >
                  <Icon name="edit" />
                  Chấm bài
                </button>
              </article>
            );
          })}
        </section>
      )}

      <Modal
        open={Boolean(selected)}
        onClose={() => !busy && setSelected(null)}
        title="Chấm bài thủ công"
        description={
          selected
            ? `${examMap.get(selected.examId)?.title ?? "Bài đánh giá"} · ${studentMap.get(selected.userId)?.fullName ?? "Học viên"}`
            : undefined
        }
        wide
      >
        {selected && (
          <form className="form-stack" onSubmit={submit}>
            <section className="grade-summary">
              <div>
                <span>Điểm tự động</span>
                <strong>{selected.score}</strong>
              </div>
              <div>
                <span>Điểm tối đa</span>
                <strong>{selected.maxScore}</strong>
              </div>
              <div>
                <span>Câu cần chấm</span>
                <strong>
                  {
                    selected.details.filter((item) => item.requiresManual)
                      .length
                  }
                </strong>
              </div>
            </section>
            <div className="manual-question-list">
              {selected.details
                .filter((item) => item.requiresManual)
                .map((item, index) => (
                  <div key={item.questionId}>
                    <span>
                      <strong>
                        Câu {index + 1}: {item.prompt || item.type}
                      </strong>
                      <small>
                        Câu trả lời:{" "}
                        {Array.isArray(item.answer)
                          ? item.answer.join(", ")
                          : String(item.answer ?? "Chưa trả lời")}
                      </small>
                    </span>
                    <strong>Tối đa {item.maximum} điểm</strong>
                  </div>
                ))}
            </div>
            <label>
              Tổng điểm sau chấm <b>*</b>
              <input
                name="score"
                type="number"
                min="0"
                max={selected.maxScore}
                step="0.01"
                defaultValue={selected.score}
                required
              />
            </label>
            <label>
              Nhận xét cho học viên
              <textarea
                name="feedback"
                rows={5}
                placeholder="Nêu điểm tốt và nội dung cần cải thiện..."
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
                onClick={() => setSelected(null)}
              >
                Hủy
              </button>
              <button className="button primary" disabled={busy}>
                {busy ? "Đang lưu..." : "Hoàn tất chấm điểm"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Modal
        open={Boolean(selectedAssignment)}
        onClose={() => !busy && setSelectedAssignment(null)}
        title="Chấm bài thực hành"
        description={
          selectedAssignment
            ? `${courseMap.get(selectedAssignment.courseId)?.name ?? "Khóa học"} · ${studentMap.get(selectedAssignment.userId)?.fullName ?? "Học viên"}`
            : undefined
        }
      >
        {selectedAssignment && (
          <form className="form-stack" onSubmit={submitAssignmentGrade}>
            <a
              className="button secondary"
              href={`/api/gateway/api/v1/files/${selectedAssignment.fileId}/content`}
              target="_blank"
              rel="noreferrer"
            >
              <Icon name="download" />
              Mở tệp bài làm
            </a>
            <div className="form-grid two">
              <label>
                Điểm <b>*</b>
                <input
                  name="score"
                  type="number"
                  min="0"
                  step="0.01"
                  required
                />
              </label>
              <label>
                Điểm tối đa <b>*</b>
                <input
                  name="maxScore"
                  type="number"
                  min="0.01"
                  step="0.01"
                  defaultValue="10"
                  required
                />
              </label>
            </div>
            <label>
              Nhận xét
              <textarea
                name="feedback"
                rows={5}
                placeholder="Nêu điểm tốt và nội dung cần cải thiện..."
              />
            </label>
            <label className="check-row">
              <input type="checkbox" name="returnForRevision" />
              <span>
                <strong>Trả lại để học viên chỉnh sửa</strong>
                <small>
                  Bài học sẽ chưa hoàn thành cho đến khi có lần nộp hợp lệ tiếp
                  theo.
                </small>
              </span>
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
                onClick={() => setSelectedAssignment(null)}
              >
                Hủy
              </button>
              <button className="button primary" disabled={busy}>
                {busy ? "Đang lưu..." : "Lưu kết quả"}
              </button>
            </div>
          </form>
        )}
      </Modal>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </>
  );
}
