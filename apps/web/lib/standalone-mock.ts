import type { Course, TrainingClass, Exam, UserAccount, PageResponse, Grade } from "./models";
import type { PortalUser } from "./types";

export const MOCK_COURSES: Course[] = [
  {
    id: "crs-01",
    code: "SEC-101",
    name: "Kiến thức An toàn thông tin & Bảo mật dữ liệu doanh nghiệp",
    description: "Khóa học chuẩn hóa nhận thức an toàn thông tin cho toàn bộ cán bộ nhân viên trong tổ chức.",
    objectives: "Nắm vững nguyên tắc CIA, nhận biết Phishing, quản lý mật khẩu an toàn và tuân thủ quy định bảo mật.",
    targetAudience: "Tất cả nhân viên, giảng viên và quản trị viên.",
    durationMinutes: 120,
    passingScore: 70,
    completionPolicyJson: JSON.stringify({ requireAllLessons: true, minExamScore: 70 }),
    status: "PUBLISHED",
    contentVersion: 1,
    publishedAt: "2026-07-01T08:00:00Z",
    ownerId: "usr-instructor-01",
    lessons: [
      {
        id: "lsn-01",
        title: "Tổng quan về an toàn thông tin trong tổ chức",
        type: "TEXT",
        textContent: "# Bài 1: Tổng quan An toàn thông tin\n\nAn toàn thông tin (Information Security) đóng vai trò then chốt trong việc bảo vệ tài sản tri thức của doanh nghiệp.\n\n### 3 Trụ cột cốt lõi (Mô hình CIA):\n1. **Tính bảo mật (Confidentiality)**: Đảm bảo thông tin chỉ được truy cập bởi đúng người có thẩm quyền.\n2. **Tính toàn vẹn (Integrity)**: Bảo đảm dữ liệu không bị sửa đổi trái phép hoặc làm sai lệch.\n3. **Tính khả dụng (Availability)**: Đảm bảo hệ thống và dữ liệu luôn sẵn sàng phục vụ khi cần.\n\n### Quy tắc vàng khi làm việc:\n- Không chia sẻ tài khoản đăng nhập cho bất kỳ ai.\n- Khóa màn hình (`Win + L`) ngay khi rời vị trí làm việc.\n- Báo cáo lập tức cho Bộ phận IT/An ninh mạng khi phát hiện dấu hiệu bất thường.",
        required: true,
        sortOrder: 1,
        estimatedMinutes: 15,
      },
      {
        id: "lsn-02",
        title: "Quy định bảo mật & Tài liệu hướng dẫn an toàn",
        type: "PDF",
        fileId: "file-sec-guidelines",
        textContent: "Tài liệu hướng dẫn an toàn thông tin nội bộ phiên bản 2026.",
        required: true,
        sortOrder: 2,
        estimatedMinutes: 20,
      },
      {
        id: "lsn-03",
        title: "Bài giảng video: Nhận diện & Phòng chống Phishing",
        type: "VIDEO",
        fileId: "file-phishing-video",
        textContent: "Video hướng dẫn chi tiết cách kiểm tra email giả mạo, liên kết độc hại và mã độc đính kèm.",
        required: true,
        sortOrder: 3,
        estimatedMinutes: 30,
      },
      {
        id: "lsn-04",
        title: "Bài tập nộp tệp: Đánh giá nguy cơ an toàn thông tin phòng ban",
        type: "ASSIGNMENT",
        textContent: "Hãy liệt kê 3 rủi ro an toàn thông tin lớn nhất tại phòng ban của bạn và đề xuất giải pháp khắc phục. Nộp báo cáo dạng PDF hoặc Word.",
        required: false,
        sortOrder: 4,
        estimatedMinutes: 25,
      },
      {
        id: "lsn-05",
        title: "Bài kiểm tra tổng hợp kiến thức An toàn thông tin",
        type: "EXAM",
        textContent: "Bài thi trắc nghiệm và tự luận 30 phút đánh giá năng lực bảo mật.",
        required: true,
        sortOrder: 5,
        estimatedMinutes: 30,
      },
    ],
  },
  {
    id: "crs-02",
    code: "OPS-201",
    name: "Quy trình vận hành LMSPilot & Quản lý đào tạo LAN",
    description: "Hướng dẫn khai thác và vận hành hệ thống LMS cho giảng viên và quản trị viên.",
    objectives: "Quản lý khóa học, tổ chức lớp học, chấm bài tự luận và xuất báo cáo đào tạo.",
    targetAudience: "Giảng viên và Quản trị viên hệ thống.",
    durationMinutes: 90,
    passingScore: 80,
    completionPolicyJson: JSON.stringify({ requireAllLessons: true, minExamScore: 80 }),
    status: "PUBLISHED",
    contentVersion: 1,
    publishedAt: "2026-07-10T09:00:00Z",
    ownerId: "usr-admin-01",
    lessons: [
      {
        id: "lsn-11",
        title: "Hướng dẫn tạo khóa học & Đăng tải tài liệu",
        type: "TEXT",
        textContent: "# Hướng dẫn vận hành LMSPilot\n\n### Các bước tạo khóa học mới:\n1. Truy cập trang **Quản lý khóa học**.\n2. Nhấn nút **Tạo khóa học mới** và điền Mã khóa học, Tên khóa học và Điểm đạt.\n3. Thêm các bài học dạng Văn bản, PDF, Video hoặc Bài thi.\n4. Đổi trạng thái sang **Chuyển sang Xuất bản (PUBLISHED)**.",
        required: true,
        sortOrder: 1,
        estimatedMinutes: 20,
      },
      {
        id: "lsn-12",
        title: "Quy trình ghi danh & Quản lý lớp học",
        type: "PDF",
        fileId: "file-ops-manual",
        textContent: "Sổ tay quy trình ghi danh học viên theo cơ cấu tổ chức.",
        required: true,
        sortOrder: 2,
        estimatedMinutes: 25,
      },
    ],
  },
  {
    id: "crs-03",
    code: "AGL-301",
    name: "Kỹ năng Quản lý dự án Công nghệ theo mô hình Agile/Scrum",
    description: "Khóa học nâng cao năng lực lập kế hoạch, theo dõi tiến độ và tối ưu hóa năng suất làm việc nhóm.",
    objectives: "Áp dụng Scrum framework, tổ chức Sprint và đánh giá hiệu quả phát triển phần mềm.",
    targetAudience: "Quản lý dự án, trưởng nhóm kỹ thuật.",
    durationMinutes: 180,
    passingScore: 75,
    completionPolicyJson: JSON.stringify({ requireAllLessons: false, minExamScore: 75 }),
    status: "DRAFT",
    contentVersion: 1,
    publishedAt: null,
    ownerId: "usr-instructor-01",
    lessons: [],
  },
];

