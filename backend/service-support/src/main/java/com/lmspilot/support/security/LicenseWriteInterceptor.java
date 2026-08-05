package com.lmspilot.support.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;

public class LicenseWriteInterceptor implements HandlerInterceptor {
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final LicenseGuard license;
    public LicenseWriteInterceptor(LicenseGuard license) { this.license = license; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (MUTATING_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))) license.requireWritable();
        return true;
    }
}
