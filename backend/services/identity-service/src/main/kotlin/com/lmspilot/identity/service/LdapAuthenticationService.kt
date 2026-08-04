package com.lmspilot.identity.service

import com.lmspilot.support.api.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.Hashtable
import javax.naming.AuthenticationException
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls

@Service
class LdapAuthenticationService(
    private val license: LicenseEntitlementClient,
    @Value("\${identity.ldap.enabled:false}") private val enabled: Boolean,
    @Value("\${identity.ldap.url:}") private val url: String,
    @Value("\${identity.ldap.user-dn-pattern:}") private val userDnPattern: String,
    @Value("\${identity.ldap.base-dn:}") private val baseDn: String,
    @Value("\${identity.ldap.user-search-filter:(uid={0})}") private val userSearchFilter: String,
    @Value("\${identity.ldap.manager-dn:}") private val managerDn: String,
    @Value("\${identity.ldap.manager-password:}") private val managerPassword: String,
    @Value("\${identity.ldap.connect-timeout-ms:3000}") private val connectTimeoutMs: Int,
    @Value("\${identity.ldap.read-timeout-ms:5000}") private val readTimeoutMs: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isEnabled(): Boolean = enabled

    /**
     * Authenticate an account that already exists in LMSPilot. Local authentication is
     * always attempted first by AuthService, so the protected bootstrap administrator
     * remains available even when LDAP/AD is down.
     */
    fun authenticate(username: String, password: String): Boolean {
        if (!enabled) return false
        if (password.isBlank()) return false
        if (!USERNAME_PATTERN.matches(username)) return false
        license.requireFeature("LDAP", write = false)
        if (url.isBlank()) unavailable("LDAP_URL chưa được cấu hình")

        return try {
            val principal = if (userDnPattern.isNotBlank()) {
                userDnPattern.replace("{0}", username)
            } else {
                resolveUserDn(username)
            }
            bind(principal, password).close()
            true
        } catch (_: AuthenticationException) {
            false
        } catch (cause: NamingException) {
            log.warn("LDAP authentication service unavailable for user={}", username, cause)
            unavailable(cause.message ?: "Không thể kết nối LDAP/Active Directory")
        } catch (cause: ApiException) {
            throw cause
        } catch (cause: Exception) {
            log.warn("Unexpected LDAP authentication failure for user={}", username, cause)
            unavailable(cause.message ?: "Không thể xác thực LDAP/Active Directory")
        }
    }

    private fun resolveUserDn(username: String): String {
        if (baseDn.isBlank() || managerDn.isBlank() || managerPassword.isBlank()) {
            unavailable("Cần user-dn-pattern hoặc đầy đủ base-dn/manager-dn/manager-password")
        }
        val manager = bind(managerDn, managerPassword)
        try {
            val controls = SearchControls().apply {
                searchScope = SearchControls.SUBTREE_SCOPE
                countLimit = 2
                returningAttributes = arrayOf("distinguishedName")
            }
            val filter = userSearchFilter.replace("{0}", escapeFilter(username))
            val results = manager.search(baseDn, filter, controls)
            val matches = mutableListOf<String>()
            try {
                while (results.hasMore()) {
                    val result = results.next()
                    val distinguishedName = result.attributes?.get("distinguishedName")?.get()?.toString()
                    matches += distinguishedName ?: runCatching { result.nameInNamespace }.getOrElse {
                        if (result.name.isBlank()) baseDn else "${result.name},$baseDn"
                    }
                }
            } finally {
                runCatching { results.close() }
            }
            return when (matches.size) {
                1 -> matches.first()
                0 -> throw AuthenticationException("LDAP user not found")
                else -> throw AuthenticationException("LDAP user is ambiguous")
            }
        } finally {
            runCatching { manager.close() }
        }
    }

    private fun bind(principal: String, credentials: String): InitialDirContext {
        val environment = Hashtable<String, String>().apply {
            put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
            put(Context.PROVIDER_URL, url.trim())
            put(Context.SECURITY_AUTHENTICATION, "simple")
            put(Context.SECURITY_PRINCIPAL, principal)
            put(Context.SECURITY_CREDENTIALS, credentials)
            put("com.sun.jndi.ldap.connect.timeout", connectTimeoutMs.coerceIn(500, 60_000).toString())
            put("com.sun.jndi.ldap.read.timeout", readTimeoutMs.coerceIn(500, 120_000).toString())
            put("com.sun.jndi.ldap.connect.pool", "false")
        }
        return InitialDirContext(environment)
    }

    private fun escapeFilter(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\5c")
                '*' -> append("\\2a")
                '(' -> append("\\28")
                ')' -> append("\\29")
                '\u0000' -> append("\\00")
                else -> append(ch)
            }
        }
    }

    private fun unavailable(message: String): Nothing = throw ApiException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "LDAP_UNAVAILABLE",
        message,
    )

    companion object {
        private val USERNAME_PATTERN = Regex("^[A-Za-z0-9._@\\-]{1,180}$")
    }
}
