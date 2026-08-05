package com.lmspilot.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PermissionsTest {
    private Set<String> allPermissions() {
        return Arrays.stream(Permissions.class.getDeclaredFields())
            .filter(field -> field.getType() == String.class)
            .map(this::read)
            .collect(Collectors.toSet());
    }

    private String read(Field field) {
        try { return (String) field.get(null); }
        catch (IllegalAccessException exception) { throw new AssertionError(exception); }
    }

    @Test
    void permissionCodesAreUnique() {
        var values = Arrays.stream(Permissions.class.getDeclaredFields())
            .filter(field -> field.getType() == String.class)
            .map(this::read)
            .toList();
        assertEquals(values.size(), values.stream().distinct().count(), "Mã quyền không được trùng nhau");
    }

    @Test
    void defaultRolesOnlyContainDeclaredPermissions() {
        var rolePermissions = new java.util.HashSet<String>();
        rolePermissions.addAll(DefaultRolePermissions.ADMIN);
        rolePermissions.addAll(DefaultRolePermissions.INSTRUCTOR);
        rolePermissions.addAll(DefaultRolePermissions.STUDENT);
        assertTrue(allPermissions().containsAll(rolePermissions));
    }

    @Test
    void studentRoleDoesNotContainAdministrativePermissions() {
        Set<String> forbidden = Set.of(
            Permissions.USERS_WRITE,
            Permissions.ROLES_MANAGE,
            Permissions.ORGANIZATION_WRITE,
            Permissions.OPERATIONS_MANAGE,
            Permissions.LICENSE_MANAGE
        );
        assertTrue(DefaultRolePermissions.STUDENT.stream().noneMatch(forbidden::contains));
    }
}
