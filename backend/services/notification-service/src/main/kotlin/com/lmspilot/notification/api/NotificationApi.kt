package com.lmspilot.notification.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.contracts.*
import com.lmspilot.notification.domain.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.security.CurrentUser
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.min

@Configuration
class NotificationMessagingConfiguration {
    @Bean fun notificationQueue() = Queue("notification.business-events", true)
    @Bean fun notificationBinding(notificationQueue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(notificationQueue).to(domainEventExchange).with("#")
}

data class NotificationResponse(val id: UUID, val type: String, val title: String, val body: String, val read: Boolean, val createdAt: Instant, val readAt: Instant?)
data class NotificationSummary(val unread: Long, val items: List<NotificationResponse>)
data class NotificationMessage(val userId: UUID, val type: String, val title: String, val body: String, val variables: Map<String, Any?> = emptyMap())
data class InternalUserContact(val userId: UUID, val username: String, val fullName: String, val email: String?, val active: Boolean)

@Component
class IdentityContactClient(
    builder: RestClient.Builder,
    @Value("\${identity-service.url:http://localhost:8081}") baseUrl: String,
    @Value("\${lmspilot.internal-token}") private val serviceToken: String,
) {
    private val client = builder.baseUrl(baseUrl).build()

    fun get(userId: UUID): InternalUserContact = client.get()
        .uri("/internal/v1/users/{id}/contact", userId)
        .header("X-Service-Token", serviceToken)
        .retrieve()
        .body(InternalUserContact::class.java)
        ?: error("Identity service returned an empty contact response")
}

@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val mapper: ObjectMapper,
    private val templates: NotificationTemplateService,
    @Value("\${notification.email-enabled:false}") private val emailEnabled: Boolean,
    @Value("\${notification.email-processing-lease:PT5M}") private val emailProcessingLease: Duration,
) {
    @RabbitListener(queues = ["notification.business-events"])
    @Transactional
    fun consume(event: DomainEventEnvelope) {
        val message = when (event.eventType) {
            EventTypes.ENROLLED -> mapper.treeToValue(event.payload, EnrolledPayload::class.java).let {
                NotificationMessage(it.userId, "ENROLLED", "Bạn đã được ghi danh", "Một khóa học mới đã được thêm vào trang học tập của bạn.", mapOf("courseId" to it.courseId, "classId" to it.classId, "dueAt" to it.dueAt))
            }
            EventTypes.EXAM_GRADED -> mapper.treeToValue(event.payload, ExamGradedPayload::class.java).let {
                val effectivePassed = it.effectivePassed ?: it.passed
                NotificationMessage(
                    it.userId,
                    "EXAM_GRADED",
                    "Đã có kết quả bài kiểm tra",
                    "Kết quả theo chiến lược ${it.scoreStrategy}: ${if (effectivePassed) "Đạt" else "Chưa đạt"}.",
                    mapOf(
                        "courseId" to it.courseId,
                        "lessonId" to it.lessonId,
                        "enrollmentId" to it.enrollmentId,
                        "score" to it.score,
                        "maxScore" to it.maxScore,
                        "effectivePercentage" to it.effectivePercentage,
                        "passed" to effectivePassed,
                    ),
                )
            }
            EventTypes.COURSE_COMPLETED -> mapper.treeToValue(event.payload, CourseCompletedPayload::class.java).let {
                NotificationMessage(it.userId, "COURSE_COMPLETED", "Bạn đã hoàn thành khóa học", "Chúc mừng bạn đã hoàn thành đầy đủ các điều kiện của khóa học.", mapOf("courseId" to it.courseId, "completedAt" to it.completedAt))
            }
            EventTypes.GRADE_APPEAL_RESOLVED -> {
                val userId = event.payload.path("userId").asText().takeIf { it.isNotBlank() }?.let(UUID::fromString) ?: return
                val status = event.payload.path("status").asText()
                val approved = status == "APPROVED"
                NotificationMessage(userId, "GRADE_APPEAL_RESOLVED", "Yêu cầu phúc khảo đã được xử lý", if (approved) "Yêu cầu phúc khảo đã được chấp thuận. Hãy kiểm tra lại kết quả." else "Yêu cầu phúc khảo đã được phản hồi. Hãy mở kết quả để xem nội dung xử lý.", mapOf("status" to status))
            }
            EventTypes.CERTIFICATE_ISSUED -> {
                val userId = event.payload.path("userId").asText().takeIf { it.isNotBlank() }?.let(UUID::fromString) ?: return
                NotificationMessage(userId, "CERTIFICATE_ISSUED", "Chứng chỉ đã được cấp", "Chứng chỉ mới hiện đã có trong hồ sơ học tập của bạn.", mapOf("certificateId" to event.aggregateId))
            }
            else -> return
        }
        val resolved = templates.resolve(event.eventType, message.title, message.body, message.variables)
        enqueue(event.eventId, message.copy(title = resolved.title, body = resolved.body), resolved.channels)
    }

    @Transactional
    fun enqueue(sourceEventId: UUID, message: NotificationMessage, channels: Set<NotificationChannel>): Int {
        val now = Instant.now()
        var created = 0
        if (NotificationChannel.IN_APP in channels) {
            created += repository.insertIfAbsent(UUID.randomUUID(), sourceEventId, message.userId, message.type.take(120), message.title.take(240), message.body.take(100000), NotificationChannel.IN_APP.name, now, null)
        }
        if (emailEnabled && NotificationChannel.EMAIL in channels) {
            created += repository.insertIfAbsent(UUID.randomUUID(), sourceEventId, message.userId, message.type.take(120), message.title.take(240), message.body.take(100000), NotificationChannel.EMAIL.name, now, now)
        }
        return created
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
        entity.read = true
        entity.readAt = Instant.now()
        return entity.response()
    }

    @Transactional
    fun claimDueEmails(limit: Int): List<UUID> {
        val leaseUntil = Instant.now().plus(emailProcessingLease.coerceAtLeast(Duration.ofSeconds(30)))
        return repository.lockDueEmails(limit.coerceIn(1, 200)).map {
            it.deliveryStatus = DeliveryStatus.PROCESSING
            // nextAttemptAt doubles as the processing lease deadline. If the process dies after
            // claiming this row, another worker can safely reclaim it after the lease expires.
            it.nextAttemptAt = leaseUntil
            it.id
        }
    }
}