export const MOCK_CLASSES: TrainingClass[] = [
  {
    id: "cls-01",
    code: "CLS-SEC-2026Q3",
    name: "Lớp An toàn thông tin Q3/2026 - Đợt 1",
    courseId: "crs-01",
    courseVersion: 1,
    startsAt: "2026-07-01T00:00:00Z",
    endsAt: "2026-08-31T23:59:59Z",
    dueAt: "2026-08-31T23:59:59Z",
    instructorIds: ["usr-instructor-01"],
    status: "OPEN",
  },
  {
    id: "cls-02",
    code: "CLS-OPS-K15",
    name: "Lớp Vận hành Đào tạo Nội bộ K15",
    courseId: "crs-02",
    courseVersion: 1,
    startsAt: "2026-07-15T00:00:00Z",
    endsAt: "2026-09-15T23:59:59Z",
    dueAt: "2026-09-15T23:59:59Z",
    instructorIds: ["usr-admin-01", "usr-instructor-01"],
    status: "OPEN",
  },
];

export const MOCK_USERS: PortalUser[] = [
  {
    id: "usr-admin-01",
    code: "ADM-001",
    username: "admin",
    fullName: "Nguyễn Văn Quản Trị",
    email: "admin@lmspilot.local",
    organizationUnitId: "ou-01",
    status: "ACTIVE",
    roles: ["ADMIN"],
    permissions: ["*"],
  },
  {
    id: "usr-instructor-01",
    code: "INST-001",
    username: "instructor",
    fullName: "Trần Thị Giảng Viên",
    email: "instructor@lmspilot.local",
    organizationUnitId: "ou-02",
    status: "ACTIVE",
    roles: ["INSTRUCTOR"],
    permissions: ["course:write", "class:write", "exam:grade"],
  },
  {
    id: "usr-student-01",
    code: "STU-001",
    username: "student",
    fullName: "Lê Văn Học Viên",
    email: "student@lmspilot.local",
    organizationUnitId: "ou-03",
    status: "ACTIVE",
    roles: ["STUDENT"],
    permissions: ["course:read", "exam:take"],
  },
  {
    id: "usr-student-02",
    code: "STU-002",
    username: "nam.phong",
    fullName: "Phạm Hoàng Nam",
    email: "nam.phong@lmspilot.local",
    organizationUnitId: "ou-03",
    status: "ACTIVE",
    roles: ["STUDENT"],
    permissions: ["course:read"],
  },
  {
    id: "usr-student-03",
    code: "STU-003",
    username: "mai.do",
    fullName: "Đỗ Thị Mai",
    email: "mai.do@lmspilot.local",
    organizationUnitId: "ou-03",
    status: "ACTIVE",
    roles: ["STUDENT"],
    permissions: ["course:read"],
  },
];

