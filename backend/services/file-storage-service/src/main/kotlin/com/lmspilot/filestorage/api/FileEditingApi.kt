package com.lmspilot.filestorage.api

import com.lmspilot.contracts.Permissions
import com.lmspilot.filestorage.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

private const val DOCX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

data class FileVersionResponse(
    val id: UUID,
    val fileId: UUID,
    val versionNumber: Int,
    val mediaType: String,
    val sizeBytes: Long,
    val sha256: String,
    val sourceType: FileVersionSource,
    val parentVersionId: UUID?,
    val changeSummary: String?,
    val createdBy: UUID,
    val createdAt: Instant,
)

data class CreateEditSessionRequest(
    val editorType: FileEditorType,
    @field:Size(max = 1000) val changeSummary: String? = null,
)

data class FileEditSessionResponse(
    val id: UUID,
    val fileId: UUID,
    val baseVersionId: UUID,
    val editorType: FileEditorType,
    val status: FileEditSessionStatus,
    val expiresAt: Instant,
    val documentUrl: String,
    val callbackUrl: String?,
    val editorServerUrl: String?,
    val editorConfig: Map<String, Any?>,
)

data class OnlyOfficeCallback(
    val status: Int = 0,
    val url: String? = null,
    val key: String? = null,
    val users: List<String> = emptyList(),
)

