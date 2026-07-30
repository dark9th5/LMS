package com.lmspilot.notification.config

import com.lmspilot.notification.domain.NotificationChannel
import com.lmspilot.notification.domain.NotificationEntity
import com.lmspilot.notification.domain.NotificationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DevelopmentSeed(
    private val notifications: NotificationRepository,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled) return
        seed(
            eventId = UUID.fromString("00000000-0000-0000-0000-000000000401"),
            userId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            title = "Hệ thống demo đã sẵn sàng",
            body = "Bạn có thể kiểm tra người dùng, khóa học, lớp và báo cáo mẫu.",
        )
        seed(
            eventId = UUID.fromString("00000000-0000-0000-0000-000000000402"),
            userId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            title = "Bạn được phân công lớp mẫu",
            body = "Lớp làm quen LMSPilot đang mở trong phạm vi giảng dạy của bạn.",
        )
        seed(
            eventId = UUID.fromString("00000000-0000-0000-0000-000000000403"),
            userId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
            title = "Bạn đã được ghi danh",
            body = "Khóa học làm quen LMSPilot đã xuất hiện trong trang học tập cá nhân.",
        )
    }

    private fun seed(eventId: UUID, userId: UUID, title: String, body: String) {
        if (!notifications.existsBySourceEventIdAndUserIdAndChannel(eventId, userId, NotificationChannel.IN_APP)) {
            notifications.save(
                NotificationEntity(
                    id = eventId,
                    sourceEventId = eventId,
                    userId = userId,
                    type = "DEMO",
                    title = title,
                    body = body,
                )
            )
        }
    }
}
