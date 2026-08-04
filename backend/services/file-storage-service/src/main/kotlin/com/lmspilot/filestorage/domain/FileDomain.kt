package com.lmspilot.filestorage.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class StoredFileStatus { AVAILABLE, QUARANTINED, DELETED }

@Entity
@Table(name = "stored_files")
class StoredFileEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var ownerId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 260) var originalName: String = "",
    @Column(nullable = false, length = 260, unique = true) var storageKey: String = "",
    @Column(nullable = false, length = 160) var contentType: String = "application/octet-stream",
    @Column(nullable = false) var sizeBytes: Long = 0,
    @Column(nullable = false, length = 64) var sha256: String = "",
    @Column(nullable = false, length = 80) var purpose: String = "GENERAL",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: StoredFileStatus = StoredFileStatus.AVAILABLE,
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    var deletedAt: Instant? = null,
)
interface StoredFileRepository : org.springframework.data.jpa.repository.JpaRepository<StoredFileEntity, UUID> {
    fun findByStorageKey(storageKey: String): StoredFileEntity?
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<StoredFileEntity>
}

@Entity
@Table(
    name = "file_access_grants",
    uniqueConstraints = [UniqueConstraint(name = "uq_file_access_grant", columnNames = ["file_id", "user_id"])],
    indexes = [Index(name = "idx_file_access_grant_user", columnList = "user_id,expires_at")],
)
class FileAccessGrantEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "file_id", nullable = false) var fileId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 80) var source: String = "INTERNAL",
    @Column(name = "expires_at", nullable = false) var expiresAt: Instant = Instant.now(),
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
)

interface FileAccessGrantRepository : org.springframework.data.jpa.repository.JpaRepository<FileAccessGrantEntity, UUID> {
    fun findByFileIdAndUserId(fileId: UUID, userId: UUID): FileAccessGrantEntity?
}

enum class FileVersionSource { UPLOAD, DOCX_EDIT, PDF_ANNOTATION, CONVERSION }
enum class FileEditorType { ONLYOFFICE, COLLABORA, PDF_ANNOTATOR }
enum class FileEditSessionStatus { OPEN, SAVED, CANCELLED, EXPIRED }

@Entity
@Table(name = "file_versions_v2", uniqueConstraints = [UniqueConstraint(name = "uq_file_version_number", columnNames = ["file_id", "version_number"])])
class FileVersionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "file_id", nullable = false) var fileId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var versionNumber: Int = 1,
    @Column(nullable = false, length = 1000) var storageKey: String = "",
    @Column(nullable = false, length = 255) var mediaType: String = "application/octet-stream",
    @Column(nullable = false) var sizeBytes: Long = 0,
    @Column(nullable = false, length = 64) var sha256: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var sourceType: FileVersionSource = FileVersionSource.UPLOAD,
    var parentVersionId: UUID? = null,
    @Column(length = 1000) var changeSummary: String? = null,
    @Column(nullable = false) var createdBy: UUID = UUID.randomUUID(),
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "file_edit_sessions")
class FileEditSessionEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(nullable = false) var fileId: UUID = UUID.randomUUID(),
    @Column(nullable = false) var baseVersionId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var editorType: FileEditorType = FileEditorType.ONLYOFFICE,
    @Column(nullable = false) var userId: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 64) var lockTokenHash: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: FileEditSessionStatus = FileEditSessionStatus.OPEN,
    @Column(nullable = false) var expiresAt: Instant = Instant.now(),
    @Column(nullable = false) var createdAt: Instant = Instant.now(),
    var closedAt: Instant? = null,
)

interface FileVersionRepository : org.springframework.data.jpa.repository.JpaRepository<FileVersionEntity, UUID> {
    fun findAllByFileIdOrderByVersionNumberDesc(fileId: UUID): List<FileVersionEntity>
    fun findFirstByFileIdOrderByVersionNumberDesc(fileId: UUID): FileVersionEntity?
}

interface FileEditSessionRepository : org.springframework.data.jpa.repository.JpaRepository<FileEditSessionEntity, UUID> {
    fun findAllByFileIdAndStatusOrderByCreatedAtDesc(fileId: UUID, status: FileEditSessionStatus): List<FileEditSessionEntity>
}
