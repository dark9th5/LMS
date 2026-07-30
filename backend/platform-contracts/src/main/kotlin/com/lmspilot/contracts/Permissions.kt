package com.lmspilot.contracts

object Permissions {
    const val USERS_READ = "users:read"
    const val USERS_WRITE = "users:write"
    const val ROLES_MANAGE = "roles:manage"
    const val ORGANIZATION_READ = "organization:read"
    const val ORGANIZATION_WRITE = "organization:write"
    const val COURSES_READ = "courses:read"
    const val COURSES_WRITE = "courses:write"
    const val COURSES_PUBLISH = "courses:publish"
    const val CLASSES_READ = "classes:read"
    const val CLASSES_WRITE = "classes:write"
    const val ENROLLMENTS_WRITE = "enrollments:write"
    const val LEARNING_READ_SELF = "learning:read:self"
    const val LEARNING_WRITE_SELF = "learning:write:self"
    const val LEARNING_READ_SCOPE = "learning:read:scope"
    const val ASSESSMENT_MANAGE = "assessment:manage"
    const val ASSESSMENT_TAKE = "assessment:take"
    const val GRADING_MANAGE = "grading:manage"
    const val GRADES_READ_SELF = "grades:read:self"
    const val REPORTS_READ = "reports:read"
    const val REPORTS_EXPORT = "reports:export"
    const val FILES_UPLOAD = "files:upload"
    const val FILES_DOWNLOAD = "files:download"
    const val CERTIFICATES_MANAGE = "certificates:manage"
    const val CERTIFICATES_READ_SELF = "certificates:read:self"
    const val AUDIT_READ = "audit:read"
    const val OPERATIONS_MANAGE = "operations:manage"
    const val LICENSE_MANAGE = "license:manage"
    const val CONFIGURATION_MANAGE = "configuration:manage"
    const val AI_USE = "ai:use"
    const val INTEGRATIONS_MANAGE = "integrations:manage"
}

object DefaultRolePermissions {
    val ADMIN = setOf(
        Permissions.USERS_READ, Permissions.USERS_WRITE, Permissions.ROLES_MANAGE,
        Permissions.ORGANIZATION_READ, Permissions.ORGANIZATION_WRITE,
        Permissions.COURSES_READ, Permissions.COURSES_WRITE, Permissions.COURSES_PUBLISH,
        Permissions.CLASSES_READ, Permissions.CLASSES_WRITE, Permissions.ENROLLMENTS_WRITE,
        Permissions.LEARNING_READ_SCOPE, Permissions.ASSESSMENT_MANAGE,
        Permissions.GRADING_MANAGE, Permissions.REPORTS_READ, Permissions.REPORTS_EXPORT,
        Permissions.FILES_UPLOAD, Permissions.FILES_DOWNLOAD, Permissions.CERTIFICATES_MANAGE,
        Permissions.AUDIT_READ, Permissions.OPERATIONS_MANAGE, Permissions.LICENSE_MANAGE,
        Permissions.CONFIGURATION_MANAGE, Permissions.AI_USE, Permissions.INTEGRATIONS_MANAGE,
    )
    val INSTRUCTOR = setOf(
        Permissions.COURSES_READ, Permissions.COURSES_WRITE, Permissions.COURSES_PUBLISH,
        Permissions.CLASSES_READ, Permissions.LEARNING_READ_SCOPE,
        Permissions.ASSESSMENT_MANAGE, Permissions.GRADING_MANAGE,
        Permissions.REPORTS_READ, Permissions.REPORTS_EXPORT,
        Permissions.FILES_UPLOAD, Permissions.FILES_DOWNLOAD, Permissions.AI_USE,
    )
    val STUDENT = setOf(
        Permissions.COURSES_READ, Permissions.LEARNING_READ_SELF, Permissions.LEARNING_WRITE_SELF,
        Permissions.ASSESSMENT_TAKE, Permissions.GRADES_READ_SELF,
        Permissions.FILES_UPLOAD, Permissions.FILES_DOWNLOAD, Permissions.CERTIFICATES_READ_SELF,
    )
}
