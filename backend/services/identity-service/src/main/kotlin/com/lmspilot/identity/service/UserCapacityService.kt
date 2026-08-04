package com.lmspilot.identity.service

import com.lmspilot.identity.domain.AccountStatus
import com.lmspilot.identity.domain.IdentitySystemLockRepository
import com.lmspilot.identity.domain.UserAccountRepository
import com.lmspilot.support.api.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Serializes active-user capacity checks across every identity-service instance.
 * The database row lock prevents concurrent create/import requests from exceeding
 * the offline license user limit after both callers observed the same count.
 */
@Service
class UserCapacityService(
    private val locks: IdentitySystemLockRepository,
    private val users: UserAccountRepository,
    private val license: LicenseEntitlementClient,
) {
    fun requireCapacity(additionalUsers: Int) {
        if (additionalUsers <= 0) return
        locks.lock(ACTIVE_USER_CAPACITY_LOCK)
            ?: throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CAPACITY_LOCK_UNAVAILABLE", "Không thể khóa kiểm tra giới hạn người dùng")
        license.requireUserCapacity(users.countByStatusNot(AccountStatus.DISABLED), additionalUsers)
    }

    companion object {
        const val ACTIVE_USER_CAPACITY_LOCK = "ACTIVE_USER_LICENSE_CAPACITY"
    }
}