@Service
class FileEditingService(
    private val storedFiles: StoredFileRepository,
    private val versions: FileVersionRepository,
    private val sessions: FileEditSessionRepository,
    @Value("\${storage.root:./data/files}") root: String,
    @Value("\${file-editor.public-base-url:http://localhost:8080}") private val publicBaseUrl: String,
    @Value("\${file-editor.onlyoffice-url:http://localhost:8088}") private val onlyOfficeUrl: String,
    @Value("\${file-editor.session-minutes:120}") private val sessionMinutes: Long,
    @Value("\${storage.max-size-bytes:209715200}") private val maxSizeBytes: Long,
) {
    private val rootPath = Paths.get(root).toAbsolutePath().normalize()
    private val random = SecureRandom()
    private val editorHttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    @Transactional(readOnly = true)
    fun listVersions(fileId: UUID): List<FileVersionResponse> {
        requireReadable(fileId)
        return versions.findAllByFileIdOrderByVersionNumberDesc(fileId).map(FileVersionEntity::response)
    }

    @Transactional
    fun createSession(fileId: UUID, request: CreateEditSessionRequest): FileEditSessionResponse {
        val file = requireEditable(fileId)
        validateEditor(file.contentType, request.editorType)
        sessions.findAllByFileIdAndStatusOrderByCreatedAtDesc(fileId, FileEditSessionStatus.OPEN)
            .filter { it.expiresAt.isBefore(Instant.now()) }
            .forEach { it.status = FileEditSessionStatus.EXPIRED; it.closedAt = Instant.now() }
        val baseVersion = latestOrCreateInitial(file)
        val token = randomToken()
        val entity = sessions.save(
            FileEditSessionEntity(
                fileId = file.id,
                baseVersionId = baseVersion.id,
                editorType = request.editorType,
                userId = CurrentUser.id(),
                lockTokenHash = sha256(token.toByteArray()),
                expiresAt = Instant.now().plus(Duration.ofMinutes(sessionMinutes.coerceIn(15, 1440))),
            )
        )
        return sessionResponse(entity, file, token, request.changeSummary)
    }

    @Transactional
    fun cancel(sessionId: UUID) {
        val session = requireOwnedSession(sessionId)
        if (session.status != FileEditSessionStatus.OPEN) return
        session.status = FileEditSessionStatus.CANCELLED
        session.closedAt = Instant.now()
    }

    @Transactional
    fun savePdf(sessionId: UUID, file: MultipartFile, changeSummary: String?): FileVersionResponse {
        val session = requireOwnedSession(sessionId)
        if (session.editorType != FileEditorType.PDF_ANNOTATOR) throw ApiException(HttpStatus.CONFLICT, "EDITOR_TYPE_MISMATCH", "Phiên này không phải trình chỉnh sửa PDF")
        if (file.contentType?.substringBefore(';') != MediaType.APPLICATION_PDF_VALUE) throw ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF_REQUIRED", "Chỉ chấp nhận tệp PDF")
        val version = saveVersion(session, file.bytes, MediaType.APPLICATION_PDF_VALUE, FileVersionSource.PDF_ANNOTATION, changeSummary)
        closeSaved(session)
        return version.response()
    }

    @Transactional(readOnly = true)
    fun publicContent(sessionId: UUID, token: String): ResponseEntity<InputStreamResource> {
        val session = requireTokenSession(sessionId, token)
        val version = versions.findById(session.baseVersionId).orElseThrow { versionNotFound() }
        val file = storedFiles.findById(session.fileId).orElseThrow { fileNotFound() }
        val path = checkedPath(version.storageKey)
        if (!Files.isRegularFile(path)) throw ApiException(HttpStatus.NOT_FOUND, "FILE_BYTES_MISSING", "Dữ liệu phiên bản không tồn tại")
        val media = runCatching { MediaType.parseMediaType(version.mediaType) }.getOrDefault(MediaType.APPLICATION_OCTET_STREAM)
        return ResponseEntity.ok()
            .contentType(media)
            .contentLength(version.sizeBytes)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.originalName, Charsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(InputStreamResource(Files.newInputStream(path, StandardOpenOption.READ)))
    }

    @Transactional
    fun onlyOfficeCallback(sessionId: UUID, token: String, payload: OnlyOfficeCallback): Map<String, Int> {
        val session = requireTokenSession(sessionId, token)
        if (session.editorType !in setOf(FileEditorType.ONLYOFFICE, FileEditorType.COLLABORA)) return mapOf("error" to 1)
        if (payload.status in setOf(2, 6)) {
            val downloadUrl = payload.url ?: return mapOf("error" to 1)
            val bytes = fetchOnlyOfficeResult(downloadUrl)
            val stored = storedFiles.findById(session.fileId).orElseThrow { fileNotFound() }
            val mediaType = stored.contentType
            val source = if (mediaType == MediaType.APPLICATION_PDF_VALUE) FileVersionSource.PDF_ANNOTATION else FileVersionSource.DOCX_EDIT
            saveVersion(session, bytes, mediaType, source, "Lưu từ trình chỉnh sửa trực tuyến")
            closeSaved(session)
        }
        if (payload.status in setOf(3, 7)) {
            session.status = FileEditSessionStatus.CANCELLED
            session.closedAt = Instant.now()
        }
        return mapOf("error" to 0)
    }

    private fun sessionResponse(session: FileEditSessionEntity, file: StoredFileEntity, token: String, changeSummary: String?): FileEditSessionResponse {
        val base = publicBaseUrl.trimEnd('/')
        val documentUrl = "$base/public/v1/file-edit/${session.id}/content?token=$token"
        val callbackUrl = if (session.editorType in setOf(FileEditorType.ONLYOFFICE, FileEditorType.COLLABORA)) "$base/public/v1/file-edit/${session.id}/callback?token=$token" else null
        val config = if (session.editorType in setOf(FileEditorType.ONLYOFFICE, FileEditorType.COLLABORA)) {
            val isPdf = file.contentType == MediaType.APPLICATION_PDF_VALUE
            mapOf(
                "document" to mapOf(
                    "fileType" to if (isPdf) "pdf" else "docx",
                    "key" to "${session.fileId}-${session.baseVersionId}",
                    "title" to file.originalName,
                    "url" to documentUrl,
                    "permissions" to mapOf("edit" to true, "download" to true, "print" to true),
                ),
                "documentType" to if (isPdf) "pdf" else "word",
                "editorConfig" to mapOf(
                    "callbackUrl" to callbackUrl,
                    "lang" to "vi",
                    "mode" to "edit",
                    "user" to mapOf("id" to session.userId.toString(), "name" to CurrentUser.username()),
                    "customization" to mapOf("autosave" to true, "forcesave" to true, "compactHeader" to false),
                ),
                "changeSummary" to changeSummary,
            )
        } else mapOf("documentUrl" to documentUrl, "changeSummary" to changeSummary)
        return FileEditSessionResponse(
            session.id, session.fileId, session.baseVersionId, session.editorType, session.status, session.expiresAt,
            documentUrl, callbackUrl, onlyOfficeUrl.takeIf { session.editorType in setOf(FileEditorType.ONLYOFFICE, FileEditorType.COLLABORA) }, config,
        )
    }

    private fun latestOrCreateInitial(file: StoredFileEntity): FileVersionEntity = versions.findFirstByFileIdOrderByVersionNumberDesc(file.id)
        ?: versions.save(
            FileVersionEntity(
                fileId = file.id,
                versionNumber = 1,
                storageKey = file.storageKey,
                mediaType = file.contentType,
                sizeBytes = file.sizeBytes,
                sha256 = file.sha256,
                sourceType = FileVersionSource.UPLOAD,
                createdBy = file.ownerId,
                createdAt = file.createdAt,
            )
        )

    private fun saveVersion(
        session: FileEditSessionEntity,
        bytes: ByteArray,
        mediaType: String,
        source: FileVersionSource,
        summary: String?,
    ): FileVersionEntity {
        if (session.status != FileEditSessionStatus.OPEN || !session.expiresAt.isAfter(Instant.now())) throw ApiException(HttpStatus.CONFLICT, "EDIT_SESSION_CLOSED", "Phiên chỉnh sửa đã đóng hoặc hết hạn")
        if (bytes.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "Tệp chỉnh sửa đang trống")
        if (bytes.size > maxSizeBytes) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp vượt quá dung lượng cho phép")
        val stored = storedFiles.findById(session.fileId).orElseThrow { fileNotFound() }
        val parent = versions.findFirstByFileIdOrderByVersionNumberDesc(session.fileId) ?: latestOrCreateInitial(stored)
        val id = UUID.randomUUID()
        val storageKey = "versions/${session.fileId}/${id}"
        val target = checkedPath(storageKey)
        Files.createDirectories(target.parent)
        val temp = Files.createTempFile(target.parent, ".edit-", ".tmp")
        try {
            Files.write(temp, bytes, StandardOpenOption.TRUNCATE_EXISTING)
            moveAtomically(temp, target)
            val digest = sha256(bytes)
            val version = versions.save(
                FileVersionEntity(
                    id = id,
                    fileId = session.fileId,
                    versionNumber = parent.versionNumber + 1,
                    storageKey = storageKey,
                    mediaType = mediaType,
                    sizeBytes = bytes.size.toLong(),
                    sha256 = digest,
                    sourceType = source,
                    parentVersionId = parent.id,
                    changeSummary = summary?.trim()?.takeIf(String::isNotBlank),
                    createdBy = session.userId,
                )
            )
            stored.storageKey = storageKey
            stored.contentType = mediaType
            stored.sizeBytes = bytes.size.toLong()
            stored.sha256 = digest
            return version
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun fetchOnlyOfficeResult(value: String): ByteArray {
        val uri = runCatching { URI.create(value) }.getOrElse { throw ApiException(HttpStatus.BAD_REQUEST, "EDITOR_URL_INVALID", "URL kết quả chỉnh sửa không hợp lệ") }
        val configured = runCatching { URI.create(onlyOfficeUrl) }.getOrNull()
        if (
            uri.scheme !in setOf("http", "https") ||
            configured?.host == null ||
            !uri.scheme.equals(configured.scheme, ignoreCase = true) ||
            !uri.host.equals(configured.host, ignoreCase = true) ||
            effectivePort(uri) != effectivePort(configured) ||
            uri.userInfo != null
        ) {
            throw ApiException(HttpStatus.BAD_REQUEST, "EDITOR_URL_NOT_ALLOWED", "Máy chủ kết quả không thuộc OnlyOffice đã cấu hình")
        }

        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = try {
            editorHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ApiException(HttpStatus.BAD_GATEWAY, "EDITOR_RESULT_INTERRUPTED", "Kết nối OnlyOffice bị gián đoạn")
        } catch (_: Exception) {
            throw ApiException(HttpStatus.BAD_GATEWAY, "EDITOR_RESULT_UNAVAILABLE", "Không thể tải kết quả từ OnlyOffice")
        }
        if (response.statusCode() !in 200..299) {
            response.body().close()
            throw ApiException(HttpStatus.BAD_GATEWAY, "EDITOR_RESULT_REJECTED", "OnlyOffice không trả kết quả thành công")
        }
        response.headers().firstValueAsLong("Content-Length").ifPresent { declared ->
            if (declared > maxSizeBytes) {
                response.body().close()
                throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp chỉnh sửa vượt quá dung lượng cho phép")
            }
        }

        return response.body().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxSizeBytes) {
                    throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp chỉnh sửa vượt quá dung lượng cho phép")
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray().takeIf { it.isNotEmpty() }
                ?: throw ApiException(HttpStatus.BAD_GATEWAY, "EDITOR_RESULT_EMPTY", "OnlyOffice không trả nội dung")
        }
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

    private fun requireReadable(fileId: UUID): StoredFileEntity {
        val file = storedFiles.findById(fileId).orElseThrow { fileNotFound() }
        if (file.status != StoredFileStatus.AVAILABLE) throw ApiException(HttpStatus.GONE, "FILE_UNAVAILABLE", "Tệp không còn khả dụng")
        if (file.ownerId != CurrentUser.id() && !hasAdministrativeFileAccess()) {
            throw ApiException(HttpStatus.FORBIDDEN, "FILE_READ_FORBIDDEN", "Bạn không có quyền đọc lịch sử tệp")
        }
        return file
    }

    private fun requireEditable(fileId: UUID): StoredFileEntity {
        val file = requireReadable(fileId)
        if (file.ownerId != CurrentUser.id() && !hasAdministrativeFileAccess()) {
            throw ApiException(HttpStatus.FORBIDDEN, "FILE_EDIT_FORBIDDEN", "Bạn không có quyền sửa tài liệu")
        }
        return file
    }

    private fun hasAdministrativeFileAccess(): Boolean =
        CurrentUser.isSystemAdmin() ||
            Permissions.FILES_EDIT in CurrentUser.authorities() ||
            Permissions.FILES_PUBLISH in CurrentUser.authorities() ||
            Permissions.OPERATIONS_MANAGE in CurrentUser.authorities()

    private fun requireOwnedSession(id: UUID): FileEditSessionEntity {
        val session = sessions.findById(id).orElseThrow { sessionNotFound() }
        if (session.userId != CurrentUser.id() && Permissions.OPERATIONS_MANAGE !in CurrentUser.authorities()) throw ApiException(HttpStatus.FORBIDDEN, "EDIT_SESSION_OWNER_MISMATCH", "Phiên chỉnh sửa không thuộc người dùng hiện tại")
        if (session.status == FileEditSessionStatus.OPEN && !session.expiresAt.isAfter(Instant.now())) {
            session.status = FileEditSessionStatus.EXPIRED
            session.closedAt = Instant.now()
            throw ApiException(HttpStatus.CONFLICT, "EDIT_SESSION_EXPIRED", "Phiên chỉnh sửa đã hết hạn")
        }
        return session
    }

    private fun requireTokenSession(id: UUID, token: String): FileEditSessionEntity {
        val session = sessions.findById(id).orElseThrow { sessionNotFound() }
        if (!MessageDigest.isEqual(session.lockTokenHash.toByteArray(), sha256(token.toByteArray()).toByteArray())) throw ApiException(HttpStatus.UNAUTHORIZED, "EDIT_TOKEN_INVALID", "Mã phiên chỉnh sửa không hợp lệ")
        if (session.status != FileEditSessionStatus.OPEN || !session.expiresAt.isAfter(Instant.now())) throw ApiException(HttpStatus.GONE, "EDIT_SESSION_CLOSED", "Phiên chỉnh sửa đã đóng hoặc hết hạn")
        return session
    }

    private fun validateEditor(mediaType: String, editor: FileEditorType) {
        when (editor) {
            FileEditorType.ONLYOFFICE, FileEditorType.COLLABORA -> if (mediaType !in setOf(DOCX_MEDIA_TYPE, MediaType.APPLICATION_PDF_VALUE)) throw ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "EDITABLE_DOCUMENT_REQUIRED", "Trình soạn thảo chỉ hỗ trợ DOCX hoặc PDF")
            FileEditorType.PDF_ANNOTATOR -> if (mediaType != MediaType.APPLICATION_PDF_VALUE) throw ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF_REQUIRED", "Trình chú thích chỉ hỗ trợ PDF")
        }
    }

    private fun checkedPath(key: String): Path = rootPath.resolve(key).normalize().also {
        if (!it.startsWith(rootPath)) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "Đường dẫn tệp không hợp lệ")
    }
    private fun randomToken(): String = ByteArray(32).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun moveAtomically(source: Path, target: Path) = try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE) } catch (_: AtomicMoveNotSupportedException) { Files.move(source, target) }
    private fun closeSaved(session: FileEditSessionEntity) { session.status = FileEditSessionStatus.SAVED; session.closedAt = Instant.now() }
    private fun fileNotFound() = ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "Không tìm thấy tệp")
    private fun versionNotFound() = ApiException(HttpStatus.NOT_FOUND, "FILE_VERSION_NOT_FOUND", "Không tìm thấy phiên bản tệp")
    private fun sessionNotFound() = ApiException(HttpStatus.NOT_FOUND, "EDIT_SESSION_NOT_FOUND", "Không tìm thấy phiên chỉnh sửa")
}

