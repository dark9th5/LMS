package com.lmspilot.notification.cls

import java.time.Instant
import java.util.UUID

enum class NewsAudienceType { SYSTEM, BRANCH, DEPARTMENT, GROUP }

data class NewsAudience(val type: NewsAudienceType, val id: UUID?) {
    init {
        require((type == NewsAudienceType.SYSTEM) == (id == null)) { "Invalid audience id" }
    }
}

data class NewsPublicationWindow(val from: Instant?, val until: Instant?) {
    init {
        require(until == null || from == null || until.isAfter(from)) { "Invalid publication window" }
    }

    fun visibleAt(now: Instant): Boolean =
        (from == null || !from.isAfter(now)) && (until == null || until.isAfter(now))
}
