"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, createIdempotencyKey } from "@/lib/api";
import type {
  Course,
  Exam,
  ExamQuestion,
  ExamSession,
  Grade,
  Question,
} from "@/lib/models";
import { formatDate, formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { resolvePortalRole } from "@/lib/role";
import { instructorCoursePath, standaloneExamPath, studentCoursePath } from "@/lib/portal-paths";
import { EmptyState, ErrorState, LoadingState, Toast } from "./Feedback";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { ProgressBar } from "./ProgressBar";
import { Modal } from "./Modal";
import { StatusBadge } from "./StatusBadge";

function secondsLeft(expiresAt: string): number {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 1000));
}
function serverRemaining(session: ExamSession): number {
  return Number.isFinite(session.remainingSeconds)
    ? Math.max(0, Math.floor(session.remainingSeconds))
    : secondsLeft(session.expiresAt);
}
function validAttempt(session: ExamSession, examId: string): boolean {
  return (
    session.examId === examId &&
    session.status === "IN_PROGRESS" &&
    Array.isArray(session.questions) &&
    session.questions.length > 0 &&
    serverRemaining(session) > 0
  );
}
function timerLabel(value: number): string {
  const hours = Math.floor(value / 3600);
  const minutes = Math.floor((value % 3600) / 60);
  const seconds = value % 60;
  return [hours, minutes, seconds]
    .filter((_, index) => index > 0 || hours > 0)
    .map((part) => String(part).padStart(2, "0"))
    .join(":");
}
function answerCount(answers?: Record<string, unknown> | null): number {
  if (!answers) return 0;
  return Object.values(answers).filter((answer) =>
    Array.isArray(answer)
      ? answer.length > 0
      : String(answer ?? "").trim().length > 0,
  ).length;
}
function localDateTime(value?: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}
function sessionStorageKey(
  examId: string,
  enrollmentId?: string | null,
): string {
  return `lmspilot:exam-session:${examId}:${enrollmentId ?? "standalone"}`;
}

type AssessmentAssignment = {
  id: string;
  assessmentId: string;
  assigneeType: "USER" | "GROUP" | "DEPARTMENT" | "BRANCH";
  assigneeId: string;
  availableFrom?: string | null;
  dueAt?: string | null;
  required: boolean;
  status: "ACTIVE" | "REVOKED";
  assignedAt: string;
};

function unwrapQuestionBank(payload: unknown): Question[] {
  if (Array.isArray(payload)) return payload as Question[];
  if (payload && typeof payload === "object" && Array.isArray((payload as { items?: unknown[] }).items)) {
    return (payload as { items: Question[] }).items;
  }
  return [];
}

