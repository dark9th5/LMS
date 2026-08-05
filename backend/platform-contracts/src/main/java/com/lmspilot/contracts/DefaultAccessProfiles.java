package com.lmspilot.contracts;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DefaultAccessProfiles {
    private DefaultAccessProfiles() {}

    public static final List<AccessProfileDefinition> PROFILES = List.of(
        new AccessProfileDefinition("ADMIN", "Quản trị viên",
            "Quản trị tài khoản, tổ chức, thương hiệu, tích hợp, vận hành và báo cáo hệ thống.",
            DefaultRolePermissions.ADMIN, Set.of("SYSTEM"), PermissionRisk.CRITICAL),
        new AccessProfileDefinition("INSTRUCTOR", "Giảng viên",
            "Biên soạn khóa học, tạo bài kiểm tra từ tài liệu, vận hành bài thi và chấm điểm.",
            DefaultRolePermissions.INSTRUCTOR, Set.of("SYSTEM", "COURSE", "EXAM"), PermissionRisk.HIGH),
        new AccessProfileDefinition("STUDENT", "Học viên",
            "Học khóa được giao, làm bài kiểm tra trong khóa học, thi độc lập và xem kết quả cá nhân.",
            DefaultRolePermissions.STUDENT, Set.of("SYSTEM"), PermissionRisk.LOW)
    );

    private static final Map<String, AccessProfileDefinition> BY_CODE = PROFILES.stream()
        .collect(Collectors.toUnmodifiableMap(AccessProfileDefinition::code, Function.identity()));

    public static AccessProfileDefinition find(String code) {
        return code == null ? null : BY_CODE.get(code.toUpperCase(Locale.ROOT));
    }
}
