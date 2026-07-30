package com.lmspilot.filestorage.config

import com.lmspilot.filestorage.domain.StoredFileEntity
import com.lmspilot.filestorage.domain.StoredFileRepository
import com.lmspilot.filestorage.domain.StoredFileStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

private const val DEMO_SEED_KEY = "lmspilot-demo-files-v2"
private val OWNER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

private data class DemoFile(
    val id: UUID,
    val resource: String,
    val originalName: String,
    val contentType: String,
)

@Component
class DevelopmentSeed(
    private val repository: StoredFileRepository,
    private val jdbc: JdbcTemplate,
    @Value("\${storage.root:./data/files}") storageRoot: String,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    private val root = Paths.get(storageRoot).toAbsolutePath().normalize()

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || alreadyApplied()) return
        Files.createDirectories(root)
        val now = Instant.now()
        listOf(
            DemoFile(
                UUID.fromString("00000000-0000-0000-0000-000000000121"),
                "demo/LMSPilot_Huong_dan_nhanh_hoc_vien.pdf",
                "LMSPilot_Huong_dan_nhanh_hoc_vien.pdf",
                "application/pdf",
            ),
            DemoFile(
                UUID.fromString("00000000-0000-0000-0000-000000000122"),
                "demo/LMSPilot_Checklist_giang_vien.docx",
                "LMSPilot_Checklist_giang_vien.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ),
            DemoFile(
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                "demo/LMSPilot_Gioi_thieu_Bai_0.mp4",
                "LMSPilot_Gioi_thieu_Bai_0.mp4",
                "video/mp4",
            ),
        ).forEach { seedFile(it, now) }
        jdbc.update("INSERT INTO demo_seed_history(seed_key, applied_at) VALUES (?, ?)", DEMO_SEED_KEY, now)
    }

    private fun seedFile(file: DemoFile, now: Instant) {
        val resource = ClassPathResource(file.resource)
        val bytes = resource.inputStream.use { it.readBytes() }
        val storageKey = "00/${file.id}"
        val target = root.resolve(storageKey).normalize()
        require(target.startsWith(root)) { "Invalid demo file path" }
        Files.createDirectories(target.parent)
        if (!Files.isRegularFile(target)) {
            val temporary = Files.createTempFile(target.parent, ".demo-", ".tmp")
            try {
                Files.write(temporary, bytes)
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }

        val entity = repository.findById(file.id).orElseGet { StoredFileEntity(id = file.id) }
        entity.ownerId = OWNER_ID
        entity.originalName = file.originalName
        entity.storageKey = storageKey
        entity.contentType = file.contentType
        entity.sizeBytes = bytes.size.toLong()
        entity.sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        entity.purpose = "COURSE_CONTENT"
        entity.status = StoredFileStatus.AVAILABLE
        entity.createdAt = now
        entity.deletedAt = null
        repository.save(entity)
    }

    private fun alreadyApplied(): Boolean = jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM demo_seed_history WHERE seed_key = ?)",
        Boolean::class.java,
        DEMO_SEED_KEY,
    ) == true
}