export const MOCK_EXAM: Exam = {
  id: "exm-01",
  title: "Bài kiểm tra tổng hợp kiến thức An toàn thông tin 2026",
  courseId: "crs-01",
  durationMinutes: 30,
  opensAt: "2026-07-01T00:00:00Z",
  closesAt: "2026-12-31T23:59:59Z",
  maxAttempts: 3,
  passingScore: 70,
  status: "ACTIVE",
  version: 1,
  questions: [
    {
      id: "q-01",
      type: "SINGLE_CHOICE",
      prompt: "Hành động nào sau đây là hiệu quả nhất để phòng chống tấn công lừa đảo (Phishing) qua Email?",
      options: [
        "A. Kiểm tra kỹ địa chỉ email người gửi và không nhấp vào liên kết lạ",
        "B. Tắt phần mềm diệt virus trên máy tính",
        "C. Đổi tên tệp đính kèm trước khi mở",
        "D. Chỉ sử dụng mạng Wi-Fi công cộng không có mật khẩu"
      ],
      points: 25,
      sortOrder: 1,
    },
    {
      id: "q-02",
      type: "TRUE_FALSE",
      prompt: "Mật khẩu an toàn chuẩn doanh nghiệp bắt buộc chứa tối thiểu 12 ký tự gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.",
      options: ["Đúng", "Sai"],
      points: 25,
      sortOrder: 2,
    },
    {
      id: "q-03",
      type: "MULTIPLE_CHOICE",
      prompt: "Các nguyên tắc cấu thành mô hình bảo mật CIA bao gồm những yếu tố nào? (Chọn các đáp án đúng)",
      options: [
        "A. Tính bảo mật (Confidentiality)",
        "B. Tính toàn vẹn (Integrity)",
        "C. Tính khả dụng (Availability)",
        "D. Tính phức tạp (Complexity)"
      ],
      points: 25,
      sortOrder: 3,
    },
    {
      id: "q-04",
      type: "ESSAY",
      prompt: "Hãy mô tả 3 bước ứng phó sự cố ban đầu bạn sẽ thực hiện ngay lập tức khi phát hiện máy tính cá nhân bị nghi ngờ nhiễm mã độc (Malware).",
      options: [],
      points: 25,
      sortOrder: 4,
    },
  ],
};

