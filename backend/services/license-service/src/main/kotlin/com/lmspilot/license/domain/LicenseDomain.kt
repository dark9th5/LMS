package com.lmspilot.license.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class LicenseStatus { ACTIVE, GRACE_PERIOD, EXPIRED, INVALID, DEVELOPMENT }

@Entity
@Table(name = "licenses")
class LicenseEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 100) var licenseId: String = "",
    @Column(nullable = false, length = 220) var organization: String = "",
    @Column(nullable = false, length = 80) var edition: String = "STANDARD",
    @Column(nullable = false) var maxUsers: Int = 100,
    @Column(nullable = false, columnDefinition = "text") var featuresJson: String = "[]",
    @Column(nullable = false) var issuedAt: Instant = Instant.now(),
    var expiresAt: Instant? = null,
    @Column(nullable = false) var gracePeriodDays: Int = 0,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: LicenseStatus = LicenseStatus.ACTIVE,
    @Column(nullable = false, columnDefinition = "text") var sourcePayload: String = "",
    @Column(nullable = false) var activatedAt: Instant = Instant.now(),
)
interface LicenseRepository : org.springframework.data.jpa.repository.JpaRepository<LicenseEntity, UUID> {
    fun findTopByOrderByActivatedAtDesc(): LicenseEntity?
    fun findByLicenseId(licenseId: String): LicenseEntity?
}
