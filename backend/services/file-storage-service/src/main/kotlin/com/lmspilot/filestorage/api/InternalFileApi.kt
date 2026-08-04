package com.lmspilot.filestorage.api

import com.lmspilot.support.security.InternalTokenAuthorizer
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class GrantFileAccessRequest(
    val userId: UUID,
    val fileIds: Set<UUID>,
    val source: String,
    val ttlSeconds: Long = 3600,
)

@RestController
@RequestMapping("/internal/v1/files")
class InternalFileController(
    private val service: FileStorageService,
    private val internal: InternalTokenAuthorizer,
) {
    @GetMapping("/{id}/content")
    fun content(
        @PathVariable id: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ) = internal.require(token).let { service.internalDownload(id, true) }

    @GetMapping("/{id}")
    fun metadata(
        @PathVariable id: UUID,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ) = internal.require(token).let { service.internalMetadata(id) }

    @PostMapping("/access-grants")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun grant(
        @RequestBody input: GrantFileAccessRequest,
        @RequestHeader("X-Service-Token", required = false) token: String?,
    ) {
        internal.require(token)
        service.grantAccess(input.userId, input.fileIds, input.source, input.ttlSeconds)
    }
}
