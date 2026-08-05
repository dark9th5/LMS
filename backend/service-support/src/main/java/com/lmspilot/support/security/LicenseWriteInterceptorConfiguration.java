package com.lmspilot.support.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "lmspilot", name = "license-enforcement-enabled", havingValue = "true", matchIfMissing = true)
public class LicenseWriteInterceptorConfiguration implements WebMvcConfigurer {
    private final LicenseGuard license;
    public LicenseWriteInterceptorConfiguration(LicenseGuard license) { this.license = license; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LicenseWriteInterceptor(license)).addPathPatterns("/api/v1/**")
            .excludePathPatterns("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
                "/api/v1/license/**", "/api/v1/operations/**", "/api/v1/notifications/*/read");
    }
}
