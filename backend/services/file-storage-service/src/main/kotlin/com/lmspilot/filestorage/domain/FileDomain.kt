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
}
