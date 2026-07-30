package com.lmspilot.notification.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.*
import com.lmspilot.notification.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@Configuration
class NotificationMessagingConfiguration {
    @Bean fun notificationQueue() = Queue("notification.business-events", true)
    @Bean fun notificationBinding(notificationQueue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(notificationQueue).to(domainEventExchange).with("#")
}

data class NotificationResponse(val id: UUID, val type: String, val title: String, val body: String, val read: Boolean, val createdAt: Instant, val readAt: Instant?)
data class NotificationSummary(val unread: Long, val items: List<NotificationResponse>)
data class NotificationMessage(val userId: UUID, val type: String, val title: String, val body: String)

@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val mapper: ObjectMapper,
    private val mailSender: JavaMailSender,
    @Value("\${notification.email-enabled:false}") private val emailEnabled: Boolean,
    @Value("\${notification.from:no-reply@lmspilot.local}") private val from: String,
) {
    @RabbitListener(queues = ["notification.business-events"])
    @Transactional
    fun consume(event: DomainEventEnvelope) {
        val message = when (event.eventType) {
            EventTypes.ENROLLED -> mapper.treeToValue(event.payload, EnrolledPayload::class.java).let { NotificationMessage(it.userId, "ENROLLED", "Bạn đã được ghi danh", "Một khóa học mới đã được thêm vào trang học tập của bạn.") }
            EventTypes.EXAM_GRADED -> mapper.treeToValue(event.payload, ExamGradedPayload::class.java).let { NotificationMessage(it.userId, "EXAM_GRADED", "Đã có kết quả bài kiểm tra", "Kết quả: ${if (it.passed) "Đạt" else "Chưa đạt"}.") }
            EventTypes.COURSE_COMPLETED -> mapper.treeToValue(event.payload, CourseCompletedPayload::class.java).let { NotificationMessage(it.userId, "COURSE_COMPLETED", "Bạn đã hoàn thành khóa học", "Chúc mừng bạn đã hoàn thành đầy đủ các điều kiện của khóa học.") }
            EventTypes.CERTIFICATE_ISSUED -> {
                val userId = event.payload.path("userId").asText().takeIf { it.isNotBlank() }?.let(UUID::fromString) ?: return
                NotificationMessage(userId, "CERTIFICATE_ISSUED", "Chứng chỉ đã được cấp", "Chứng chỉ mới hiện đã có trong hồ sơ học tập của bạn.")
            }
            else -> return
        }
        if (!repository.existsBySourceEventIdAndUserIdAndChannel(event.eventId, message.userId, NotificationChannel.IN_APP)) repository.save(NotificationEntity(sourceEventId = event.eventId, userId = message.userId, type = message.type, title = message.title, body = message.body))
    }

    @Transactional(readOnly = true)
    fun mine(): NotificationSummary {
        val user = CurrentUser.id()
        return NotificationSummary(repository.countByUserIdAndChannelAndReadFalse(user, NotificationChannel.IN_APP), repository.findAllByUserIdAndChannelOrderByCreatedAtDesc(user, NotificationChannel.IN_APP).take(100).map { it.response() })
    }

    @Transactional
    fun markRead(id: UUID): NotificationResponse {
        val entity = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo") }
        if (entity.userId != CurrentUser.id()) throw ApiException(HttpStatus.FORBIDDEN, "NOTIFICATION_OWNER_MISMATCH", "Thông báo không thuộc người dùng hiện tại")
        entity.read = true; entity.readAt = Instant.now()
        return entity.response()
    }

    fun sendEmail(to: String, subject: String, body: String) {
        if (!emailEnabled) return
        runCatching { mailSender.send(SimpleMailMessage().apply { setFrom(from); setTo(to); setSubject(subject); setText(body) }) }
    }
}
private fun NotificationEntity.response() = NotificationResponse(id, type, title, body, read, createdAt, readAt)

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(private val service: NotificationService) {
    @GetMapping fun mine() = service.mine()
    @PutMapping("/{id}/read") fun read(@PathVariable id: UUID) = service.markRead(id)
}
