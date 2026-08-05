package com.lmspilot.support.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CurrentUser {
    private CurrentUser() {}

    public static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) return token.getToken();
        throw new IllegalStateException("Authenticated JWT is required");
    }
    public static UUID id() { return UUID.fromString(jwt().getSubject()); }
    public static String username() {
        String value = jwt().getClaimAsString("username");
        return value == null ? jwt().getSubject() : value;
    }
    public static Set<String> roles() { return copyClaim("roles"); }
    public static String accountType() {
        String value = jwt().getClaimAsString("accountType");
        return value == null ? "USER" : value;
    }
    public static boolean hasRole(String role) {
        Set<String> roles = roles();
        return roles.size() == 1 && roles.iterator().next().equalsIgnoreCase(role);
    }
    @Deprecated
    public static boolean isSystemAdmin() { return hasRole("ADMIN") && accountType().equals("SYSTEM_ADMIN"); }
    public static Set<String> authorities() { return copyClaim("permissions"); }
    public static Set<String> globalAuthorities() { return copyClaim("globalPermissions"); }
    private static Set<String> copyClaim(String name) {
        List<String> values = jwt().getClaimAsStringList(name);
        return values == null ? Set.of() : Set.copyOf(new HashSet<>(values));
    }
}
