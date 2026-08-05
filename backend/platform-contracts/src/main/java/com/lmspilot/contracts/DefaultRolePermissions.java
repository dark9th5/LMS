package com.lmspilot.contracts;

import java.util.Set;

/** Canonical, mutually exclusive product role permissions. */
public final class DefaultRolePermissions {
    private DefaultRolePermissions() {}
    public static final Set<String> ADMIN = Set.of(
        Permissions.USERS_READ,
        Permissions.USERS_CREATE,
        Permissions.USERS_UPDATE,
        Permissions.USERS_LOCK,
        Permissions.USERS_BULK_MANAGE,
        Permissions.USERS_WRITE,
        Permissions.USERS_SESSIONS_MANAGE,
        Permissions.USERS_PASSWORD_POLICY_MANAGE,
        Permissions.ROLES_READ,
        Permissions.ORGANIZATION_READ,
        Permissions.ORGANIZATION_MANAGE,
        Permissions.ORGANIZATION_MEMBERSHIP_MANAGE,
        Permissions.ORGANIZATION_WRITE,
        Permissions.REPORTS_READ,
        Permissions.REPORTS_EXPORT,
        Permissions.REPORTS_SCHEDULE,
        Permissions.REPORTS_KPI_READ,
        Permissions.BRANDING_MANAGE,
        Permissions.CONFIGURATION_MANAGE,
        Permissions.INTEGRATIONS_MANAGE,
        Permissions.FILES_READ,
        Permissions.FILES_UPLOAD,
        Permissions.FILES_DOWNLOAD,
        Permissions.NEWS_READ,
        Permissions.NEWS_MANAGE,
        Permissions.NEWS_PUBLISH,
        Permissions.NOTIFICATION_TEMPLATES_MANAGE,
        Permissions.NOTIFICATION_REMINDERS_MANAGE,
        Permissions.AUDIT_READ,
        Permissions.AUDIT_EXPORT,
        Permissions.OPERATIONS_MANAGE,
        Permissions.LICENSE_MANAGE
    );

    public static final Set<String> INSTRUCTOR = Set.of(
        Permissions.COURSES_READ,
        Permissions.COURSES_CREATE,
        Permissions.COURSES_UPDATE,
        Permissions.COURSES_WRITE,
        Permissions.COURSES_PUBLISH,
        Permissions.COURSES_ASSIGN,
        Permissions.COURSE_CATEGORIES_MANAGE,
        Permissions.DISCUSSIONS_READ,
        Permissions.DISCUSSIONS_WRITE,
        Permissions.DISCUSSIONS_MODERATE,
        Permissions.ENROLLMENTS_WRITE,
        Permissions.LIVE_SESSIONS_MANAGE,
        Permissions.LEARNING_READ_SCOPE,
        Permissions.XAPI_WRITE,
        Permissions.XAPI_READ_SCOPE,
        Permissions.ASSESSMENTS_READ,
        Permissions.ASSESSMENTS_CREATE,
        Permissions.ASSESSMENTS_UPDATE,
        Permissions.ASSESSMENTS_GRADE,
        Permissions.ASSESSMENT_MANAGE,
        Permissions.GRADING_MANAGE,
        Permissions.GRADE_APPEALS_MANAGE,
        Permissions.EXAMS_MANAGE,
        Permissions.EXAMS_ASSIGN,
        Permissions.QUESTIONS_READ,
        Permissions.QUESTIONS_MANAGE,
        Permissions.QUESTIONS_GENERATE_AI,
        Permissions.QUESTIONS_APPROVE_AI,
        Permissions.REPORTS_READ,
        Permissions.REPORTS_READ_SCOPE,
        Permissions.REPORTS_EXPORT,
        Permissions.REPORTS_KPI_READ,
        Permissions.FILES_READ,
        Permissions.FILES_UPLOAD,
        Permissions.FILES_DOWNLOAD,
        Permissions.FILES_EDIT,
        Permissions.FILES_VERSION_READ,
        Permissions.NEWS_READ,
        Permissions.AI_USE
    );

    public static final Set<String> STUDENT = Set.of(
        Permissions.COURSES_READ,
        Permissions.COURSES_LEARN,
        Permissions.DISCUSSIONS_READ,
        Permissions.DISCUSSIONS_WRITE,
        Permissions.LIVE_SESSIONS_JOIN,
        Permissions.LEARNING_READ_SELF,
        Permissions.LEARNING_WRITE_SELF,
        Permissions.XAPI_WRITE,
        Permissions.ASSESSMENT_TAKE,
        Permissions.ASSESSMENTS_TAKE,
        Permissions.GRADES_READ_SELF,
        Permissions.GRADE_APPEALS_CREATE,
        Permissions.REPORTS_READ_SELF,
        Permissions.COMPETENCIES_READ_SELF,
        Permissions.FILES_READ,
        Permissions.FILES_UPLOAD,
        Permissions.FILES_DOWNLOAD,
        Permissions.NEWS_READ,
        Permissions.CERTIFICATES_READ_SELF
    );

    public static final Set<String> LEARNER = STUDENT;
}
