#!/usr/bin/env node
import process from "node:process";

const baseUrl = (process.env.LMSPILOT_WEB_URL ?? "http://localhost:3000").replace(/\/+$/, "");
const username = process.env.LMSPILOT_SMOKE_USERNAME;
const password = process.env.LMSPILOT_SMOKE_PASSWORD;
if (!username || !password) {
  console.error("Thiếu LMSPILOT_SMOKE_USERNAME hoặc LMSPILOT_SMOKE_PASSWORD của tài khoản Admin/Giảng viên.");
  process.exit(2);
}

let cookie = "";
const created = { courseId: null, lessonId: null, questionId: null, examId: null };

async function request(path, init = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15_000);
  try {
    const response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers: {
        ...(init.body && !(init.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
        ...(cookie ? { Cookie: cookie } : {}),
        ...(init.headers ?? {}),
      },
      signal: controller.signal,
      redirect: "manual",
    });
    const text = await response.text();
    const body = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
    if (!response.ok) {
      throw new Error(`${init.method ?? "GET"} ${path} -> ${response.status}: ${typeof body === "string" ? body : JSON.stringify(body)}`);
    }
    return { response, body };
  } finally {
    clearTimeout(timer);
  }
}

function expect(condition, message) {
  if (!condition) throw new Error(message);
}

async function cleanup() {
  const actions = [
    created.examId && ["exam", `/api/gateway/api/v1/exams/${created.examId}`],
    created.questionId && ["question", `/api/gateway/api/v1/questions/${created.questionId}`],
    created.courseId && ["course", `/api/gateway/api/v1/courses/${created.courseId}`],
  ].filter(Boolean);
  for (const [label, path] of actions) {
    try { await request(path, { method: "DELETE" }); }
    catch (error) { console.warn(`Cleanup ${label} chưa hoàn tất: ${error.message}`); }
  }
}