private fun FileVersionEntity.response() = FileVersionResponse(id, fileId, versionNumber, mediaType, sizeBytes, sha256, sourceType, parentVersionId, changeSummary, createdBy, createdAt)

@RestController
@RequestMapping("/api/v1/files")
class FileEditingController(private val service: FileEditingService) {
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('${Permissions.FILES_VERSION_READ}','${Permissions.FILES_EDIT}')")
    fun versions(@PathVariable id: UUID) = service.listVersions(id)

    @PostMapping("/{id}/edit-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('${Permissions.FILES_EDIT}')")
    fun createSession(@PathVariable id: UUID, @Valid @RequestBody input: CreateEditSessionRequest) = service.createSession(id, input)

    @PostMapping("/edit-sessions/{id}/pdf", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('${Permissions.FILES_EDIT}')")
    fun savePdf(@PathVariable id: UUID, @RequestPart("file") file: MultipartFile, @RequestParam(required = false) changeSummary: String?) = service.savePdf(id, file, changeSummary)

    @DeleteMapping("/edit-sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('${Permissions.FILES_EDIT}')")
    fun cancel(@PathVariable id: UUID) = service.cancel(id)
}

@RestController
@RequestMapping("/public/v1/file-edit")
class PublicFileEditingController(private val service: FileEditingService) {
    @GetMapping("/{id}/content")
    fun content(@PathVariable id: UUID, @RequestParam token: String) = service.publicContent(id, token)

    @PostMapping("/{id}/callback")
    fun callback(@PathVariable id: UUID, @RequestParam token: String, @RequestBody payload: OnlyOfficeCallback) = service.onlyOfficeCallback(id, token, payload)
}
