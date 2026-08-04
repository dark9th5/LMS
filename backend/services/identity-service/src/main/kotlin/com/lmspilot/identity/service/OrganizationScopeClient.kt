package com.lmspilot.identity.service

import com.lmspilot.identity.domain.ScopeType
import com.lmspilot.support.api.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class OrganizationScopeClient(
    builder: RestClient.Builder,
    @Value("\${organization-service.url:http://localhost:8082}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = builder.baseUrl(baseUrl).build()

    fun existingActiveUnitIds(ids: Set<UUID>): Set<UUID> {
        if (ids.isEmpty()) return emptySet()
        return runCatching {
            val values = client.post()
                .uri("/internal/v1/organization/units/validate-active")
                .header("X-Service-Token", serviceToken)
                .body(mapOf("ids" to ids))
                .retrieve()
                .body(Array<String>::class.java)
                ?: emptyArray()
            values.map(UUID::fromString).toSet()
        }.getOrElse { cause ->
            log.error("Could not validate organization units", cause)
            throw ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ORGANIZATION_SERVICE_UNAVAILABLE",
                "Không thể xác nhận đơn vị tổ chức",
            )
        }
    }

    fun requireActiveUnit(unitId: UUID?) {
        if (unitId == null) return
        if (unitId !in existingActiveUnitIds(setOf(unitId))) {
            throw ApiException(HttpStatus.BAD_REQUEST, "ORGANIZATION_UNIT_INVALID", "Đơn vị không tồn tại hoặc đã ngừng hoạt động")
        }
    }

    fun applicableUnitIds(scopeType: ScopeType, scopeId: UUID?): Set<UUID> {
        if (scopeId == null || scopeType !in ORGANIZATION_SCOPES) return scopeId?.let(::setOf) ?: emptySet()
        return runCatching {
            val values = client.get()
                .uri("/internal/v1/organization/units/{id}/ancestors", scopeId)
                .header("X-Service-Token", serviceToken)
                .retrieve()
                .body(Array<String>::class.java)
                ?: emptyArray()
            values.map(UUID::fromString).toSet() + scopeId
        }.getOrElse {
            log.warn("Could not resolve organization ancestors for scopeId={}; falling back to exact scope", scopeId)
            setOf(scopeId)
        }
    }

    companion object {
        val ORGANIZATION_SCOPES = setOf(ScopeType.BRANCH, ScopeType.DEPARTMENT, ScopeType.GROUP)
    }
}
