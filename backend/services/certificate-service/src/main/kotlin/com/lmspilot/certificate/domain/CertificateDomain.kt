package com.lmspilot.certificate.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class CertificateStatus { ACTIVE, REVOKED, REISSUED }

@Entity
@Table(name = "certificates", uniqueConstraints = [UniqueConstraint(name = "uq_certificate_enrollment_active", columnNames = ["enrollment_id", "generation"])])
class CertificateEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var enrollmentId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var courseId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true, length = 48) var verificationCode: String = "",
    @Column(nullable = false) var generation: Int = 1,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: CertificateStatus = CertificateStatus.ACTIVE,
    @Column(nullable = false) var issuedAt: Instant = Instant.now(),
    var revokedAt: Instant? = null,
    @Column(columnDefinition = "text") var revokeReason: String? = null,
    var replacesCertificateId: UUID? = null,
)
interface CertificateRepository : org.springframework.data.jpa.repository.JpaRepository<CertificateEntity, UUID> {
    fun findByVerificationCode(code: String): CertificateEntity?
    fun findAllByUserIdOrderByIssuedAtDesc(userId: UUID): List<CertificateEntity>
    fun findAllByOrderByIssuedAtDesc(): List<CertificateEntity>
    fun findAllByEnrollmentIdOrderByGenerationDesc(enrollmentId: UUID): List<CertificateEntity>
}
