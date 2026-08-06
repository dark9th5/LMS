export type CourseStatus = "DRAFT" | "PUBLISHED" | "HIDDEN" | "ARCHIVED";
export type LessonType = "TEXT" | "PDF" | "DOCX" | "VIDEO" | "AUDIO" | "FILE" | "ASSIGNMENT" | "EXAM";

export type Lesson = {
  id: string;
  title: string;
  type: LessonType;
  textContent?: string | null;
  fileId?: string | null;
  required: boolean;
  sortOrder: number;
  estimatedMinutes: number;
};

export type Course = {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  objectives?: string | null;
  targetAudience?: string | null;
  durationMinutes?: number | null;
  passingScore: number;
  completionPolicyJson: string;
  categoryId?: string | null;
  status: CourseStatus;
  contentVersion: number;
  publishedVersion: number;
  hasUnpublishedChanges: boolean;
  publishedAt?: string | null;
  ownerId: string;
  lessons?: Lesson[];
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Enrollment = {
  id: string;
  courseId: string;
  userId: string;
  dueAt?: string | null;
  status: string;
  enrolledAt: string;
};

export type LessonProgress = {
  lessonId: string;
  completed: boolean;
  learningSeconds: number;
  position?: string | null;
  completedAt?: string | null;
};

export type CourseProgress = {
  enrollmentId: string;
  courseId: string;
  courseVersion: number;
  userId: string;
  progressPercent: number;
  status: "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED" | "OVERDUE";
  lastLessonId?: string | null;
  lastPosition?: string | null;
  totalLearningSeconds: number;
  lastAccessedAt?: string | null;
  completedAt?: string | null;
  lessons?: LessonProgress[];
};

export type UserAccount = {
  id: string;
  code: string;
  username: string;
  fullName: string;
  email?: string | null;
  organizationUnitId?: string | null;
  status: string;
  roles: string[];
  permissions?: string[];
};

export type Question = {
  id: string;
  type: "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE" | "SHORT_TEXT" | "ESSAY";
  prompt: string;
  options: string[];
  correctAnswers: string[];
  explanation?: string | null;
  difficulty: number;
  tags: string[];
  defaultPoints: number;
  status: "DRAFT" | "ACTIVE" | "ARCHIVED";
  version: number;
};

export type ExamQuestion = {
  id: string;
  type: Question["type"];
  prompt: string;
  options: string[];
  points: number;
  sortOrder: number;
};

export type Exam = {
  id: string;
  title: string;
  courseId?: string | null;
  lessonId?: string | null;
  contextType?: "COURSE_QUIZ" | "COURSE_ASSIGNMENT" | "STANDALONE_EXAM" | "COMPETITION";
  cohortId?: string | null;
  autoGrade?: boolean;
  durationMinutes: number;
  opensAt?: string | null;
  closesAt?: string | null;
  maxAttempts: number;
  waitMinutesBetweenAttempts: number;
  passingScore: number;
  shuffleQuestions: boolean;
  shuffleAnswers: boolean;
  scoreStrategy: "HIGHEST" | "LATEST" | "AVERAGE";
  status: "DRAFT" | "ACTIVE" | "INACTIVE" | "ARCHIVED";
  version: number;
  questions: ExamQuestion[];
};

export type ExamSession = {
  id: string;
  examId: string;
  enrollmentId?: string | null;
  courseId?: string | null;
  lessonId?: string | null;
  attemptNo: number;
  status: "IN_PROGRESS" | "SUBMITTED" | "EXPIRED" | "GRADED";
  startedAt: string;
  expiresAt: string;
  graceUntil?: string;
  remainingSeconds: number;
  lastHeartbeatAt?: string;
  suspiciousEventCount?: number;
  submittedAt?: string | null;
  answers: Record<string, unknown>;
  questions: ExamQuestion[];
};

export type Grade = {
  id: string;
  sessionId: string;
  examId: string;
  enrollmentId?: string | null;
  courseId?: string | null;
  lessonId?: string | null;
  userId: string;
  score: number;
  maxScore: number;
  percentage: number;
  passed: boolean;
  status: "PENDING_MANUAL" | "COMPLETED";
  details: Array<{ questionId: string; type: string; prompt?: string; answer?: unknown; awarded: number; maximum: number; requiresManual: boolean }>;
  feedback?: string | null;
  updatedAt: string;
};


export type AssignmentSubmission = {
  id: string;
  enrollmentId: string;
  courseId: string;
  courseVersion: number;
  lessonId: string;
  userId: string;
  attemptNumber: number;
  fileId: string;
  comment?: string | null;
  submittedAt: string;
  late: boolean;
  status: "SUBMITTED" | "GRADED" | "RETURNED";
  score?: number | null;
  maxScore?: number | null;
  feedback?: string | null;
  gradedBy?: string | null;
  gradedAt?: string | null;
};

export type StoredFile = {
  id: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  purpose: string;
  status: string;
  createdAt: string;
};

export type NotificationSummary = {
  unread: number;
  items: Array<{ id: string; title: string; body: string; read: boolean; createdAt: string }>;
};

export function formatDate(value?: string | null, includeTime = false): string {
  if (!value) return "Chưa xác định";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Chưa xác định";
  return new Intl.DateTimeFormat("vi-VN", includeTime
    ? { dateStyle: "medium", timeStyle: "short" }
    : { dateStyle: "medium" }).format(date);
}

export function formatDuration(minutes?: number | null): string {
  if (!minutes) return "Chưa đặt";
  if (minutes < 60) return `${minutes} phút`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest ? `${hours} giờ ${rest} phút` : `${hours} giờ`;
}

export function roleLabel(roles: string[]): string {
  if (!roles.length) return "Chưa có gói quyền";
  return roles.map((role) => role.replaceAll("_", " ")).join(" · ");
}

/** @deprecated Dùng accountType và permission hiệu lực thay cho role chính. */
export function primaryRole(roles: string[]): "USER" | "POWER_USER" {
  return roles.length > 1 ? "POWER_USER" : "USER";
}