@Service
class EmailDeliveryService(
    private val repository: NotificationRepository,
    private val contacts: IdentityContactClient,
    private val mailSender: JavaMailSender,
    @Value("\${notification.from:no-reply@lmspilot.local}") private val from: String,
    @Value("\${notification.email-max-attempts:8}") private val maxAttempts: Int,
    @Value("\${notification.email-initial-backoff:PT1M}") private val initialBackoff: Duration,
    @Value("\${notification.email-max-backoff:PT6H}") private val maxBackoff: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun deliver(id: UUID) {
        val entity = repository.findById(id).orElse(null) ?: return
        if (entity.channel != NotificationChannel.EMAIL || entity.deliveryStatus != DeliveryStatus.PROCESSING) return
        entity.attemptCount += 1
        try {
            val contact = contacts.get(entity.userId)
            if (!contact.active || contact.email.isNullOrBlank()) {
                entity.deliveryStatus = DeliveryStatus.SKIPPED
                entity.lastError = if (!contact.active) "Tài khoản không hoạt động" else "Tài khoản chưa có email"
                entity.deliveredAt = Instant.now()
                entity.nextAttemptAt = null
                return
            }

            val destination = contact.email.trim()
            entity.recipientEmail = destination
            mailSender.send(SimpleMailMessage().apply {
                setFrom(from)
                setTo(destination)
                setSubject(entity.title)
                setText(entity.body)
            })
            entity.deliveryStatus = DeliveryStatus.SENT
            entity.deliveredAt = Instant.now()
            entity.lastError = null
            entity.nextAttemptAt = null
        } catch (cause: Exception) {
            entity.lastError = (cause.message ?: cause.javaClass.simpleName).take(2000)
            if (entity.attemptCount >= maxAttempts.coerceAtLeast(1)) {
                entity.deliveryStatus = DeliveryStatus.DEAD
                entity.nextAttemptAt = null
            } else {
                entity.deliveryStatus = DeliveryStatus.FAILED
                entity.nextAttemptAt = Instant.now().plus(backoff(entity.attemptCount))
            }
            log.warn("Email delivery failed notification={} attempt={}", entity.id, entity.attemptCount, cause)
        }
    }

    private fun backoff(attempt: Int): Duration {
        val exponent = (attempt - 1).coerceIn(0, 20)
        val multiplier = 1L shl exponent
        val millis = runCatching { Math.multiplyExact(initialBackoff.toMillis(), multiplier) }.getOrDefault(Long.MAX_VALUE)
        return Duration.ofMillis(min(millis, maxBackoff.toMillis()))
    }
}

@Component
class EmailOutboxScheduler(
    private val notifications: NotificationService,
    private val delivery: EmailDeliveryService,
    @Value("\${notification.email-batch-size:50}") private val batchSize: Int,
) {
    @Scheduled(fixedDelayString = "\${notification.email-worker-delay-ms:15000}")
    fun run() {
        notifications.claimDueEmails(batchSize).forEach(delivery::deliver)
    }
}

private fun NotificationEntity.response() = NotificationResponse(id, type, title, body, read, createdAt, readAt)

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(private val service: NotificationService) {
    @GetMapping fun mine() = service.mine()
    @PutMapping("/{id}/read") fun read(@PathVariable id: UUID) = service.markRead(id)
}
