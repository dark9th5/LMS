package com.lmspilot.contracts

/**
 * Stable permission vocabulary. New code should use the granular constants;
 * legacy aliases remain until all services and the web app have migrated.
 */
object Permissions {
    const val USERS_READ = "users:read"
    const val USERS_CREATE = "users:create"
    const val USERS_UPDATE = "users:update"
    const val USERS_LOCK = "users:lock"
    const val USERS_BULK_MANAGE = "users:bulk-manage"
    const val USERS_WRITE = "users:write" // legacy alias checked by existing endpoints
    const val USERS_SESSIONS_MANAGE = "users:sessions:manage"
    const val USERS_PASSWORD_POLICY_MANAGE = "users:password-policy:manage"

    const val ROLES_READ = "roles:read"
    const val ROLES_MANAGE = "roles:manage"
    const val AUTHORIZATION_GRANT = "authorization:grant"
    const val AUTHORIZATION_REVOKE = "authorization:revoke"

    const val ORGANIZATION_READ = "organization:read"
    const val ORGANIZATION_MANAGE = "organization:manage"
    const val ORGANIZATION_MEMBERSHIP_MANAGE = "organization:membership:manage"
    const val ORGANIZATION_WRITE = "organization:write" // legacy

    const val COURSES_READ = "courses:read"
    const val COURSES_CREATE = "courses:create"
    const val COURSES_UPDATE = "courses:update"
    const val COURSES_WRITE = "courses:write" // legacy
    const val COURSES_PUBLISH = "courses:publish"
    const val COURSES_ASSIGN = "courses:assign"
    const val COURSES_LEARN = "courses:learn"
    const val COURSE_CATEGORIES_MANAGE = "course-categories:manage"
    const val DISCUSSIONS_READ = "discussions:read"
    const val DISCUSSIONS_WRITE = "discussions:write"
    const val DISCUSSIONS_MODERATE = "discussions:moderate"

    const val CLASSES_READ = "classes:read"
    const val CLASSES_WRITE = "classes:write"
    const val CLASSES_MANAGE = "classes:manage"
    const val ENROLLMENTS_WRITE = "enrollments:write"
    const val LIVE_SESSIONS_MANAGE = "live-sessions:manage"
    const val LIVE_SESSIONS_JOIN = "live-sessions:join"
    const val LEARNING_PATHS_READ = "learning-paths:read"
    const val LEARNING_PATHS_MANAGE = "learning-paths:manage"
    const val LEARNING_PATHS_ASSIGN = "learning-paths:assign"

    const val LEARNING_READ_SELF = "learning:read:self"
    const val LEARNING_WRITE_SELF = "learning:write:self"
    const val LEARNING_READ_SCOPE = "learning:read:scope"
    const val XAPI_WRITE = "xapi:write"
    const val XAPI_READ_SCOPE = "xapi:read:scope"

    const val ASSESSMENTS_READ = "assessments:read"
    const val ASSESSMENTS_CREATE = "assessments:create"
    const val ASSESSMENTS_UPDATE = "assessments:update"
    const val ASSESSMENT_MANAGE = "assessment:manage" // legacy
    const val ASSESSMENT_TAKE = "assessment:take" // legacy
    const val ASSESSMENTS_TAKE = "assessments:take"
    const val ASSESSMENTS_GRADE = "assessments:grade"
    const val GRADING_MANAGE = "grading:manage" // legacy
    const val GRADES_READ_SELF = "grades:read:self"
    const val GRADE_APPEALS_CREATE = "grade-appeals:create"
    const val GRADE_APPEALS_MANAGE = "grade-appeals:manage"

    const val EXAMS_MANAGE = "exams:manage"
    const val EXAMS_ASSIGN = "exams:assign"
    const val COMPETITIONS_MANAGE = "competitions:manage"
    const val COMPETITIONS_PARTICIPATE = "competitions:participate"
    const val COMPETITIONS_REWARD = "competitions:reward"

    const val QUESTIONS_READ = "questions:read"
    const val QUESTIONS_MANAGE = "questions:manage"
    const val QUESTIONS_GENERATE_AI = "questions:generate:ai"
    const val QUESTIONS_APPROVE_AI = "questions:approve:ai"

    const val REPORTS_READ_SELF = "reports:read:self"
    const val REPORTS_READ_SCOPE = "reports:read:scope"
    const val REPORTS_READ = "reports:read" // legacy
    const val REPORTS_EXPORT = "reports:export"
    const val REPORTS_SCHEDULE = "reports:schedule"
    const val REPORTS_KPI_READ = "reports:kpi:read"

