package com.lmspilot.filestorage.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.filestorage.domain.StoredFileEntity
import com.lmspilot.filestorage.domain.StoredFileRepository
import com.lmspilot.filestorage.domain.StoredFileStatus
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import java.util.UUID

private const val MAX_PURPOSE_LENGTH = 50

data class StoredFileResponse(
    val id: UUID,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
    val purpose: String,
    val status: StoredFileStatus,
    val createdAt: Instant,
)

@Service
class FileStorageService(
    private val repository: StoredFileRepository,
    @Value("\${storage.root:./data/files}") root: String,
    @Value("\${storage.max-size-bytes:209715200}") private val maxSizeBytes: Long,
    @Value("\${storage.allowed-content-types:application/pdf,text/plain}") allowedContentTypes: String,
) {
    private val rootPath = Paths.get(root).toAbsolutePath().normalize()
    private val blockedExtensions = setOf("exe", "dll", "bat", "cmd", "ps1", "sh", "jar", "msi", "com", "scr", "js", "html", "htm", "svg")
    private val allowedTypes = allowedContentTypes.split(',').map(String::trim).filter(String::isNotBlank).map(String::lowercase).toSet()
    private val extensionTypes = mapOf(
        "pdf" to "application/pdf",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "mp4" to "video/mp4",
        "mp3" to "audio/mpeg",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "txt" to "text/plain",
        "csv" to "text/csv",
    )

    @PostConstruct
    fun initialize() {
        require(maxSizeBytes > 0) { "FILE_MAX_SIZE_BYTES must be positive" }
        require(allowedTypes.isNotEmpty()) { "FILE_ALLOWED_TYPES must not be empty" }
        Files.createDirectories(rootPath)
    }

    @Transactional
    fun store(file: MultipartFile, purposeInput: String): StoredFileResponse {
        if (file.isEmpty) throw ApiException(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "Tệp tải lên đang trống")
        if (file.size > maxSizeBytes) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp vượt quá dung lượng cho phép")

        val safeName = safeFilename(file.originalFilename)
        val extension = safeName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension.isBlank() || extension in blockedExtensions) {
            throw ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_TYPE_BLOCKED", "Loại tệp không được phép")
        }
        val contentType = normalizedContentType(file.contentType, extension)
        if (contentType !in allowedTypes) {
            throw ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_TYPE_NOT_ALLOWED", "Định dạng tệp không nằm trong danh sách cho phép")
        }
        val purpose = normalizePurpose(purposeInput)

        val id = UUID.randomUUID()
        val storageKey = "${id.toString().substring(0, 2)}/$id"
        val target = checkedPath(storageKey)
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, ".upload-", ".tmp")
        val digest = MessageDigest.getInstance("SHA-256")

        try {
            file.inputStream.use { input ->
                Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        written += read
                        if (written > maxSizeBytes) {
                            throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp vượt quá dung lượng cho phép")
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    if (written != file.size) {
                        throw ApiException(HttpStatus.BAD_REQUEST, "FILE_SIZE_MISMATCH", "Dung lượng tệp tải lên không hợp lệ")
                    }
                }
            }
            moveAtomically(temporary, target)
            return try {
                repository.save(
                    StoredFileEntity(
                        id = id,
                        ownerId = CurrentUser.id(),
                        originalName = safeName,
                        storageKey = storageKey,
                        contentType = contentType,
                        sizeBytes = file.size,
                        sha256 = digest.digest().joinToString("") { "%02x".format(it) },
                        purpose = purpose,
                    )
                ).response()
            } catch (ex: Exception) {
                Files.deleteIfExists(target)
                throw ex
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    @Transactional(readOnly = true)
    fun metadata(id: UUID): StoredFileResponse = available(id).response()

    @Transactional(readOnly = true)
    fun download(id: UUID, inline: Boolean = false): ResponseEntity<InputStreamResource> {
        val entity = available(id)
        val path = checkedPath(entity.storageKey)
        if (!Files.isRegularFile(path)) throw ApiException(HttpStatus.NOT_FOUND, "FILE_BYTES_MISSING", "Dữ liệu tệp không còn tồn tại")
        val resource = InputStreamResource(Files.newInputStream(path, StandardOpenOption.READ))
        val mediaType = runCatching { MediaType.parseMediaType(entity.contentType) }.getOrDefault(MediaType.APPLICATION_OCTET_STREAM)
        val disposition = (if (inline) ContentDisposition.inline() else ContentDisposition.attachment()).filename(entity.originalName, Charsets.UTF_8).build().toString()
        return ResponseEntity.ok()
            .contentType(mediaType)
            .contentLength(entity.sizeBytes)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            .header("X-Content-Type-Options", "nosniff")
            .body(resource)
    }

    @Transactional
    fun delete(id: UUID) {
        val entity = available(id)
        if (entity.ownerId != CurrentUser.id() && Permissions.OPERATIONS_MANAGE !in CurrentUser.authorities()) {
            throw ApiException(HttpStatus.FORBIDDEN, "FILE_OWNER_MISMATCH", "Bạn không có quyền xóa tệp")
        }
        entity.status = StoredFileStatus.DELETED
        entity.deletedAt = Instant.now()
    }

    private fun available(id: UUID): StoredFileEntity {
        val entity = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "Không tìm thấy tệp") }
        if (entity.status != StoredFileStatus.AVAILABLE) throw ApiException(HttpStatus.GONE, "FILE_UNAVAILABLE", "Tệp không còn khả dụng")
        return entity
    }

    private fun checkedPath(storageKey: String): Path {
        val path = rootPath.resolve(storageKey).normalize()
        if (!path.startsWith(rootPath)) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "Đường dẫn tệp không hợp lệ")
        return path
    }

    private fun safeFilename(value: String?): String {
        val normalized = (value ?: "file.bin").replace('\\', '/').substringAfterLast('/').trim()
        if (normalized.isBlank() || normalized.length > 255 || normalized.any { it.code < 32 }) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILENAME", "Tên tệp không hợp lệ")
        }
        return normalized
    }

    private fun normalizedContentType(value: String?, extension: String): String {
        val declared = value?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (declared.isBlank() || declared == MediaType.APPLICATION_OCTET_STREAM_VALUE) {
            return extensionTypes[extension] ?: declared.ifBlank { MediaType.APPLICATION_OCTET_STREAM_VALUE }
        }
        val expected = extensionTypes[extension]
        if (expected != null && declared != expected) {
            throw ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE_TYPE_MISMATCH", "Phần mở rộng và loại nội dung của tệp không khớp")
        }
        return declared
    }

    private fun normalizePurpose(value: String): String {
        val purpose = value.trim().uppercase(Locale.ROOT)
        if (purpose.isBlank() || purpose.length > MAX_PURPOSE_LENGTH || !purpose.matches(Regex("[A-Z0-9_]+"))) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PURPOSE", "Mục đích sử dụng tệp không hợp lệ")
        }
        return purpose
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }
}

private fun StoredFileEntity.response() = StoredFileResponse(id, originalName, contentType, sizeBytes, sha256, purpose, status, createdAt)

@RestController
@RequestMapping("/api/v1/files")
class FileController(private val service: FileStorageService) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('${Permissions.FILES_UPLOAD}')")
    fun upload(@RequestPart("file") file: MultipartFile, @RequestParam(defaultValue = "GENERAL") purpose: String) = service.store(file, purpose)

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('${Permissions.FILES_DOWNLOAD}')")
    fun metadata(@PathVariable id: UUID) = service.metadata(id)

    @GetMapping("/{id}/content")
    @PreAuthorize("hasAuthority('${Permissions.FILES_DOWNLOAD}')")
    fun download(@PathVariable id: UUID, @RequestParam(defaultValue = "false") inline: Boolean) = service.download(id, inline)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    fun delete(@PathVariable id: UUID) = service.delete(id)
}
