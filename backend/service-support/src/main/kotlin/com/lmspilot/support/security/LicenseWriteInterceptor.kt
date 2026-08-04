package com.lmspilot.support.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Enforces the offline-license read-only state consistently at the HTTP boundary of
 * every servlet business service. Authentication, renewal and internal service
 * traffic remain available so an expired installation can still be inspected,
 * backed up and reactivated without deleting customer data.
 */
class LicenseWriteInterceptor(private val license: LicenseGuard) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method.uppercase() in MUTATING_METHODS) license.requireWritable()
        return true
    }

    companion object {
        private val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "lmspilot", name = ["license-enforcement-enabled"], havingValue = "true", matchIfMissing = true)
class LicenseWriteInterceptorConfiguration(private val license: LicenseGuard) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(LicenseWriteInterceptor(license))
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout",
                "/api/v1/license/**",
                "/api/v1/operations/**",
                "/api/v1/notifications/*/read",
            )
    }
}
