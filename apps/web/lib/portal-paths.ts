import type { PortalRole } from "./role";

export const PORTAL_PATHS = {
  ADMIN: {
    home: "/admin",
    users: "/admin/users",
    userImport: "/admin/users/import",
    organization: "/admin/organization",
    reports: "/admin/reports",
    settings: "/admin/settings",
  },
  INSTRUCTOR: {
    home: "/instructor",
    courses: "/instructor/courses",
    exams: "/instructor/exams",
    grading: "/instructor/grading",
    reports: "/instructor/reports",
  },
  STUDENT: {
    home: "/student",
    courses: "/student/courses",
    exams: "/student/exams",
    results: "/student/results",
    certificates: "/student/certificates",
  },
} as const;

export function roleHome(role: PortalRole): string {
  return PORTAL_PATHS[role].home;
}

export function instructorCoursePath(courseId?: string): string {
  return courseId
    ? `${PORTAL_PATHS.INSTRUCTOR.courses}/${courseId}`
    : PORTAL_PATHS.INSTRUCTOR.courses;
}

export function instructorCourseAssessmentPath(
  courseId: string,
  examId: string,
): string {
  return `${instructorCoursePath(courseId)}/assessments/${examId}`;
}

export function studentCoursePath(enrollmentId?: string): string {
  return enrollmentId
    ? `${PORTAL_PATHS.STUDENT.courses}/${enrollmentId}`
    : PORTAL_PATHS.STUDENT.courses;
}

export function studentCourseQuizPath(
  enrollmentId: string,
  examId: string,
): string {
  return `${studentCoursePath(enrollmentId)}/quiz/${examId}`;
}

export function standaloneExamPath(role: PortalRole, examId?: string): string {
  const base =
    role === "INSTRUCTOR"
      ? PORTAL_PATHS.INSTRUCTOR.exams
      : PORTAL_PATHS.STUDENT.exams;
  return examId ? `${base}/${examId}` : base;
}
