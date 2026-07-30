package com.lmspilot.certificate.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.certificate.domain.*
import com.lmspilot.contracts.*
import com.lmspilot.support.api.ApiException
import com.lmspilot.support.events.DomainEventPublisher
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

@Configuration
class CertificateMessagingConfiguration {
    @Bean fun certificateQueue() = Queue("certificate.course-completed", true)
    @Bean fun certificateBinding(certificateQueue: Queue, domainEventExchange: TopicExchange) = BindingBuilder.bind(certificateQueue).to(domainEventExchange).with(EventTypes.COURSE_COMPLETED)
}

data class CertificateResponse(val id: UUID, val enrollmentId: UUID, val courseId: UUID, val userId: UUID, val verificationCode: String, val generation: Int, val status: CertificateStatus, val issuedAt: Instant, val revokedAt: Instant?, val revokeReason: String?)
data class CertificateIssuedEvent(val certificateId: UUID, val enrollmentId: UUID, val courseId: UUID, val userId: UUID, val verificationCode: String, val issuedAt: Instant)
data class RevokeRequest(@field:NotBlank val reason: String)

@Service
class CertificateService(private val repository: CertificateRepository, private val mapper: ObjectMapper, private val events: DomainEventPublisher) {
    private val random = SecureRandom()

    @RabbitListener(queues = ["certificate.course-completed"])
    @Transactional
    fun consume(event: DomainEventEnvelope) {
        val payload = mapper.treeToValue(event.payload, CourseCompletedPayload::class.java)
        if (repository.findAllByEnrollmentIdOrderByGenerationDesc(payload.enrollmentId).any { it.status == CertificateStatus.ACTIVE }) return
        issue(payload.enrollmentId, payload.courseId, payload.userId, null)
    }

    @Transactional
    fun issue(enrollmentId: UUID, courseId: UUID, userId: UUID, replaces: UUID?): CertificateResponse {
        val generation = (repository.findAllByEnrollmentIdOrderByGenerationDesc(enrollmentId).maxOfOrNull { it.generation } ?: 0) + 1
        val entity = repository.save(CertificateEntity(enrollmentId = enrollmentId, courseId = courseId, userId = userId, verificationCode = nextCode(), generation = generation, replacesCertificateId = replaces))
        events.publish(EventTypes.CERTIFICATE_ISSUED, "certificate-service", entity.id.toString(), CertificateIssuedEvent(entity.id, enrollmentId, courseId, userId, entity.verificationCode, entity.issuedAt))
        return entity.response()
    }

    @Transactional(readOnly = true)
    fun all() = repository.findAllByOrderByIssuedAtDesc().map { it.response() }

    @Transactional(readOnly = true)
    fun mine() = repository.findAllByUserIdOrderByIssuedAtDesc(CurrentUser.id()).map { it.response() }

    @Transactional(readOnly = true)
    fun verify(code: String): CertificateResponse = repository.findByVerificationCode(code.uppercase())?.response() ?: throw ApiException(HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_FOUND", "Không tìm thấy chứng chỉ")

    @Transactional
    fun revoke(id: UUID, input: RevokeRequest): CertificateResponse {
        val entity = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_FOUND", "Không tìm thấy chứng chỉ") }
        if (entity.status != CertificateStatus.ACTIVE) throw ApiException(HttpStatus.CONFLICT, "CERTIFICATE_NOT_ACTIVE", "Chứng chỉ không còn hiệu lực")
        entity.status = CertificateStatus.REVOKED; entity.revokedAt = Instant.now(); entity.revokeReason = input.reason.trim()
        return entity.response()
    }

    @Transactional
    fun reissue(id: UUID, input: RevokeRequest): CertificateResponse {
        val old = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_FOUND", "Không tìm thấy chứng chỉ") }
        if (old.status == CertificateStatus.ACTIVE) { old.status = CertificateStatus.REISSUED; old.revokedAt = Instant.now(); old.revokeReason = input.reason.trim() }
        return issue(old.enrollmentId, old.courseId, old.userId, old.id)
    }

    @Transactional(readOnly = true)
    fun printable(id: UUID): String {
        val c = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_FOUND", "Không tìm thấy chứng chỉ") }
        if (c.userId != CurrentUser.id() && !CurrentUser.authorities().contains(Permissions.CERTIFICATES_MANAGE)) throw ApiException(HttpStatus.FORBIDDEN, "CERTIFICATE_SCOPE", "Bạn không có quyền xem chứng chỉ")
        return """<!doctype html>
            <html lang="vi">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Chứng chỉ LMSPilot</title>
              <style>
                body{font-family:Arial,sans-serif;background:#f4f7fb;padding:40px}
                .certificate{max-width:900px;margin:auto;background:white;border:12px solid #173b65;padding:80px;text-align:center}
                .title{font-size:44px;color:#173b65}
                .code{font-family:monospace;font-size:20px}
              </style>
            </head>
            <body>
              <main class="certificate">
                <div class="title">CHỨNG CHỈ HOÀN THÀNH</div>
                <p>Xác nhận học viên <b>${c.userId}</b></p>
                <p>đã hoàn thành khóa học <b>${c.courseId}</b></p>
                <p>Ngày cấp: ${c.issuedAt}</p>
                <p class="code">Mã xác minh: ${c.verificationCode}</p>
                <p>Trạng thái: ${c.status}</p>
              </main>
            </body>
            </html>""".trimIndent()
    }

    private fun nextCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(20) {
            val code = buildString { repeat(16) { append(alphabet[random.nextInt(alphabet.length)]) } }.chunked(4).joinToString("-")
            if (repository.findByVerificationCode(code) == null) return code
        }
        throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CERTIFICATE_CODE_FAILURE", "Không thể tạo mã chứng chỉ")
    }
}
private fun CertificateEntity.response() = CertificateResponse(id, enrollmentId, courseId, userId, verificationCode, generation, status, issuedAt, revokedAt, revokeReason)

@RestController
@RequestMapping("/api/v1/certificates")
class CertificateController(private val service: CertificateService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_MANAGE}')") fun all() = service.all()
    @GetMapping("/me") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_READ_SELF}')") fun mine() = service.mine()
    @GetMapping("/{id}/print", produces = [MediaType.TEXT_HTML_VALUE]) @PreAuthorize("isAuthenticated()") fun print(@PathVariable id: UUID) = service.printable(id)
    @PutMapping("/{id}/revoke") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_MANAGE}')") fun revoke(@PathVariable id: UUID, @Valid @RequestBody input: RevokeRequest) = service.revoke(id, input)
    @PostMapping("/{id}/reissue") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_MANAGE}')") fun reissue(@PathVariable id: UUID, @Valid @RequestBody input: RevokeRequest) = service.reissue(id, input)
}

@RestController
@RequestMapping("/public/v1/certificates")
class PublicCertificateController(private val service: CertificateService) {
    @GetMapping("/{code}") fun verify(@PathVariable code: String) = service.verify(code)
}
