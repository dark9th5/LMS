package com.lmspilot.identity.config;

import com.lmspilot.identity.domain.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DevelopmentSeed implements ApplicationRunner {
    private final RoleRepository roles;
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DevelopmentSeed(RoleRepository r, UserAccountRepository u, PasswordEncoder e,
                           @Value("${lmspilot.seed-demo:true}") boolean enabled) {
        roles = r;
        users = u;
        encoder = e;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        ensure("00000000-0000-0000-0000-000000000001", "ADM001", "admin", "Quản trị hệ thống", "ADMIN", true, "admin123");
        ensure("00000000-0000-0000-0000-000000000002", "INS001", "instructor", "Giảng viên mẫu", "INSTRUCTOR", false, "instructor123");
        ensure("00000000-0000-0000-0000-000000000003", "STU001", "student", "Học viên mẫu", "STUDENT", false, "student123");
    }

    private void ensure(String id, String code, String username, String name, String role, boolean protect, String pass) {
        UserAccountEntity u = users.findByUsernameIgnoreCase(username).orElseGet(UserAccountEntity::new);
        RoleEntity r = roles.findByCodeIgnoreCase(role).orElseThrow();
        u.id = UUID.fromString(id);
        u.code = code;
        u.username = username;
        u.fullName = name;
        u.passwordHash = encoder.encode(pass);
        u.roles = new LinkedHashSet<>(Set.of(r));
        u.accountType = protect ? AccountType.SYSTEM_ADMIN : AccountType.USER;
        u.protectedAccount = protect;
        u.mustChangePassword = false;
        u.passwordChangedAt = Instant.now();
        u.status = AccountStatus.ACTIVE;
        u.failedLoginCount = 0;
        u.lockedUntil = null;
        users.save(u);
    }
}