export const MOCK_GRADES: Grade[] = [
  {
    id: "grd-01",
    sessionId: "ses-101",
    examId: "exm-01",
    courseId: "crs-01",
    userId: "usr-student-01",
    score: 85,
    maxScore: 100,
    percentage: 85,
    passed: true,
    status: "COMPLETED",
    details: [
      { questionId: "q-01", type: "SINGLE_CHOICE", awarded: 25, maximum: 25, requiresManual: false },
      { questionId: "q-02", type: "TRUE_FALSE", awarded: 25, maximum: 25, requiresManual: false },
      { questionId: "q-03", type: "MULTIPLE_CHOICE", awarded: 25, maximum: 25, requiresManual: false },
      { questionId: "q-04", type: "ESSAY", awarded: 10, maximum: 25, requiresManual: true },
    ],
    feedback: "Bài tự luận phân tích tương đối tốt các bước cô lập mạng.",
    updatedAt: "2026-07-25T14:30:00Z",
  },
  {
    id: "grd-02",
    sessionId: "ses-102",
    examId: "exm-01",
    courseId: "crs-01",
    userId: "usr-student-02",
    score: 50,
    maxScore: 100,
    percentage: 50,
    passed: false,
    status: "PENDING_MANUAL",
    details: [
      { questionId: "q-01", type: "SINGLE_CHOICE", awarded: 25, maximum: 25, requiresManual: false },
      { questionId: "q-02", type: "TRUE_FALSE", awarded: 25, maximum: 25, requiresManual: false },
      { questionId: "q-03", type: "MULTIPLE_CHOICE", awarded: 0, maximum: 25, requiresManual: false },
      { questionId: "q-04", type: "ESSAY", awarded: 0, maximum: 25, requiresManual: true },
    ],
    feedback: null,
    updatedAt: "2026-07-28T10:15:00Z",
  },
];

