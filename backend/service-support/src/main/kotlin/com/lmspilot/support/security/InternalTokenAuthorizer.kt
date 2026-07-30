package com.lmspilot.support.security

import com.lmspilot.support.api.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class InternalTokenAuthorizer(@Value("\${lmspilot.internal-token}") private val expected: String) {
    fun require(token: String?) {
        val supplied = token?.toByteArray() ?: byteArrayOf()
        val configured = expected.toByteArray()
        if (configured.isEmpty() || !MessageDigest.isEqual(supplied, configured)) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SERVICE_TOKEN", "Internal service token is invalid")
        }
    }
}
