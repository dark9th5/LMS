package com.lmspilot.identity.service

import com.lmspilot.identity.domain.BulkOperationEntity
import com.lmspilot.identity.domain.BulkOperationRepository
import com.lmspilot.identity.domain.IdentitySystemLockRepository
import com.lmspilot.support.api.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Serializes every identity bulk mutation and validates idempotent replays.
 * Callers must invoke this inside their transaction before making any changes.
 */
@Service
class BulkOperationGuard(
    private val locks: IdentitySystemLockRepository,
    private val operations: BulkOperationRepository,
) {
    fun replay(operationId: String, expectedType: String, requestedBy: UUID): BulkOperationEntity? {
        locks.lock(BULK_OPERATION_LOCK)
            ?: throw ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "BULK_OPERATION_LOCK_UNAVAILABLE",
                "Không thể khóa thao tác hàng loạt",
            )

        val saved = operations.findById(operationId).orElse(null) ?: return null
        if (saved.operationType != expectedType || saved.requestedBy != requestedBy) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "OPERATION_ID_REUSED",
                "operationId đã được dùng cho người dùng hoặc thao tác khác",
            )
        }
        return saved
    }

    companion object {
        const val BULK_OPERATION_LOCK = "IDENTITY_BULK_OPERATION_SERIALIZATION"
    }
}
