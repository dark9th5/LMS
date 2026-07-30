package com.lmspilot.contracts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PermissionsTest {
    private val allPermissions = Permissions::class.java.declaredFields
        .filter { it.type == String::class.java }
        .map { it.get(null) as String }
        .toSet()

    @Test
    fun `permission codes are unique`() {
        val fields = Permissions::class.java.declaredFields.filter { it.type == String::class.java }
        val values = fields.map { it.get(null) as String }
        assertEquals(values.size, values.toSet().size, "Mã quyền không được trùng nhau")
    }

    @Test
    fun `default roles only contain declared permissions`() {
        val rolePermissions = DefaultRolePermissions.ADMIN +
            DefaultRolePermissions.INSTRUCTOR +
            DefaultRolePermissions.STUDENT
        assertTrue(allPermissions.containsAll(rolePermissions))
    }

    @Test
    fun `student role does not contain administrative permissions`() {
        val forbidden = setOf(
            Permissions.USERS_WRITE,
            Permissions.ROLES_MANAGE,
            Permissions.ORGANIZATION_WRITE,
            Permissions.OPERATIONS_MANAGE,
            Permissions.LICENSE_MANAGE,
        )
        assertTrue(DefaultRolePermissions.STUDENT.intersect(forbidden).isEmpty())
    }
}
