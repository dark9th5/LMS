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
    var templateId: UUID? = null,
    @Column(nullable = false, columnDefinition = "text") var templateSnapshotJson: String = "{}",
)
interface CertificateRepository : org.springframework.data.jpa.repository.JpaRepository<CertificateEntity, UUID> {
    fun findByVerificationCode(code: String): CertificateEntity?
    fun findAllByUserIdOrderByIssuedAtDesc(userId: UUID): List<CertificateEntity>
    fun findAllByOrderByIssuedAtDesc(): List<CertificateEntity>
    fun findAllByEnrollmentIdOrderByGenerationDesc(enrollmentId: UUID): List<CertificateEntity>
}


@Entity
@Table(name = "certificate_templates", indexes = [Index(name = "idx_certificate_template_course", columnList = "course_id,active,updated_at")])
class CertificateTemplateEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 180) var name: String = "Mẫu mặc định",
    @Column(name = "course_id") var courseId: UUID? = null,
    @Column(nullable = false, length = 240) var title: String = "CHỨNG CHỈ HOÀN THÀNH",
    @Column(name = "issuer_name", nullable = false, length = 240) var issuerName: String = "LMSPilot",
    @Column(name = "body_text", nullable = false, length = 1000) var bodyText: String = "Xác nhận người học đã hoàn thành chương trình đào tạo.",
    @Column(name = "primary_color", nullable = false, length = 20) var primaryColor: String = "#173b65",
    @Column(name = "secondary_color", nullable = false, length = 20) var secondaryColor: String = "#b99044",
    @Column(name = "logo_url", length = 500) var logoUrl: String? = null,
    @Column(name = "signature_name", length = 240) var signatureName: String? = null,
    @Column(nullable = false) var active: Boolean = true,
    @Column(name = "created_by", nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface CertificateTemplateRepository : org.springframework.data.jpa.repository.JpaRepository<CertificateTemplateEntity, UUID> {
    fun findFirstByCourseIdAndActiveTrueOrderByUpdatedAtDesc(courseId: UUID): CertificateTemplateEntity?
    fun findFirstByCourseIdIsNullAndActiveTrueOrderByUpdatedAtDesc(): CertificateTemplateEntity?
    fun findAllByOrderByUpdatedAtDesc(): List<CertificateTemplateEntity>
}