try {
  const login = await fetch(`${baseUrl}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const loginBody = await login.json().catch(() => null);
  expect(login.ok, `Đăng nhập thất bại: ${login.status} ${JSON.stringify(loginBody)}`);
  expect(loginBody?.user?.permissions?.includes("courses:write"), "Tài khoản smoke test thiếu courses:write");
  expect(loginBody?.user?.permissions?.includes("assessment:manage"), "Tài khoản smoke test thiếu assessment:manage");
  const setCookies = typeof login.headers.getSetCookie === "function" ? login.headers.getSetCookie() : [login.headers.get("set-cookie")].filter(Boolean);
  cookie = setCookies.map((value) => value.split(";", 1)[0]).join("; ");
  expect(cookie.includes("lmspilot_access="), "Không nhận được access cookie thật");
  console.log("PASS login thật và quyền quản lý");

  const sampleCourse = (await request("/api/gateway/api/v1/courses/00000000-0000-0000-0000-000000000101")).body;
  expect(sampleCourse.name === "Bài 0 - Làm quen với LMSPilot", "Khóa học mẫu Bài 0 chưa được seed");
  const sampleTypes = new Set((sampleCourse.lessons ?? []).map((item) => item.type));
  for (const type of ["TEXT", "VIDEO", "PDF", "DOCX", "ASSIGNMENT", "EXAM"]) expect(sampleTypes.has(type), `Bài 0 thiếu loại ${type}`);
  for (const id of ["00000000-0000-0000-0000-000000000121", "00000000-0000-0000-0000-000000000122", "00000000-0000-0000-0000-000000000123"]) {
    const metadata = (await request(`/api/gateway/api/v1/files/${id}`)).body;
    expect(metadata.sizeBytes > 1000, `Tệp mẫu ${id} không có dữ liệu thật`);
  }
  const sampleExam = (await request("/api/gateway/api/v1/exams/00000000-0000-0000-0000-000000000303")).body;
  expect(sampleExam.questions?.length >= 5, "Bài kiểm tra mẫu chưa đủ 5 câu");
  console.log("PASS Bài 0, video/PDF/DOCX và bài kiểm tra mẫu");

  const stamp = Date.now().toString(36).toUpperCase();
  const course = (await request("/api/gateway/api/v1/courses", {
    method: "POST",
    body: JSON.stringify({
      code: `SMOKE-${stamp}`,
      name: "Khóa học kiểm thử CRUD",
      description: "Tạo tự động bởi scripts/smoke-lms-crud.mjs",
      objectives: "Xác nhận dữ liệu được lưu thật",
      targetAudience: "Kiểm thử",
      durationMinutes: 20,
      passingScore: 70,
      completionPolicyJson: "{\"requiredLessonPercent\":100}",
      categoryId: null,
    }),
  })).body;
  created.courseId = course.id;

  const updatedCourse = (await request(`/api/gateway/api/v1/courses/${course.id}`, {
    method: "PUT",
    body: JSON.stringify({
      code: course.code,
      name: "Khóa học kiểm thử CRUD - đã sửa",
      description: course.description,
      objectives: course.objectives,
      targetAudience: course.targetAudience,
      durationMinutes: 25,
      passingScore: 75,
      completionPolicyJson: course.completionPolicyJson,
      categoryId: null,
    }),
  })).body;
  expect(updatedCourse.name.endsWith("đã sửa"), "Sửa khóa học không được lưu");

  const lesson = (await request(`/api/gateway/api/v1/courses/${course.id}/lessons`, {
    method: "POST",
    body: JSON.stringify({ title: "Bài kiểm thử", type: "TEXT", textContent: "Dữ liệu thật", fileId: null, required: true, sortOrder: 1, estimatedMinutes: 5 }),
  })).body;
  created.lessonId = lesson.id;
  const readCourse = (await request(`/api/gateway/api/v1/courses/${course.id}`)).body;
  expect(readCourse.lessons.some((item) => item.id === lesson.id && item.textContent === "Dữ liệu thật"), "Bài học không tồn tại sau khi đọc lại");
  await request(`/api/gateway/api/v1/courses/${course.id}/lessons/${lesson.id}`, { method: "DELETE" });
  created.lessonId = null;
  const afterDelete = (await request(`/api/gateway/api/v1/courses/${course.id}`)).body;
  expect(!afterDelete.lessons.some((item) => item.id === lesson.id), "Xóa bài học không có hiệu lực");
  console.log("PASS tạo/sửa/đọc/xóa bài học và khóa học");

  const question = (await request("/api/gateway/api/v1/questions", {
    method: "POST",
    body: JSON.stringify({ type: "SINGLE_CHOICE", prompt: "Smoke test chọn gì?", options: ["A", "B"], correctAnswers: ["A"], explanation: "A", difficulty: 1, tags: ["smoke"], defaultPoints: 1 }),
  })).body;
  created.questionId = question.id;
  const updatedQuestion = (await request(`/api/gateway/api/v1/questions/${question.id}`, {
    method: "PUT",
    body: JSON.stringify({ type: "SINGLE_CHOICE", prompt: "Smoke test đã sửa?", options: ["Có", "Không"], correctAnswers: ["Có"], explanation: "Có", difficulty: 1, tags: ["smoke"], defaultPoints: 2 }),
  })).body;
  expect(updatedQuestion.prompt.includes("đã sửa"), "Sửa câu hỏi không được lưu");

  const exam = (await request("/api/gateway/api/v1/exams", {
    method: "POST",
    body: JSON.stringify({ title: "Đề smoke CRUD", courseId: course.id, lessonId: null, durationMinutes: 5, opensAt: null, closesAt: null, maxAttempts: 1, waitMinutesBetweenAttempts: 0, passingScore: 70, shuffleQuestions: false, shuffleAnswers: false, scoreStrategy: "HIGHEST", status: "DRAFT", questions: [{ questionId: question.id, points: 2, sortOrder: 1 }] }),
  })).body;
  created.examId = exam.id;
  const updatedExam = (await request(`/api/gateway/api/v1/exams/${exam.id}`, {
    method: "PUT",
    body: JSON.stringify({ title: "Đề smoke CRUD - đã sửa", courseId: course.id, lessonId: null, durationMinutes: 7, opensAt: null, closesAt: null, maxAttempts: 2, waitMinutesBetweenAttempts: 0, passingScore: 75, shuffleQuestions: false, shuffleAnswers: false, scoreStrategy: "HIGHEST", status: "ACTIVE", questions: [{ questionId: question.id, points: 2, sortOrder: 1 }] }),
  })).body;
  expect(updatedExam.title.endsWith("đã sửa") && updatedExam.status === "ACTIVE", "Sửa bài kiểm tra không được lưu");
  console.log("PASS tạo/sửa/lưu trữ câu hỏi và bài kiểm tra");

  await cleanup();
  console.log("LMS CRUD smoke test: PASSED");
} catch (error) {
  console.error(`LMS CRUD smoke test: FAILED\n${error.stack ?? error}`);
  await cleanup();
  process.exit(1);
}
