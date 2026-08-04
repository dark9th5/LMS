package com.lmspilot.storage.cls

import java.time.Instant
import java.util.UUID

enum class EditorType { ONLYOFFICE, COLLABORA, PDF_ANNOTATOR }
enum class FileChangeSource { UPLOAD, DOCX_EDIT, PDF_ANNOTATION, CONVERSION }

data class FileVersionSpec(
    val fileId: UUID,
    val parentVersionId: UUID?,
    val mediaType: String,
    val source: FileChangeSource,
    val sha256: String,
) {
    init {
        require(Regex("^[0-9a-f]{64}$").matches(sha256)) { "sha256 must be lowercase hexadecimal" }
        if (source == FileChangeSource.DOCX_EDIT) {
            require(mediaType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        }
        if (source == FileChangeSource.PDF_ANNOTATION) require(mediaType == "application/pdf")
    }
}

data class EditSession(
    val id: UUID,
    val fileId: UUID,
    val baseVersionId: UUID,
    val userId: UUID,
    val editorType: EditorType,
    val expiresAt: Instant,
) {
    fun isExpired(now: Instant): Boolean = !expiresAt.isAfter(now)
}
