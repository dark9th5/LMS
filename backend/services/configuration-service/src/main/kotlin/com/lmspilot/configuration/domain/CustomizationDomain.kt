package com.lmspilot.configuration.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "branding_profiles")
class BrandingProfileEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "profile_key", nullable = false, unique = true, length = 80)
    var profileKey: String = "default",
    @Column(name = "system_name", nullable = false, length = 240)
    var systemName: String = "Học viện Huyền Tri",
    @Column(columnDefinition = "text") var introduction: String? = null,
    @Column(name = "logo_file_id") var logoFileId: UUID? = null,
    @Column(name = "favicon_file_id") var faviconFileId: UUID? = null,
    @Column(name = "background_file_id") var backgroundFileId: UUID? = null,
    @Column(name = "theme_key", nullable = false, length = 64)
    var themeKey: String = "soft-spectrum",
    @Column(name = "primary_color", nullable = false, length = 9) var primaryColor: String = "#B95547",
    @Column(name = "secondary_color", nullable = false, length = 9) var secondaryColor: String = "#5967B8",
    @Column(name = "background_color", nullable = false, length = 9) var backgroundColor: String = "#F6F3EF",
    @Column(name = "text_color", nullable = false, length = 9) var textColor: String = "#20232E",
    @Column(name = "custom_domain", length = 253) var customDomain: String? = null,
    @Column(name = "updated_by", nullable = false) var updatedBy: UUID = UUID(0, 1),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface BrandingProfileRepository : org.springframework.data.jpa.repository.JpaRepository<BrandingProfileEntity, UUID> {
    fun findByProfileKey(profileKey: String): BrandingProfileEntity?
}

enum class ExternalServiceType { REDIS, SMTP, VIDEO_CONFERENCE, AI_PROVIDER, OBJECT_STORAGE, DOCUMENT_EDITOR }
enum class ExternalServiceHealth { UNKNOWN, HEALTHY, DEGRADED, UNREACHABLE, MISCONFIGURED }

@Entity
@Table(name = "external_service_configs", uniqueConstraints = [UniqueConstraint(columnNames = ["service_type", "config_key"])])
class ExternalServiceConfigEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 40)
    var serviceType: ExternalServiceType = ExternalServiceType.REDIS,
    @Column(name = "config_key", nullable = false, length = 80)
    var configKey: String = "default",
    @Column(nullable = false) var enabled: Boolean = false,
    @Column(name = "config_json", nullable = false, columnDefinition = "text") var configJson: String = "{}",
    @Column(name = "encrypted_secret") var encryptedSecret: ByteArray? = null,
    @Column(name = "secret_key_version") var secretKeyVersion: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    var healthStatus: ExternalServiceHealth = ExternalServiceHealth.UNKNOWN,
    @Column(name = "last_checked_at") var lastCheckedAt: Instant? = null,
    @Column(name = "last_error", length = 1000) var lastError: String? = null,
    @Column(name = "updated_by", nullable = false) var updatedBy: UUID = UUID(0, 1),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
    @Version var version: Long = 0,
)

interface ExternalServiceConfigRepository : org.springframework.data.jpa.repository.JpaRepository<ExternalServiceConfigEntity, UUID> {
    fun findByServiceTypeAndConfigKey(serviceType: ExternalServiceType, configKey: String): ExternalServiceConfigEntity?
    fun findAllByOrderByServiceTypeAscConfigKeyAsc(): List<ExternalServiceConfigEntity>
}