export function ExamDetail({
  examId,
  user,
  standaloneOnly = false,
  enrollmentIdOverride,
}: {
  examId: string;
  user: PortalUser;
  standaloneOnly?: boolean;
  enrollmentIdOverride?: string;
}) {
  const searchParams = useSearchParams();
  const enrollmentId = enrollmentIdOverride ?? searchParams.get("enrollmentId");
  const role = resolvePortalRole(user);
  const canManage = role === "INSTRUCTOR";
  const canAssign = role === "INSTRUCTOR";
  const [exam, setExam] = useState<Exam | null>(null);
  const [course, setCourse] = useState<Course | null>(null);
  const [session, setSession] = useState<ExamSession | null>(null);
  const [answers, setAnswers] = useState<Record<string, unknown>>({});
  const [current, setCurrent] = useState(0);
  const [remaining, setRemaining] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [result, setResult] = useState<Grade | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [submitConfirmOpen, setSubmitConfirmOpen] = useState(false);
  const [formError, setFormError] = useState("");
  const [assignments, setAssignments] = useState<AssessmentAssignment[]>([]);
  const [questionBank, setQuestionBank] = useState<Question[]>([]);
  const [questionQuery, setQuestionQuery] = useState("");
  const [assignmentDraft, setAssignmentDraft] = useState({
    assigneeType: "USER",
    assigneeId: "",
    availableFrom: "",
    dueAt: "",
    required: true,
  });
  const lastSaved = useRef("");
  const autoSubmitted = useRef(false);
  const sessionReadyAt = useRef(0);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiRequest<Exam>(`/api/v1/exams/${examId}`);
      if (standaloneOnly && data.courseId) {
        setExam(null);
        setError("Bài kiểm tra này thuộc khóa học. Hãy mở bài từ nội dung khóa học tương ứng.");
        return;
      }
      if (!standaloneOnly && !data.courseId) {
        setExam(null);
        setError("Đây là kỳ thi độc lập. Hãy mở từ mục Bài thi.");
        return;
      }
      setExam(data);
      if (canManage) {
        try {
          const bankPayload = await apiRequest<unknown>("/api/v1/questions?size=250");
          setQuestionBank(unwrapQuestionBank(bankPayload));
        } catch {
          setQuestionBank([]);
        }
      }
      if (data.courseId) {
        try {
          setCourse(
            await apiRequest<Course>(`/api/v1/courses/${data.courseId}`),
          );
        } catch {
          setCourse(null);
        }
        setAssignments([]);
      } else if (canAssign) {
        try {
          setAssignments(
            await apiRequest<AssessmentAssignment[]>(
              `/api/v1/assessment-assignments?assessmentId=${data.id}`,
            ),
          );
        } catch {
          setAssignments([]);
        }
      }
      if (!canManage) {
        const remembered = window.localStorage.getItem(
          sessionStorageKey(data.id, enrollmentId),
        );
        if (remembered) {
          try {
            const resumed = await apiRequest<ExamSession>(
              `/api/v1/exam-sessions/${remembered}`,
            );
            if (validAttempt(resumed, data.id)) {
              setRemaining(serverRemaining(resumed));
              autoSubmitted.current = false;
              sessionReadyAt.current = Date.now();
              setSession(resumed);
              setAnswers(resumed.answers ?? {});
              lastSaved.current = JSON.stringify(resumed.answers ?? {});
              setToast("Đã khôi phục phiên thi đang làm trên máy chủ.");
            } else {
              window.localStorage.removeItem(
                sessionStorageKey(data.id, enrollmentId),
              );
              if (resumed.status === "IN_PROGRESS" && (!resumed.questions || resumed.questions.length === 0)) {
                setError("Phiên thi không có câu hỏi. Hệ thống đã dừng phiên để tránh tự hoàn thành một đề rỗng.");
              }
            }
          } catch {
            window.localStorage.removeItem(
              sessionStorageKey(data.id, enrollmentId),
            );
          }
        }
      }
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể tải bài kiểm tra",
      );
    } finally {
      setLoading(false);
    }
  }, [examId, canAssign, canManage, enrollmentId, standaloneOnly]);
  useEffect(() => {
    void load();
  }, [load]);

  async function assignAudience(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (
      !exam ||
      exam.courseId ||
      !canAssign ||
      !assignmentDraft.assigneeId.trim()
    )
      return;
    setBusy(true);
    setFormError("");
    try {
      await apiRequest("/api/v1/assessment-assignments", {
        method: "POST",
        body: JSON.stringify({
          assessmentId: exam.id,
          assigneeType: assignmentDraft.assigneeType,
          assigneeId: assignmentDraft.assigneeId.trim(),
          availableFrom: assignmentDraft.availableFrom
            ? new Date(assignmentDraft.availableFrom).toISOString()
            : null,
          dueAt: assignmentDraft.dueAt
            ? new Date(assignmentDraft.dueAt).toISOString()
            : null,
          required: assignmentDraft.required,
        }),
      });
      setAssignments(
        await apiRequest<AssessmentAssignment[]>(
          `/api/v1/assessment-assignments?assessmentId=${exam.id}`,
        ),
      );
      setAssignmentDraft({
        assigneeType: "USER",
        assigneeId: "",
        availableFrom: "",
        dueAt: "",
        required: true,
      });
      setToast("Đã giao bài thi cho đối tượng được chọn.");
    } catch (caught) {
      setFormError(
        caught instanceof Error ? caught.message : "Không thể giao bài thi",
      );
    } finally {
      setBusy(false);
    }
  }

  async function revokeAudience(id: string) {
    if (!exam || !canAssign) return;
    setBusy(true);
    try {
      await apiRequest(`/api/v1/assessment-assignments/${id}`, {
        method: "DELETE",
      });
      setAssignments(
        await apiRequest<AssessmentAssignment[]>(
          `/api/v1/assessment-assignments?assessmentId=${exam.id}`,
        ),
      );
      setToast("Đã thu hồi đối tượng được giao.");
    } catch (caught) {
      setToast(
        caught instanceof Error
          ? caught.message
          : "Không thể thu hồi đối tượng",
      );
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    if (!session || session.status !== "IN_PROGRESS") {
      setRemaining(null);
      return;
    }
    setRemaining(serverRemaining(session));
    const timer = window.setInterval(() => {
      setRemaining((value) => (value === null ? serverRemaining(session) : Math.max(0, value - 1)));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [session]);

  const questions = session?.questions ?? exam?.questions ?? [];
  const answered = answerCount(answers);
  const question = questions[current];
  const progress = questions.length
    ? Math.round((answered * 100) / questions.length)
    : 0;
  const selectedQuestionIds = useMemo(
    () => new Set((exam?.questions ?? []).map((item) => item.id)),
    [exam?.questions],
  );
  const filteredQuestionBank = useMemo(() => {
    const query = questionQuery.trim().toLocaleLowerCase("vi-VN");
    return questionBank.filter((item) => {
      if (selectedQuestionIds.has(item.id)) return false;
      if (!query) return true;
      return `${item.prompt} ${item.type} ${item.id}`
        .toLocaleLowerCase("vi-VN")
        .includes(query);
    });
  }, [questionBank, questionQuery, selectedQuestionIds]);

  async function startExam() {
    setBusy(true);
    setError("");
    try {
      const started = await apiRequest<ExamSession>("/api/v1/exams/start", {
        method: "POST",
        body: JSON.stringify({ examId, enrollmentId: enrollmentId || null }),
      });
      if (!validAttempt(started, examId)) {
        if (started.status !== "IN_PROGRESS") {
          throw new Error("Phiên thi vừa tạo đã kết thúc. Vui lòng tải lại bài thi trước khi bắt đầu lượt mới.");
        }
        if (!started.questions?.length) {
          throw new Error("Đề thi chưa tải được câu hỏi. Hệ thống không ghi nhận hoàn thành và không trừ lượt làm.");
        }
        throw new Error("Thời gian phiên thi không hợp lệ. Vui lòng đồng bộ thời gian máy chủ rồi thử lại.");
      }
      setRemaining(serverRemaining(started));
      autoSubmitted.current = false;
      sessionReadyAt.current = Date.now();
      setSession(started);
      setAnswers(started.answers ?? {});
      setCurrent(0);
      lastSaved.current = JSON.stringify(started.answers ?? {});
      window.localStorage.setItem(
        sessionStorageKey(examId, enrollmentId),
        started.id,
      );
      setToast(
        started.attemptNo > 1
          ? `Đã bắt đầu lần thi thứ ${started.attemptNo}.`
          : "Đã bắt đầu bài kiểm tra.",
      );
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Không thể bắt đầu bài kiểm tra",
      );
    } finally {
      setBusy(false);
    }
  }

  function setAnswer(questionId: string, value: unknown) {
    setAnswers((previous) => ({ ...previous, [questionId]: value }));
  }

  async function save(showMessage = true) {
    if (!session || session.status !== "IN_PROGRESS") return;
    const serialized = JSON.stringify(answers);
    if (serialized === lastSaved.current) {
      if (showMessage) setToast("Đáp án đã được lưu.");
      return;
    }
    setBusy(true);
    try {
      const updated = await apiRequest<ExamSession>(
        `/api/v1/exam-sessions/${session.id}/answers`,
        { method: "PUT", body: JSON.stringify({ answers }) },
      );
      setSession(updated);
      lastSaved.current = serialized;
      if (showMessage) setToast("Đã lưu đáp án lên hệ thống.");
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể lưu đáp án",
      );
    } finally {
      setBusy(false);
    }
  }

  async function refreshGrade(sessionId: string, attempt = 0) {
    try {
      const [grades, refreshed] = await Promise.all([
        apiRequest<Grade[]>("/api/v1/grades/me"),
        apiRequest<ExamSession>(`/api/v1/exam-sessions/${sessionId}`),
      ]);
      const found =
        grades.find((grade) => grade.sessionId === sessionId) ?? null;
      setResult(found);
      setSession(refreshed);
      if (!found && attempt < 5) {
        window.setTimeout(
          () => void refreshGrade(sessionId, attempt + 1),
          1200 * (attempt + 1),
        );
      }
    } catch {
      if (attempt < 3) {
        window.setTimeout(
          () => void refreshGrade(sessionId, attempt + 1),
          1500 * (attempt + 1),
        );
      }
    }
  }

  async function submitExam(automatic = false) {
    if (!session) return;
    setSubmitConfirmOpen(false);
    setBusy(true);
    setError("");
    try {
      if (JSON.stringify(answers) !== lastSaved.current) {
        await apiRequest(`/api/v1/exam-sessions/${session.id}/answers`, {
          method: "PUT",
          body: JSON.stringify({ answers }),
        });
      }
      const submitted = await apiRequest<ExamSession>(
        `/api/v1/exam-sessions/${session.id}/submit`,
        {
          method: "POST",
          headers: { "Idempotency-Key": createIdempotencyKey() },
        },
      );
      setSession(submitted);
      window.localStorage.removeItem(sessionStorageKey(examId, enrollmentId));
      if (submitted.status === "EXPIRED") {
        setToast("Phiên thi đã quá thời gian ân hạn nên không được gửi chấm.");
        return;
      }
      setToast("Đã nộp bài thành công. Hệ thống đang chấm điểm.");
      window.setTimeout(() => void refreshGrade(submitted.id), 1200);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể nộp bài");
    } finally {
      setBusy(false);
    }
  }

  async function updateExamQuestions(nextQuestions: ExamQuestion[]) {
    if (!exam || !canManage || busy) return;
    if (exam.status !== "DRAFT") {
      setFormError("Chỉ có thể thay đổi cấu trúc câu hỏi khi bài thi còn ở trạng thái bản nháp.");
      return;
    }
    if (!nextQuestions.length) {
      setFormError("Bài thi phải có ít nhất một câu hỏi; hệ thống không cho lưu đề rỗng.");
      return;
    }
    setBusy(true);
    setFormError("");
    try {
      const updated = await apiRequest<Exam>(`/api/v1/exams/${exam.id}`, {
        method: "PUT",
        body: JSON.stringify({
          title: exam.title,
          courseId: exam.courseId,
          lessonId: exam.lessonId ?? null,
          contextType: exam.contextType ?? (exam.courseId ? "COURSE_QUIZ" : "STANDALONE_EXAM"),
          cohortId: exam.cohortId ?? null,
          autoGrade: exam.autoGrade,
          durationMinutes: exam.durationMinutes,
          opensAt: exam.opensAt ?? null,
          closesAt: exam.closesAt ?? null,
          maxAttempts: exam.maxAttempts,
          waitMinutesBetweenAttempts: exam.waitMinutesBetweenAttempts ?? 0,
          passingScore: exam.passingScore,
          shuffleQuestions: exam.shuffleQuestions,
          shuffleAnswers: exam.shuffleAnswers,
          scoreStrategy: exam.scoreStrategy,
          status: exam.status,
          questions: nextQuestions.map((item, index) => ({
            questionId: item.id,
            points: item.points || 1,
            sortOrder: index + 1,
          })),
        }),
      });
      setExam(updated);
      setToast("Đã cập nhật nội dung đề thi.");
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Không thể cập nhật nội dung đề thi");
    } finally {
      setBusy(false);
    }
  }

  async function updateExam(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!exam || !canManage) return;
    setBusy(true);
    setFormError("");
    const data = new FormData(event.currentTarget);
    const opensAt = String(data.get("opensAt") ?? "");
    const closesAt = String(data.get("closesAt") ?? "");
    try {
      await apiRequest(`/api/v1/exams/${exam.id}`, {
        method: "PUT",
        body: JSON.stringify({
          title: String(data.get("title") ?? ""),
          courseId: exam.courseId,
          lessonId: exam.lessonId ?? null,
          contextType:
            exam.contextType ??
            (exam.courseId ? "COURSE_QUIZ" : "STANDALONE_EXAM"),
          cohortId: exam.cohortId ?? null,
          autoGrade: data.get("autoGrade") === "on",
          durationMinutes: Number(data.get("durationMinutes") || 30),
          opensAt: opensAt ? new Date(opensAt).toISOString() : null,
          closesAt: closesAt ? new Date(closesAt).toISOString() : null,
          maxAttempts: Number(data.get("maxAttempts") || 1),
          waitMinutesBetweenAttempts: Number(
            data.get("waitMinutesBetweenAttempts") || 0,
          ),
          passingScore: Number(data.get("passingScore") || 70),
          shuffleQuestions: data.get("shuffleQuestions") === "on",
          shuffleAnswers: data.get("shuffleAnswers") === "on",
          scoreStrategy: String(data.get("scoreStrategy") ?? "HIGHEST"),
          status: String(data.get("status") ?? "DRAFT"),
          questions: exam.questions.map((item, index) => ({
            questionId: item.id,
            points: item.points,
            sortOrder: index + 1,
          })),
        }),
      });
      setEditOpen(false);
      setToast(exam.courseId ? "Đã lưu cấu hình bài kiểm tra." : "Đã lưu cấu hình bài thi.");
      await load();
    } catch (caught) {
      setFormError(
        caught instanceof Error
          ? caught.message
          : "Không thể cập nhật bài kiểm tra",
      );
    } finally {
      setBusy(false);
    }
  }

  async function archiveExam() {
    if (!exam || !canManage) return;
    if (
      !window.confirm(
        exam.courseId ? "Lưu trữ bài kiểm tra khỏi khóa học? Mọi lượt làm, đáp án và điểm vẫn được giữ nguyên." : "Lưu trữ bài thi khỏi danh sách? Mọi lượt làm, đáp án và điểm vẫn được giữ nguyên.",
      )
    )
      return;
    setBusy(true);
    try {
      await apiRequest(`/api/v1/exams/${exam.id}`, { method: "DELETE" });
      window.location.assign(exam.courseId ? `/courses/${exam.courseId}` : "/exams");
    } catch (caught) {
      setToast(
        caught instanceof Error
          ? caught.message
          : "Không thể lưu trữ bài kiểm tra",
      );
      setBusy(false);
    }
  }

  useEffect(() => {
    if (!session || session.status !== "IN_PROGRESS") return;
    let online = navigator.onLine;
    const heartbeat = () => {
      void apiRequest<ExamSession>(`/api/v1/exam-sessions/${session.id}/heartbeat`, {
        method: "POST",
      }).then((updated) => {
        setSession(updated);
        if (updated.status === "IN_PROGRESS") setRemaining(serverRemaining(updated));
      }).catch(() => undefined);
    };
    const record = (type: string, details?: string) => {
      void apiRequest(`/api/v1/exam-sessions/${session.id}/events`, {
        method: "POST",
        body: JSON.stringify({
          type,
          details: details ?? null,
          occurredAt: new Date().toISOString(),
        }),
      }).catch(() => undefined);
    };
    const onVisibility = () => {
      if (document.visibilityState === "hidden")
        record("TAB_HIDDEN", "document.hidden");
    };
    const onBlur = () => record("WINDOW_BLUR", "window.blur");
    const onOffline = () => {
      online = false;
      record("NETWORK_DISCONNECTED", "navigator.offline");
    };
    const onOnline = () => {
      if (!online) record("NETWORK_RECONNECTED", "navigator.online");
      online = true;
      heartbeat();
    };
    heartbeat();
    const timer = window.setInterval(heartbeat, 20000);
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("blur", onBlur);
    window.addEventListener("offline", onOffline);
    window.addEventListener("online", onOnline);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("blur", onBlur);
      window.removeEventListener("offline", onOffline);
      window.removeEventListener("online", onOnline);
    };
  }, [session?.id, session?.status]);

  useEffect(() => {
    if (!session || session.status !== "IN_PROGRESS") return;
    const timer = window.setInterval(() => {
      if (!busy && JSON.stringify(answers) !== lastSaved.current)
        void save(false);
    }, 20000);
    return () => window.clearInterval(timer);
  }, [session, answers, busy]);

  useEffect(() => {
    if (
      !session ||
      session.status !== "IN_PROGRESS" ||
      remaining === null ||
      remaining > 0 ||
      autoSubmitted.current ||
      !session.questions?.length ||
      Date.now() - sessionReadyAt.current < 1_500
    ) return;

    autoSubmitted.current = true;
    void apiRequest<ExamSession>(`/api/v1/exam-sessions/${session.id}/heartbeat`, { method: "POST" })
      .then((verified) => {
        setSession(verified);
        const verifiedRemaining = serverRemaining(verified);
        setRemaining(verifiedRemaining);
        if (verified.status === "IN_PROGRESS" && verified.questions?.length && verifiedRemaining === 0) {
          return submitExam(true);
        }
        autoSubmitted.current = false;
        return undefined;
      })
      .catch((caught) => {
        autoSubmitted.current = false;
        setError(caught instanceof Error ? caught.message : "Không thể xác minh thời gian phiên thi");
      });
  }, [remaining, session?.id, session?.status, session?.questions?.length]);

  if (loading)
    return <LoadingState label="Đang tải cấu hình bài kiểm tra..." />;
  if (error && !exam)
    return <ErrorState message={error} onRetry={() => void load()} />;
  if (!exam)
    return (
      <EmptyState
        title={standaloneOnly ? "Không tìm thấy bài thi" : "Không tìm thấy bài kiểm tra"}
        description={error || "Nội dung có thể đã bị đóng hoặc không thuộc phạm vi của vai trò đang đăng nhập."}
      />
    );

  const backHref = exam.courseId
    ? role === "INSTRUCTOR"
      ? instructorCoursePath(exam.courseId)
      : enrollmentId
        ? studentCoursePath(enrollmentId)
        : studentCoursePath()
    : standaloneExamPath(role);

  if (canManage)
    return (
      <>
        <PageHeader
          backHref={backHref}
          eyebrow={exam.courseId ? "BÀI KIỂM TRA KHÓA HỌC" : "CẤU HÌNH BÀI THI"}
          title={exam.title}
          description={course?.name ?? "Kỳ thi độc lập — không thuộc khóa học"}
          actions={
            <>
              <StatusBadge value={exam.status} />
              <button
                className="button secondary"
                onClick={() => {
                  setFormError("");
                  setEditOpen(true);
                }}
              >
                <Icon name="edit" />
                Chỉnh sửa
              </button>
              <button
                className="button danger"
                disabled={busy}
                onClick={() => void archiveExam()}
              >
                <Icon name="trash" />
                Xóa
              </button>
            </>
          }
        />
        <section className="exam-editor-source-layout">
          <article className="workspace-panel exam-editor-bank-panel">
            <header>
              <div>
                <h2>Ngân hàng câu hỏi</h2>
                <p>{questionBank.length} câu khả dụng · câu đã chọn được ẩn khỏi danh sách.</p>
              </div>
              <span className="workspace-tag">{filteredQuestionBank.length} còn lại</span>
            </header>
            <div className="workspace-panel-body">
              <label className="exam-editor-search">
                <Icon name="search" size={17} />
                <input
                  value={questionQuery}
                  onChange={(event) => setQuestionQuery(event.target.value)}
                  placeholder="Tìm nội dung, loại hoặc mã câu hỏi…"
                />
              </label>
              <div className="exam-editor-bank-list">
                {filteredQuestionBank.slice(0, 80).map((item) => (
                  <article className="exam-editor-bank-item" key={item.id}>
                    <span className="exam-editor-type-icon"><Icon name="question" size={17} /></span>
                    <div>
                      <div className="exam-editor-item-title">
                        <strong>{item.prompt}</strong>
                        <span>{(item.type ?? "QUESTION").replaceAll("_", " ")}</span>
                      </div>
                      <small>{item.defaultPoints || 1} điểm · {item.options?.length || 0} phương án</small>
                    </div>
                    <button
                      type="button"
                      className="icon-button"
                      aria-label={`Thêm câu hỏi ${item.prompt}`}
                      disabled={busy || exam.status !== "DRAFT"}
                      onClick={() => void updateExamQuestions([
                        ...(exam.questions ?? []),
                        {
                          id: item.id,
                          type: item.type,
                          prompt: item.prompt,
                          options: item.options ?? [],
                          points: item.defaultPoints || 1,
                          sortOrder: (exam.questions?.length ?? 0) + 1,
                        },
                      ])}
                    >
                      <Icon name="plus" size={17} />
                    </button>
                  </article>
                ))}
                {!filteredQuestionBank.length && (
                  <EmptyState
                    title="Không còn câu hỏi phù hợp"
                    description="Thay đổi từ khóa hoặc tạo thêm câu hỏi trong ngân hàng."
                  />
                )}
              </div>
            </div>
          </article>

          <article className="workspace-panel exam-editor-content-panel">
            <header>
              <div>
                <h2>Nội dung đề thi</h2>
                <p>Phiên bản {exam.version} · {(exam.questions ?? []).length} câu · {exam.questions.reduce((sum, item) => sum + Number(item.points || 0), 0)} điểm</p>
              </div>
              <StatusBadge value={exam.status} />
            </header>
            <div className="workspace-panel-body">
              {formError && <div className="form-alert error"><Icon name="warning" />{formError}</div>}
              <div className="exam-editor-selected-list">
                {(exam.questions ?? []).map((item, index) => (
                  <article className="exam-editor-selected-item" key={item.id}>
                    <span className="exam-editor-drag" aria-hidden="true">⋮⋮</span>
                    <span className="question-number">{index + 1}</span>
                    <span className="exam-editor-type-icon"><Icon name="question" size={17} /></span>
                    <div>
                      <div className="exam-editor-item-title">
                        <strong>{item.prompt}</strong>
                        <span>{(item.type ?? "QUESTION").replaceAll("_", " ")}</span>
                      </div>
                      <small>{item.options?.length ? `${item.options.length} phương án` : "Câu trả lời tự do"}</small>
                    </div>
                    <strong className="exam-editor-points">{item.points} điểm</strong>
                    <button
                      type="button"
                      className="icon-button"
                      aria-label={`Xóa câu ${index + 1}`}
                      disabled={busy || exam.status !== "DRAFT" || exam.questions.length <= 1}
                      onClick={() => void updateExamQuestions(exam.questions.filter((question) => question.id !== item.id))}
                    >
                      <Icon name="trash" size={16} />
                    </button>
                  </article>
                ))}
              </div>
              <div className="exam-editor-drop-zone">
                <Icon name="list" size={19} />
                <span>Kéo thả để sắp xếp được giữ làm bước tiếp theo; thứ tự hiện tại được lưu ổn định theo số câu.</span>
              </div>
            </div>
          </article>

          <aside className="workspace-panel exam-editor-settings-panel">
            <header>
              <div><h2>Cài đặt bài thi</h2><p>Cấu hình thời gian, lần làm và điều kiện đạt.</p></div>
            </header>
            <div className="workspace-panel-body">
              <dl className="exam-editor-settings-list">
                <div><dt>Khóa học</dt><dd>{course?.name ?? "Kỳ thi độc lập"}</dd></div>
                <div><dt>Thời lượng</dt><dd>{formatDuration(exam.durationMinutes)}</dd></div>
                <div><dt>Số câu hỏi</dt><dd>{exam.questions.length} câu</dd></div>
                <div><dt>Tổng điểm</dt><dd>{exam.questions.reduce((sum, item) => sum + Number(item.points || 0), 0)} điểm</dd></div>
                <div><dt>Điểm đạt</dt><dd>{exam.passingScore}%</dd></div>
                <div><dt>Số lượt làm</dt><dd>{exam.maxAttempts}</dd></div>
                <div><dt>Thời gian mở</dt><dd>{formatDate(exam.opensAt, true)}</dd></div>
                <div><dt>Thời gian đóng</dt><dd>{formatDate(exam.closesAt, true)}</dd></div>
              </dl>
              <button className="button primary full" onClick={() => setEditOpen(true)}>
                <Icon name="edit" /> Lưu / sửa cấu hình
              </button>
              {course && (
                <Link className="button secondary full" href={instructorCoursePath(course.id)}>
                  <Icon name="book" /> Mở khóa học
                </Link>
              )}
              <div className="form-alert info">
                <Icon name="warning" />
                Khi bài thi đã xuất bản hoặc có lượt làm, cấu trúc câu hỏi được khóa để bảo toàn kết quả.
              </div>
            </div>
          </aside>
        </section>
        {!exam.courseId && canAssign && (
          <section className="section-card exam-audience-card">
            <div className="section-title">
              <div>
                <h2>Đối tượng được giao</h2>
                <p>
                  {assignments.some((item) => item.status === "ACTIVE")
                    ? "Chỉ tài khoản hoặc đơn vị đang hoạt động bên dưới được tham gia trong thời hạn cấu hình."
                    : "Chưa có đối tượng cụ thể: kỳ thi đang mở cho mọi người có quyền tham gia."}
                </p>
              </div>
              <StatusBadge
                value={
                  assignments.some((item) => item.status === "ACTIVE")
                    ? "RESTRICTED"
                    : "OPEN"
                }
              />
            </div>
            <div className="exam-audience-layout">
              <div className="audience-list">
                {assignments.length ? (
                  assignments.map((item) => (
                    <article
                      key={item.id}
                      className={item.status === "REVOKED" ? "revoked" : ""}
                    >
                      <span className="mini-avatar">
                        {item.assigneeType.slice(0, 1)}
                      </span>
                      <div>
                        <strong>
                          {item.assigneeType} · {item.assigneeId}
                        </strong>
                        <small>
                          {item.availableFrom
                            ? `Từ ${formatDate(item.availableFrom, true)}`
                            : "Có hiệu lực ngay"}
                          {item.dueAt
                            ? ` · đến ${formatDate(item.dueAt, true)}`
                            : " · không giới hạn hạn"}
                        </small>
                      </div>
                      <StatusBadge value={item.status} />
                      {item.status === "ACTIVE" && (
                        <button
                          className="button ghost compact"
                          disabled={busy}
                          onClick={() => void revokeAudience(item.id)}
                        >
                          <Icon name="close" />
                          Thu hồi
                        </button>
                      )}
                    </article>
                  ))
                ) : (
                  <EmptyState
                    title="Chưa giới hạn đối tượng"
                    description="Thêm user, nhóm, phòng ban hoặc chi nhánh để biến kỳ thi thành kỳ thi được giao."
                  />
                )}
              </div>
              <form
                className="form-stack audience-form"
                onSubmit={assignAudience}
              >
                <div className="form-grid two">
                  <label>
                    Loại đối tượng
                    <select
                      value={assignmentDraft.assigneeType}
                      onChange={(event) =>
                        setAssignmentDraft({
                          ...assignmentDraft,
                          assigneeType: event.target.value,
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
                    ID đối tượng <b>*</b>
                    <input
                      required
                      value={assignmentDraft.assigneeId}
                      onChange={(event) =>
                        setAssignmentDraft({
                          ...assignmentDraft,
                          assigneeId: event.target.value,
                        })
                      }
                      placeholder="UUID người dùng hoặc đơn vị"
                    />
                  </label>
                </div>
                <div className="form-grid two">
                  <label>
                    Có hiệu lực từ
                    <input
                      type="datetime-local"
                      value={assignmentDraft.availableFrom}
                      onChange={(event) =>
                        setAssignmentDraft({
                          ...assignmentDraft,
                          availableFrom: event.target.value,
                        })
                      }
                    />
                  </label>
                  <label>
                    Hạn tham gia
                    <input
                      type="datetime-local"
                      value={assignmentDraft.dueAt}
                      onChange={(event) =>
                        setAssignmentDraft({
                          ...assignmentDraft,
                          dueAt: event.target.value,
                        })
                      }
                    />
                  </label>
                </div>
                <label className="check-row">
                  <input
                    type="checkbox"
                    checked={assignmentDraft.required}
                    onChange={(event) =>
                      setAssignmentDraft({
                        ...assignmentDraft,
                        required: event.target.checked,
                      })
                    }
                  />
                  <span>
                    <strong>Đánh dấu là kỳ thi bắt buộc</strong>
                    <small>Dùng cho báo cáo và nhắc việc sau này.</small>
                  </span>
                </label>
                {formError && (
                  <div className="form-alert error">
                    <Icon name="warning" />
                    {formError}
                  </div>
                )}
                <button className="button primary" disabled={busy}>
                  <Icon name="users" />
                  {busy ? "Đang giao..." : "Giao bài thi"}
                </button>
              </form>
            </div>
          </section>
        )}
        <Modal
          open={editOpen}
          onClose={() => !busy && setEditOpen(false)}
          title="Chỉnh sửa bài kiểm tra"
          description="Có thể sửa khi đề chưa phát sinh lượt làm. Mọi thay đổi được lưu trực tiếp trong Assessment Service."
        >
          <form className="form-stack" onSubmit={updateExam}>
            <label>
              Tên bài kiểm tra <b>*</b>
              <input name="title" required defaultValue={exam.title} />
            </label>
            <div className="form-grid four">
              <label>
                Thời lượng
                <input
                  name="durationMinutes"
                  type="number"
                  min="1"
                  max="480"
                  defaultValue={exam.durationMinutes}
                />
              </label>
              <label>
                Số lần làm
                <input
                  name="maxAttempts"
                  type="number"
                  min="1"
                  max="20"
                  defaultValue={exam.maxAttempts}
                />
              </label>
              <label>
                Điểm đạt
                <input
                  name="passingScore"
                  type="number"
                  min="0"
                  max="100"
                  defaultValue={exam.passingScore}
                />
              </label>
              <label>
                Trạng thái
                <select name="status" defaultValue={exam.status}>
                  <option value="DRAFT">Bản nháp</option>
                  <option value="ACTIVE">Hoạt động</option>
                  <option value="INACTIVE">Tạm đóng</option>
                </select>
              </label>
            </div>
            <div className="form-grid two">
              <label>
                Mở từ
                <input
                  name="opensAt"
                  type="datetime-local"
                  defaultValue={localDateTime(exam.opensAt)}
                />
              </label>
              <label>
                Đóng lúc
                <input
                  name="closesAt"
                  type="datetime-local"
                  defaultValue={localDateTime(exam.closesAt)}
                />
              </label>
            </div>
            <div className="form-grid two">
              <label>
                Chờ giữa các lần làm
                <input
                  name="waitMinutesBetweenAttempts"
                  type="number"
                  min="0"
                  defaultValue={exam.waitMinutesBetweenAttempts}
                />
              </label>
              <label>
                Cách lấy điểm
                <select name="scoreStrategy" defaultValue={exam.scoreStrategy}>
                  <option value="HIGHEST">Cao nhất</option>
                  <option value="LATEST">Lần gần nhất</option>
                  <option value="AVERAGE">Trung bình</option>
                </select>
              </label>
            </div>
            <div className="check-group">
              <label className="check-row">
                <input
                  type="checkbox"
                  name="autoGrade"
                  defaultChecked={exam.autoGrade ?? true}
                />
                <span>
                  <strong>Tự chấm câu khách quan</strong>
                </span>
              </label>
              <label className="check-row">
                <input
                  type="checkbox"
                  name="shuffleQuestions"
                  defaultChecked={exam.shuffleQuestions}
                />
                <span>
                  <strong>Trộn câu hỏi</strong>
                </span>
              </label>
              <label className="check-row">
                <input
                  type="checkbox"
                  name="shuffleAnswers"
                  defaultChecked={exam.shuffleAnswers}
                />
                <span>
                  <strong>Trộn phương án</strong>
                </span>
              </label>
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
                onClick={() => setEditOpen(false)}
              >
                Hủy
              </button>
              <button className="button primary" disabled={busy}>
                {busy ? "Đang lưu..." : "Lưu thay đổi"}
              </button>
            </div>
          </form>
        </Modal>
        {toast && <Toast message={toast} onClose={() => setToast("")} />}
      </>
    );

  if (session?.status === "EXPIRED")
    return (
      <>
        <PageHeader
          backHref={backHref}
          eyebrow="PHIÊN ĐÃ HẾT HẠN"
          title={exam.title}
          description={`Lần thi ${session.attemptNo} không được ghi nhận vì máy chủ nhận yêu cầu sau thời gian ân hạn.`}
        />
        <section className="result-panel">
          <span className="result-icon">
            <Icon name="warning" size={34} />
          </span>
          <h2>Không thể nộp bài</h2>
          <p>
            Hệ thống đã khóa phiên và không gửi dữ liệu sang chấm điểm. Nếu còn
            lượt làm, bạn có thể bắt đầu lại theo quy định của đề.
          </p>
          <Link className="button primary" href={backHref}>
            Về danh sách bài kiểm tra
          </Link>
        </section>
        {toast && <Toast message={toast} onClose={() => setToast("")} />}
      </>
    );

  if (session?.status === "SUBMITTED" || session?.status === "GRADED")
    return (
      <>
        <PageHeader
          backHref={backHref}
          eyebrow="ĐÃ NỘP BÀI"
          title={exam.title}
          description={`Lần thi ${session.attemptNo} đã được ghi nhận lúc ${formatDate(session.submittedAt, true)}.`}
        />
        <section className="result-panel">
          <span className="result-icon">
            <Icon name="check" size={34} />
          </span>
          <h2>Nộp bài thành công</h2>
          {result ? (
            <>
              <strong className="result-score">
                {Math.round(result.percentage)}%
              </strong>
              <StatusBadge value={result.status} />
              <p>
                {result.status === "PENDING_MANUAL"
                  ? "Bài có câu tự luận và đang chờ giảng viên chấm."
                  : result.passed
                    ? "Bạn đã đạt yêu cầu bài kiểm tra."
                    : "Kết quả chưa đạt mức yêu cầu."}
              </p>
            </>
          ) : (
            <>
              <p>
                Hệ thống đang xử lý kết quả và sẽ tự làm mới trong giây lát.
              </p>
              <button
                className="button secondary"
                onClick={() => void refreshGrade(session.id)}
              >
                <Icon name="refresh" />
                Làm mới kết quả
              </button>
            </>
          )}
          <Link className="button primary" href={backHref}>
            Về danh sách bài kiểm tra
          </Link>
        </section>
        {toast && <Toast message={toast} onClose={() => setToast("")} />}
      </>
    );

  if (!canManage && (exam.questions ?? []).length === 0)
    return (
      <>
        <PageHeader
          backHref={backHref}
          eyebrow="ĐỀ THI CHƯA SẴN SÀNG"
          title={exam.title}
          description={course?.name ?? "Kỳ thi độc lập được cấp quyền"}
        />
        <section className="result-panel exam-not-ready-panel">
          <span className="result-icon">
            <Icon name="warning" size={34} />
          </span>
          <h2>Đề thi chưa có câu hỏi</h2>
          <p>
            Hệ thống không tạo phiên làm bài rỗng. Giảng viên cần thêm ít nhất
            một câu hỏi và xuất bản lại đề trước khi học viên bắt đầu.
          </p>
          <button className="button secondary" onClick={() => void load()}>
            <Icon name="refresh" />
            Kiểm tra lại đề
          </button>
          <Link className="button primary" href={backHref}>
            Quay lại
          </Link>
        </section>
      </>
    );

  if (session?.status === "IN_PROGRESS" && questions.length === 0)
    return (
      <>
        <PageHeader
          backHref={backHref}
          eyebrow="PHIÊN THI KHÔNG HỢP LỆ"
          title={exam.title}
          description="Máy chủ đã tạo phiên nhưng không trả về câu hỏi."
        />
        <section className="result-panel exam-not-ready-panel">
          <span className="result-icon">
            <Icon name="warning" size={34} />
          </span>
          <h2>Không thể hiển thị đề thi</h2>
          <p>
            Phiên này được giữ nguyên để tránh mất lượt làm. Hãy tải lại dữ liệu;
            hệ thống sẽ không tự nộp một đề rỗng.
          </p>
          <button className="button secondary" onClick={() => void load()}>
            <Icon name="refresh" />
            Tải lại dữ liệu
          </button>
          <Link className="button primary" href={backHref}>
            Quay lại
          </Link>
        </section>
      </>
    );

  if (!session)
    return (
      <>
        <PageHeader
          backHref={backHref}
          eyebrow="BÀI KIỂM TRA ĐƯỢC GIAO"
          title={exam.title}
          description={course?.name ?? "Kỳ thi độc lập được cấp quyền"}
        />
        <section className="exam-intro">
          <div className="exam-intro-main">
            <span className="exam-hero-icon">
              <Icon name="exam" size={36} />
            </span>
            <div>
              <StatusBadge value={exam.status} />
              <h2>Sẵn sàng bắt đầu?</h2>
              <p>
                Khi bắt đầu, đồng hồ sẽ chạy liên tục. Đáp án được lưu trên máy
                chủ khi bạn bấm lưu hoặc nộp bài.
              </p>
            </div>
          </div>
          <div className="exam-rules">
            <div>
              <Icon name="clock" />
              <span>
                <strong>{formatDuration(exam.durationMinutes)}</strong>Thời gian
                làm bài
              </span>
            </div>
            <div>
              <Icon name="question" />
              <span>
                <strong>{(exam.questions ?? []).length} câu</strong>Tổng số câu
                hỏi
              </span>
            </div>
            <div>
              <Icon name="target" />
              <span>
                <strong>{exam.passingScore}%</strong>Điểm đạt
              </span>
            </div>
            <div>
              <Icon name="refresh" />
              <span>
                <strong>{exam.maxAttempts} lần</strong>Số lượt tối đa
              </span>
            </div>
          </div>
          {error && (
            <div className="form-alert error">
              <Icon name="warning" />
              {error}
            </div>
          )}
          <button
            className="button primary large-action"
            disabled={busy || exam.status !== "ACTIVE" || (exam.questions ?? []).length === 0}
            onClick={() => void startExam()}
          >
            {busy ? "Đang tạo phiên thi..." : "Bắt đầu làm bài"}
            <Icon name="arrow" />
          </button>
        </section>
      </>
    );

  return (
    <div className="exam-taking">
      <header className="exam-taking-header">
        <div>
          <Link href={backHref} className="icon-button">
            <Icon name="close" />
          </Link>
          <div>
            <small>BÀI KIỂM TRA ĐANG DIỄN RA</small>
            <h1>{exam.title}</h1>
          </div>
        </div>
        <div className="exam-session-signals">
          {Boolean(session.suspiciousEventCount) && (
            <span title="Sự kiện bất thường phía trình duyệt">
              <Icon name="warning" size={15} />
              {session.suspiciousEventCount}
            </span>
          )}
          <div className={`exam-timer ${remaining !== null && remaining < 300 ? "urgent" : ""}`}>
            <Icon name="clock" />
            <span>
              <small>Thời gian làm bài còn lại</small>
              <strong>{timerLabel(remaining ?? 0)}</strong>
            </span>
          </div>
        </div>
      </header>
      <div className="exam-progress-row">
        <ProgressBar value={progress} />
        <span>
          {answered}/{(questions ?? []).length} câu đã trả lời
        </span>
      </div>
      <div className="exam-taking-layout">
        <aside className="exam-navigator">
          <h2>Danh sách câu hỏi</h2>
          <div>
            {(questions ?? []).map((item, index) => (
              <button
                key={item.id}
                type="button"
                className={`${index === current ? "active" : ""} ${answers[item.id] !== undefined && String(answers[item.id]).length ? "answered" : ""}`}
                onClick={() => setCurrent(index)}
                aria-current={index === current ? "step" : undefined}
                aria-label={`Câu ${index + 1}, ${answers[item.id] !== undefined && String(answers[item.id]).length ? "đã trả lời" : "chưa trả lời"}`}
              >
                {index + 1}
              </button>
            ))}
          </div>
          <div className="exam-legend">
            <span>
              <i className="answered" />
              Đã trả lời
            </span>
            <span>
              <i />
              Chưa trả lời
            </span>
          </div>
        </aside>
        <main className="exam-question-panel">
          {question && (
            <QuestionEditor
              question={question}
              value={answers[question.id]}
              onChange={(value) => setAnswer(question.id, value)}
              index={current}
            />
          )}
          <footer className="exam-actions">
            <button
              className="button secondary"
              disabled={current === 0}
              onClick={() => {
                void save(false);
                setCurrent((value) => Math.max(0, value - 1));
              }}
            >
              <Icon name="back" />
              Câu trước
            </button>
            <div>
              <button
                className="button secondary"
                disabled={busy}
                onClick={() => void save()}
              >
                <Icon name="save" />
                Lưu bài
              </button>
              {current < (questions ?? []).length - 1 ? (
                <button
                  className="button primary"
                  onClick={() => {
                    void save(false);
                    setCurrent((value) =>
                      Math.min((questions ?? []).length - 1, value + 1),
                    );
                  }}
                >
                  Câu tiếp theo
                  <Icon name="arrow" />
                </button>
              ) : (
                <button
                  className="button primary"
                  disabled={busy}
                  onClick={() => setSubmitConfirmOpen(true)}
                >
                  Nộp bài
                  <Icon name="check" />
                </button>
              )}
            </div>
          </footer>
        </main>
      </div>
      <Modal
        open={submitConfirmOpen}
        title="Xác nhận nộp bài"
        description="Sau khi nộp, bạn không thể thay đổi đáp án."
        onClose={() => setSubmitConfirmOpen(false)}
      >
        <div className="submit-review">
          <dl>
            <div><dt>Tổng số câu</dt><dd>{questions.length}</dd></div>
            <div><dt>Đã trả lời</dt><dd>{answered}</dd></div>
            <div><dt>Chưa trả lời</dt><dd>{Math.max(0, questions.length - answered)}</dd></div>
            <div><dt>Thời gian còn lại</dt><dd>{timerLabel(remaining ?? 0)}</dd></div>
          </dl>
          {questions.length - answered > 0 && (
            <div className="form-alert error" role="alert">
              <Icon name="warning" />
              Bạn còn {questions.length - answered} câu chưa trả lời.
            </div>
          )}
          <div className="modal-actions">
            <button
              type="button"
              className="button secondary"
              onClick={() => setSubmitConfirmOpen(false)}
            >
              Quay lại kiểm tra
            </button>
            <button
              type="button"
              className="button primary"
              disabled={busy}
              onClick={() => void submitExam(false)}
            >
              <Icon name="check" />
              {busy ? "Đang nộp…" : "Xác nhận nộp bài"}
            </button>
          </div>
        </div>
      </Modal>
      {error && (
        <div className="floating-error">
          <Icon name="warning" />
          {error}
          <button onClick={() => setError("")}>
            <Icon name="close" />
          </button>
        </div>
      )}
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </div>
  );
}

function QuestionEditor({
  question,
  value,
  onChange,
  index,
}: {
  question: ExamQuestion;
  value: unknown;
  onChange: (value: unknown) => void;
  index: number;
}) {
  const selected = Array.isArray(value) ? value.map(String) : [];
  const options =
    question.type === "TRUE_FALSE" && question.options.length === 0
      ? ["Đúng", "Sai"]
      : question.options;
  return (
    <article className="question-editor">
      <div className="question-editor-head">
        <span>Câu {index + 1}</span>
        <strong>{question.points} điểm</strong>
      </div>
      <h2>{question.prompt}</h2>
      {question.type === "MULTIPLE_CHOICE" ? (
        <div className="answer-options">
          {options.map((option, optionIndex) => {
            const optionLetter = String.fromCharCode(65 + optionIndex);
            const visibleOption = option.replace(/^\s*[A-Z][.)\-:]?\s+/, "");
            const checked = selected.includes(option);
            return (
              <label
                className={`answer-choice ${checked ? "selected" : ""}`}
                key={option}
              >
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={(event) =>
                    onChange(
                      event.target.checked
                        ? [...selected, option]
                        : selected.filter((item) => item !== option),
                    )
                  }
                />
                <span className="answer-letter" aria-hidden="true">
                  {optionLetter}
                </span>
                <span className="answer-text">{visibleOption}</span>
              </label>
            );
          })}
        </div>
      ) : question.type === "SINGLE_CHOICE" ||
        question.type === "TRUE_FALSE" ? (
        <div className="answer-options">
          {options.map((option, optionIndex) => {
            const optionLetter = String.fromCharCode(65 + optionIndex);
            const visibleOption = option.replace(/^\s*[A-Z][.)\-:]?\s+/, "");
            const checked = String(value ?? "") === option;
            return (
              <label
                className={`answer-choice ${checked ? "selected" : ""}`}
                key={option}
              >
                <input
                  type="radio"
                  name={question.id}
                  checked={checked}
                  onChange={() => onChange(option)}
                />
                <span className="answer-letter" aria-hidden="true">
                  {optionLetter}
                </span>
                <span className="answer-text">{visibleOption}</span>
              </label>
            );
          })}
        </div>
      ) : (
        <label className="essay-answer">
          Câu trả lời
          <textarea
            rows={question.type === "ESSAY" ? 10 : 4}
            value={String(value ?? "")}
            onChange={(event) => onChange(event.target.value)}
            placeholder="Nhập câu trả lời của bạn..."
          />
        </label>
      )}
    </article>
  );
}