    const val FILES_READ = "files:read"
    const val FILES_UPLOAD = "files:upload"
    const val FILES_DOWNLOAD = "files:download"
    const val FILES_EDIT = "files:edit"
    const val FILES_PUBLISH = "files:publish"
    const val FILES_VERSION_READ = "files:version:read"

    const val NEWS_READ = "news:read"
    const val NEWS_MANAGE = "news:manage"
    const val NEWS_PUBLISH = "news:publish"
    const val NOTIFICATION_TEMPLATES_MANAGE = "notification-templates:manage"
    const val NOTIFICATION_REMINDERS_MANAGE = "notification-reminders:manage"

    const val BRANDING_MANAGE = "branding:manage"
    const val CONFIGURATION_MANAGE = "configuration:manage"
    const val INTEGRATIONS_MANAGE = "integrations:manage"
    const val AI_USE = "ai:use" // legacy

    const val CERTIFICATES_MANAGE = "certificates:manage"
    const val CERTIFICATES_READ_SELF = "certificates:read:self"
    const val CERTIFICATE_TEMPLATES_MANAGE = "certificate-templates:manage"
    const val COMPETENCIES_READ_SELF = "competencies:read:self"
    const val COMPETENCIES_READ_SCOPE = "competencies:read:scope"
    const val COMPETENCIES_MANAGE = "competencies:manage"
    const val COMPETENCIES_ASSESS = "competencies:assess"
    const val AUDIT_READ = "audit:read"
    const val AUDIT_EXPORT = "audit:export"
    const val OPERATIONS_MANAGE = "operations:manage"
    const val LICENSE_MANAGE = "license:manage"
}

object DefaultRolePermissions {
    val ADMIN = setOf(
        Permissions.USERS_READ, Permissions.USERS_CREATE, Permissions.USERS_UPDATE,
        Permissions.USERS_LOCK, Permissions.USERS_BULK_MANAGE, Permissions.USERS_WRITE,
        Permissions.USERS_SESSIONS_MANAGE, Permissions.USERS_PASSWORD_POLICY_MANAGE,
        Permissions.ROLES_READ, Permissions.ROLES_MANAGE,
        Permissions.AUTHORIZATION_GRANT, Permissions.AUTHORIZATION_REVOKE,
        Permissions.ORGANIZATION_READ, Permissions.ORGANIZATION_MANAGE,
        Permissions.ORGANIZATION_MEMBERSHIP_MANAGE, Permissions.ORGANIZATION_WRITE,
        Permissions.COURSES_READ, Permissions.COURSES_CREATE, Permissions.COURSES_UPDATE,
        Permissions.COURSES_WRITE, Permissions.COURSES_PUBLISH, Permissions.COURSES_ASSIGN,
        Permissions.COURSE_CATEGORIES_MANAGE, Permissions.DISCUSSIONS_READ, Permissions.DISCUSSIONS_WRITE, Permissions.DISCUSSIONS_MODERATE,
        Permissions.CLASSES_READ, Permissions.CLASSES_WRITE, Permissions.CLASSES_MANAGE,
        Permissions.ENROLLMENTS_WRITE, Permissions.LIVE_SESSIONS_MANAGE,
        Permissions.LEARNING_PATHS_READ, Permissions.LEARNING_PATHS_MANAGE, Permissions.LEARNING_PATHS_ASSIGN,
        Permissions.LEARNING_READ_SCOPE, Permissions.XAPI_WRITE, Permissions.XAPI_READ_SCOPE,
        Permissions.ASSESSMENTS_READ, Permissions.ASSESSMENTS_CREATE,
        Permissions.ASSESSMENTS_UPDATE, Permissions.ASSESSMENTS_GRADE,
        Permissions.ASSESSMENT_MANAGE, Permissions.GRADING_MANAGE,
        Permissions.GRADE_APPEALS_CREATE, Permissions.GRADE_APPEALS_MANAGE,
        Permissions.EXAMS_MANAGE, Permissions.EXAMS_ASSIGN,
        Permissions.COMPETITIONS_MANAGE, Permissions.COMPETITIONS_REWARD,
        Permissions.QUESTIONS_READ, Permissions.QUESTIONS_MANAGE,
        Permissions.QUESTIONS_GENERATE_AI, Permissions.QUESTIONS_APPROVE_AI,
        Permissions.REPORTS_READ, Permissions.REPORTS_READ_SCOPE, Permissions.REPORTS_EXPORT, Permissions.REPORTS_SCHEDULE, Permissions.REPORTS_KPI_READ,
        Permissions.FILES_READ, Permissions.FILES_UPLOAD, Permissions.FILES_DOWNLOAD,
        Permissions.FILES_EDIT, Permissions.FILES_PUBLISH, Permissions.FILES_VERSION_READ,
        Permissions.NEWS_READ, Permissions.NEWS_MANAGE, Permissions.NEWS_PUBLISH,
        Permissions.NOTIFICATION_TEMPLATES_MANAGE, Permissions.NOTIFICATION_REMINDERS_MANAGE,
        Permissions.BRANDING_MANAGE, Permissions.CONFIGURATION_MANAGE,
        Permissions.INTEGRATIONS_MANAGE, Permissions.AI_USE,
        Permissions.CERTIFICATES_MANAGE, Permissions.CERTIFICATE_TEMPLATES_MANAGE,
        Permissions.COMPETENCIES_READ_SELF, Permissions.COMPETENCIES_READ_SCOPE, Permissions.COMPETENCIES_MANAGE, Permissions.COMPETENCIES_ASSESS,
        Permissions.AUDIT_READ, Permissions.AUDIT_EXPORT,
        Permissions.OPERATIONS_MANAGE, Permissions.LICENSE_MANAGE,
    )

