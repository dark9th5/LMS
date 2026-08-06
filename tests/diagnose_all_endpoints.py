import urllib.request
import json
import ssl

BASE_URL = "http://localhost:8080"

def login(username, password):
    url = f"{BASE_URL}/api/v1/auth/login"
    payload = json.dumps({"username": username, "password": password}).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data["accessToken"]
    except Exception as e:
        print(f"FAILED LOGIN {username}: {e}")
        if hasattr(e, "read"):
            print("  Body:", e.read().decode("utf-8", errors="ignore"))
        return None

def test_endpoint(token, name, path, method="GET", body=None):
    url = f"{BASE_URL}{path}"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    payload = json.dumps(body).encode("utf-8") if body else None
    req = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read().decode("utf-8", errors="ignore")
            print(f"[OK {status}] {name} ({path}) -> {len(content)} bytes")
    except Exception as e:
        status = getattr(e, "code", "ERR")
        body_text = e.read().decode("utf-8", errors="ignore") if hasattr(e, "read") else str(e)
        print(f"[FAIL {status}] {name} ({path}) -> {body_text}")

print("=== DIAGNOSING ADMIN ENDPOINTS ===")
admin_token = login("admin", "admin123")
if admin_token:
    test_endpoint(admin_token, "Branding", "/public/v1/branding")
    test_endpoint(admin_token, "Users", "/api/v1/users")
    test_endpoint(admin_token, "Organization Units", "/api/v1/organization/units")
    test_endpoint(admin_token, "Organization Memberships", "/api/v1/organization/memberships")
    test_endpoint(admin_token, "Categories", "/api/v1/categories")
    test_endpoint(admin_token, "Courses", "/api/v1/courses")
    test_endpoint(admin_token, "Reports KPIs", "/api/v1/reports/kpis")
    test_endpoint(admin_token, "Reports System", "/api/v1/reports")
    test_endpoint(admin_token, "License Info", "/api/v1/license")
    test_endpoint(admin_token, "Audit Logs", "/api/v1/audit")
    test_endpoint(admin_token, "Notifications", "/api/v1/notifications")
    test_endpoint(admin_token, "News", "/api/v1/news")
    test_endpoint(admin_token, "External Services", "/api/v1/external-services")

print("\n=== DIAGNOSING INSTRUCTOR ENDPOINTS ===")
instructor_token = login("instructor", "instructor123")
if instructor_token:
    test_endpoint(instructor_token, "Instructor Courses", "/api/v1/courses")
    test_endpoint(instructor_token, "Instructor Questions", "/api/v1/questions")
    test_endpoint(instructor_token, "Instructor Exams", "/api/v1/exams")
    test_endpoint(instructor_token, "Instructor Competitions", "/api/v1/competitions")
    test_endpoint(instructor_token, "Instructor Grades", "/api/v1/grades")

print("\n=== DIAGNOSING STUDENT ENDPOINTS ===")
student_token = login("student", "student123")
if student_token:
    test_endpoint(student_token, "Student Course Assignments", "/api/v1/course-assignments")
    test_endpoint(student_token, "Student Learning Paths", "/api/v1/learning-paths")
    test_endpoint(student_token, "Student Exam Assignments", "/api/v1/assessment-assignments")
    test_endpoint(student_token, "Student Grades", "/api/v1/grades")
    test_endpoint(student_token, "Student Competencies", "/api/v1/competencies")
