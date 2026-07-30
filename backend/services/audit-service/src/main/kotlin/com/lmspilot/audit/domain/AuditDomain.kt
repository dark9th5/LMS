package com.lmspilot.audit.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_entries", uniqueConstraints = [UniqueConstraint(name = "uq_audit_event", columnNames = ["event_id"])])
class AuditEntryEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "event_id", nullable = false) var eventId: UUID = UUID.randomUUID(),
    var actorId: String? = null,
    var actorUsername: String? = null,
    @Column(nullable = false, length = 120) var action: String = "",
    @Column(nullable = false, length = 120) var resourceType: String = "",
    var resourceId: String? = null,
    @Column(nullable = false, length = 40) var outcome: String = "SUCCESS",
    @Column(columnDefinition = "text") var beforeJson: String? = null,
    @Column(columnDefinition = "text") var afterJson: String? = null,
    var ipAddress: String? = null,
    var correlationId: String? = null,
    @Column(nullable = false) var occurredAt: Instant = Instant.now(),
)
interface AuditEntryRepository : org.springframework.data.jpa.repository.JpaRepository<AuditEntryEntity, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<AuditEntryEntity> {
    fun existsByEventId(eventId: UUID): Boolean
}