export function handleMockGatewayRequest(pathParts: string[], method: string, bodyText?: string): Response {
  const fullPath = pathParts.join("/");

  // Courses
  if (fullPath === "api/v1/courses" || fullPath === "api/v1/courses/search") {
    if (method === "POST" && bodyText) {
      try {
        const payload = JSON.parse(bodyText);
        const newCourse: Course = {
          id: `crs-${Date.now().toString(36)}`,
          code: payload.code || `CRS-${Math.floor(100 + Math.random() * 900)}`,
          name: payload.name || "Khóa học mới",
          description: payload.description || "",
          objectives: payload.objectives || "",
          targetAudience: payload.targetAudience || "",
          durationMinutes: payload.durationMinutes || 60,
          passingScore: payload.passingScore || 70,
          completionPolicyJson: JSON.stringify({ requireAllLessons: true }),
          status: "DRAFT",
          contentVersion: 1,
          publishedAt: null,
          ownerId: "usr-instructor-01",
          lessons: [],
        };
        MOCK_COURSES.unshift(newCourse);
        return Response.json(newCourse, { status: 201 });
      } catch {}
    }
    const pageResp: PageResponse<Course> = {
      items: MOCK_COURSES,
      page: 0,
      size: 20,
      totalElements: MOCK_COURSES.length,
      totalPages: 1,
    };
    return Response.json(pageResp);
  }

  if (fullPath.startsWith("api/v1/courses/")) {
    const courseId = pathParts[3];
    const course = MOCK_COURSES.find((c) => c.id === courseId || c.code === courseId) || MOCK_COURSES[0];
    if (pathParts.length === 5 && pathParts[4] === "lessons" && method === "POST" && bodyText) {
      try {
        const payload = JSON.parse(bodyText);
        const newLesson = {
          id: `lsn-${Date.now().toString(36)}`,
          title: payload.title || "Bài học mới",
          type: payload.type || "TEXT",
          textContent: payload.textContent || "Nội dung bài học...",
          required: payload.required ?? true,
          sortOrder: (course.lessons?.length || 0) + 1,
          estimatedMinutes: payload.estimatedMinutes || 15,
        };
        course.lessons = course.lessons || [];
        course.lessons.push(newLesson);
        return Response.json(newLesson, { status: 201 });
      } catch {}
    }
    if (pathParts.length === 5 && pathParts[4] === "publish") {
      course.status = "PUBLISHED";
      course.publishedAt = new Date().toISOString();
      return Response.json(course);
    }
    return Response.json(course);
  }

  // Classes
  if (fullPath === "api/v1/classes" || fullPath === "api/v1/classes/search") {
    if (method === "POST" && bodyText) {
      try {
        const payload = JSON.parse(bodyText);
        const newClass: TrainingClass = {
          id: `cls-${Date.now().toString(36)}`,
          code: payload.code || `CLS-${Math.floor(100 + Math.random() * 900)}`,
          name: payload.name || "Lớp học mới",
          courseId: payload.courseId || "crs-01",
          courseVersion: 1,
          startsAt: payload.startsAt || new Date().toISOString(),
          endsAt: payload.endsAt || new Date(Date.now() + 30 * 86400000).toISOString(),
          dueAt: payload.dueAt || new Date(Date.now() + 30 * 86400000).toISOString(),
          instructorIds: ["usr-instructor-01"],
          status: "OPEN",
        };
        MOCK_CLASSES.unshift(newClass);
        return Response.json(newClass, { status: 201 });
      } catch {}
    }
    return Response.json({ items: MOCK_CLASSES, page: 0, size: 20, totalElements: MOCK_CLASSES.length, totalPages: 1 });
  }

  if (fullPath.startsWith("api/v1/classes/")) {
    const classId = pathParts[3];
    const cls = MOCK_CLASSES.find((c) => c.id === classId) || MOCK_CLASSES[0];
    return Response.json(cls);
  }

  // Enrollments & Progress
  if (fullPath.includes("enrollments") || fullPath.includes("learning") || fullPath.includes("progress")) {
    return Response.json({
      enrollmentId: "enr-01",
      courseId: "crs-01",
      userId: "usr-student-01",
      progressPercent: 60,
      status: "IN_PROGRESS",
      lastLessonId: "lsn-01",
      totalLearningSeconds: 1800,
      lessons: [
        { lessonId: "lsn-01", completed: true, learningSeconds: 900, completedAt: "2026-07-20T10:00:00Z" },
        { lessonId: "lsn-02", completed: true, learningSeconds: 900, completedAt: "2026-07-22T11:00:00Z" },
        { lessonId: "lsn-03", completed: false, learningSeconds: 0 },
      ],
    });
  }

  // Users
  if (fullPath === "api/v1/users" || fullPath === "api/v1/users/search") {
    return Response.json({ items: MOCK_USERS, page: 0, size: 20, totalElements: MOCK_USERS.length, totalPages: 1 });
  }
  if (fullPath.startsWith("api/v1/users/me")) {
    return Response.json(MOCK_USERS[0]);
  }

  // Exams & Sessions
  if (fullPath.includes("exams") || fullPath.includes("exam-sessions")) {
    if (fullPath.includes("start") || fullPath.includes("sessions")) {
      return Response.json({
        id: `ses-${Date.now().toString(36)}`,
        examId: "exm-01",
        attemptNo: 1,
        status: "IN_PROGRESS",
        startedAt: new Date().toISOString(),
        expiresAt: new Date(Date.now() + 30 * 60000).toISOString(),
        answers: {},
        questions: MOCK_EXAM.questions,
      });
    }
    if (fullPath.includes("submit")) {
      return Response.json(MOCK_GRADES[0]);
    }
    return Response.json({ items: [MOCK_EXAM], page: 0, size: 20, totalElements: 1, totalPages: 1 });
  }

  // Grading Queue & Grades
  if (fullPath.includes("grades") || fullPath.includes("grading")) {
    return Response.json({ items: MOCK_GRADES, page: 0, size: 20, totalElements: MOCK_GRADES.length, totalPages: 1 });
  }

  // Reports
  if (fullPath.includes("reports") || fullPath.includes("dashboard")) {
    return Response.json({
      totalCourses: MOCK_COURSES.length,
      activeClasses: MOCK_CLASSES.length,
      enrolledStudents: 24,
      passRatePercent: 92.5,
      completionRatePercent: 88.0,
      monthlyEnrollments: [12, 18, 24, 30, 28, 35],
    });
  }

  // Notifications
  if (fullPath.includes("notifications")) {
    return Response.json({
      unread: 2,
      items: [
        { id: "notif-01", title: "Chào mừng đến với LMSPilot 0.4", body: "Hệ thống LMS On-Premise đã sẵn sàng vận hành.", read: false, createdAt: new Date().toISOString() },
        { id: "notif-02", title: "Nhắc nhở học tập", body: "Bạn có 1 bài kiểm tra An toàn thông tin sắp đến hạn nộp.", read: false, createdAt: new Date().toISOString() },
      ],
    });
  }

  // Files
  if (fullPath.includes("files")) {
    return Response.json({
      id: `file-${Date.now().toString(36)}`,
      originalName: "tai-lieu-huong-dan.pdf",
      contentType: "application/pdf",
      sizeBytes: 1048576,
      sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      purpose: "DOCUMENT",
      status: "READY",
      createdAt: new Date().toISOString(),
    });
  }

  // Default fallback response
  return Response.json({ ok: true, message: "Standby mock data endpoint" });
}
