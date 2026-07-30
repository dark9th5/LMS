package com.lmspilot.integration.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class AdapterType { LDAP, ACTIVE_DIRECTORY, HRM, ERP, NAS, SMTP, GENERIC_REST }
enum class AdapterStatus { DRAFT, ACTIVE, DISABLED, ERROR }

@Entity
@Table(name = "integration_adapters")
class IntegrationAdapterEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 100) var code: String = "",
    @Column(nullable = false, length = 180) var name: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) var type: AdapterType = AdapterType.GENERIC_REST,
    @Column(nullable = false, length = 1000) var endpoint: String = "",
    @Column(nullable = false, columnDefinition = "text") var mappingJson: String = "{}",
    @Column(nullable = false, columnDefinition = "text") var secretReference: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: AdapterStatus = AdapterStatus.DRAFT,
    var lastTestedAt: Instant? = null,
    @Column(columnDefinition = "text") var lastTestResult: String? = null,
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var rowVersion: Long = 0,
)
interface IntegrationAdapterRepository : org.springframework.data.jpa.repository.JpaRepository<IntegrationAdapterEntity, UUID> { fun findByCode(code: String): IntegrationAdapterEntity? }