    val INSTRUCTOR = setOf(
        Permissions.COURSES_READ, Permissions.COURSES_CREATE, Permissions.COURSES_UPDATE,
        Permissions.COURSES_WRITE, Permissions.COURSES_PUBLISH, Permissions.COURSES_ASSIGN,
        Permissions.COURSE_CATEGORIES_MANAGE, Permissions.DISCUSSIONS_READ, Permissions.DISCUSSIONS_WRITE, Permissions.DISCUSSIONS_MODERATE,
        Permissions.CLASSES_READ, Permissions.CLASSES_WRITE, Permissions.CLASSES_MANAGE,
        Permissions.ENROLLMENTS_WRITE, Permissions.LIVE_SESSIONS_MANAGE, Permissions.LIVE_SESSIONS_JOIN,
        Permissions.LEARNING_PATHS_READ, Permissions.LEARNING_PATHS_MANAGE, Permissions.LEARNING_PATHS_ASSIGN,
        Permissions.LEARNING_READ_SCOPE, Permissions.XAPI_WRITE, Permissions.XAPI_READ_SCOPE,
        Permissions.ASSESSMENTS_READ, Permissions.ASSESSMENTS_CREATE,
        Permissions.ASSESSMENTS_UPDATE, Permissions.ASSESSMENTS_GRADE,
        Permissions.ASSESSMENT_MANAGE, Permissions.GRADING_MANAGE,
        Permissions.GRADE_APPEALS_CREATE, Permissions.GRADE_APPEALS_MANAGE,
        Permissions.EXAMS_MANAGE, Permissions.EXAMS_ASSIGN,
        Permissions.QUESTIONS_READ, Permissions.QUESTIONS_MANAGE,
        Permissions.QUESTIONS_GENERATE_AI, Permissions.QUESTIONS_APPROVE_AI,
        Permissions.REPORTS_READ, Permissions.REPORTS_READ_SCOPE, Permissions.REPORTS_EXPORT, Permissions.REPORTS_KPI_READ,
        Permissions.COMPETENCIES_READ_SELF, Permissions.COMPETENCIES_READ_SCOPE, Permissions.COMPETENCIES_ASSESS,
        Permissions.FILES_READ, Permissions.FILES_UPLOAD, Permissions.FILES_DOWNLOAD,
        Permissions.FILES_EDIT, Permissions.FILES_VERSION_READ,
        Permissions.NEWS_READ, Permissions.AI_USE,
    )

    val STUDENT = setOf(
        Permissions.COURSES_READ, Permissions.COURSES_LEARN, Permissions.DISCUSSIONS_READ, Permissions.DISCUSSIONS_WRITE,
        Permissions.LEARNING_PATHS_READ,
        Permissions.LIVE_SESSIONS_JOIN,
        Permissions.LEARNING_READ_SELF, Permissions.LEARNING_WRITE_SELF, Permissions.XAPI_WRITE,
        Permissions.ASSESSMENT_TAKE, Permissions.ASSESSMENTS_TAKE,
        Permissions.COMPETITIONS_PARTICIPATE,
        Permissions.GRADES_READ_SELF, Permissions.GRADE_APPEALS_CREATE, Permissions.REPORTS_READ_SELF, Permissions.COMPETENCIES_READ_SELF,
        Permissions.FILES_READ, Permissions.FILES_DOWNLOAD,
        Permissions.NEWS_READ, Permissions.CERTIFICATES_READ_SELF,
    )

    val LEARNER = STUDENT
}
