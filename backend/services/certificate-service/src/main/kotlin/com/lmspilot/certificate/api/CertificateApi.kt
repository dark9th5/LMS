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
data class CertificateTemplateRequest(
    @field:NotBlank val name: String,
    val courseId: UUID? = null,
    @field:NotBlank val title: String = "CHỨNG CHỈ HOÀN THÀNH",
    @field:NotBlank val issuerName: String = "LMSPilot",
    @field:NotBlank val bodyText: String = "Xác nhận người học đã hoàn thành chương trình đào tạo.",
    val primaryColor: String = "#173b65",
    val secondaryColor: String = "#b99044",
    val logoUrl: String? = null,
    val signatureName: String? = null,
    val active: Boolean = true,
)
data class CertificateTemplateResponse(val id: UUID, val name: String, val courseId: UUID?, val title: String, val issuerName: String, val bodyText: String, val primaryColor: String, val secondaryColor: String, val logoUrl: String?, val signatureName: String?, val active: Boolean, val updatedAt: Instant)
data class CertificateTemplateSnapshot(val title: String, val issuerName: String, val bodyText: String, val primaryColor: String, val secondaryColor: String, val logoUrl: String?, val signatureName: String?)

@Service
class CertificateService(private val repository: CertificateRepository, private val templates: CertificateTemplateRepository, private val mapper: ObjectMapper, private val events: DomainEventPublisher) {
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
        val template = templates.findFirstByCourseIdAndActiveTrueOrderByUpdatedAtDesc(courseId)
            ?: templates.findFirstByCourseIdIsNullAndActiveTrueOrderByUpdatedAtDesc()
        val snapshot = template?.snapshot() ?: CertificateTemplateSnapshot("CHỨNG CHỈ HOÀN THÀNH", "LMSPilot", "Xác nhận người học đã hoàn thành chương trình đào tạo.", "#173b65", "#b99044", null, null)
        val entity = repository.save(CertificateEntity(enrollmentId = enrollmentId, courseId = courseId, userId = userId, verificationCode = nextCode(), generation = generation, replacesCertificateId = replaces, templateId = template?.id, templateSnapshotJson = mapper.writeValueAsString(snapshot)))
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
    fun listTemplates() = templates.findAllByOrderByUpdatedAtDesc().map { it.templateResponse() }

    @Transactional
    fun createTemplate(input: CertificateTemplateRequest): CertificateTemplateResponse {
        validateTemplate(input)
        return templates.save(input.toEntity(CurrentUser.id())).templateResponse()
    }

    @Transactional
    fun updateTemplate(id: UUID, input: CertificateTemplateRequest): CertificateTemplateResponse {
        validateTemplate(input)
        val entity = templates.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu chứng chỉ") }
        entity.name = input.name.trim(); entity.courseId = input.courseId; entity.title = input.title.trim(); entity.issuerName = input.issuerName.trim()
        entity.bodyText = input.bodyText.trim(); entity.primaryColor = input.primaryColor.lowercase(); entity.secondaryColor = input.secondaryColor.lowercase()
        entity.logoUrl = input.logoUrl?.trim()?.takeIf { it.isNotBlank() }; entity.signatureName = input.signatureName?.trim()?.takeIf { it.isNotBlank() }
        entity.active = input.active; entity.updatedAt = Instant.now()
        return entity.templateResponse()
    }

    @Transactional
    fun disableTemplate(id: UUID) {
        val entity = templates.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu chứng chỉ") }
        entity.active = false
        entity.updatedAt = Instant.now()
    }

    @Transactional(readOnly = true)
    fun printable(id: UUID): String {
        val c = repository.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_FOUND", "Không tìm thấy chứng chỉ") }
        if (c.userId != CurrentUser.id() && !CurrentUser.authorities().contains(Permissions.CERTIFICATES_MANAGE)) throw ApiException(HttpStatus.FORBIDDEN, "CERTIFICATE_SCOPE", "Bạn không có quyền xem chứng chỉ")
        val template = runCatching { mapper.readValue(c.templateSnapshotJson, CertificateTemplateSnapshot::class.java) }
            .getOrElse { CertificateTemplateSnapshot("CHỨNG CHỈ HOÀN THÀNH", "LMSPilot", "Xác nhận người học đã hoàn thành chương trình đào tạo.", "#173b65", "#b99044", null, null) }
        val logo = template.logoUrl?.let { "<img class=\"logo\" src=\"${escapeHtml(it)}\" alt=\"Logo\">" } ?: ""
        val signature = template.signatureName?.let { "<p class=\"signature\">${escapeHtml(it)}</p>" } ?: ""
        return """<!doctype html>
            <html lang="vi">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>${escapeHtml(template.title)}</title>
              <style>
                body{font-family:Arial,sans-serif;background:#f4f7fb;padding:40px}
                .certificate{max-width:900px;margin:auto;background:white;border:12px solid ${template.primaryColor};box-shadow:inset 0 0 0 3px ${template.secondaryColor};padding:70px;text-align:center}
                .logo{max-height:84px;max-width:220px}.issuer{letter-spacing:.18em;text-transform:uppercase;color:${template.secondaryColor}}
                .title{font-size:44px;color:${template.primaryColor};margin:24px 0}.body{font-size:20px;line-height:1.7}.code{font-family:monospace;font-size:20px}.signature{margin-top:44px;font-weight:700}
              </style>
            </head>
            <body>
              <main class="certificate">
                $logo
                <p class="issuer">${escapeHtml(template.issuerName)}</p>
                <div class="title">${escapeHtml(template.title)}</div>
                <p class="body">${escapeHtml(template.bodyText)}</p>
                <p>Xác nhận học viên <b>${c.userId}</b></p>
                <p>đã hoàn thành khóa học <b>${c.courseId}</b></p>
                <p>Ngày cấp: ${c.issuedAt}</p>
                <p class="code">Mã xác minh: ${c.verificationCode}</p>
                <p>Trạng thái: ${c.status}</p>
                $signature
              </main>
            </body>
            </html>""".trimIndent()
    }

    private fun validateTemplate(input: CertificateTemplateRequest) {
        val color = Regex("^#[0-9a-fA-F]{6}$")
        if (!color.matches(input.primaryColor) || !color.matches(input.secondaryColor)) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_COLOR", "Màu mẫu chứng chỉ phải ở dạng #RRGGBB")
        if (input.name.length > 180 || input.title.length > 240 || input.issuerName.length > 240 || input.bodyText.length > 1000) throw ApiException(HttpStatus.BAD_REQUEST, "TEMPLATE_TOO_LONG", "Nội dung mẫu chứng chỉ quá dài")
        input.logoUrl?.let { if (!it.startsWith("/api/v1/files/") && !it.startsWith("/public/")) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_LOGO", "Logo phải là tài nguyên nội bộ") }
    }

    private fun escapeHtml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun nextCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(20) {
            val code = buildString { repeat(16) { append(alphabet[random.nextInt(alphabet.length)]) } }.chunked(4).joinToString("-")
            if (repository.findByVerificationCode(code) == null) return code
        }
        throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CERTIFICATE_CODE_FAILURE", "Không thể tạo mã chứng chỉ")
    }
}
private fun CertificateTemplateRequest.toEntity(actorId: UUID) = CertificateTemplateEntity(name = name.trim(), courseId = courseId, title = title.trim(), issuerName = issuerName.trim(), bodyText = bodyText.trim(), primaryColor = primaryColor.lowercase(), secondaryColor = secondaryColor.lowercase(), logoUrl = logoUrl?.trim()?.takeIf { it.isNotBlank() }, signatureName = signatureName?.trim()?.takeIf { it.isNotBlank() }, active = active, createdBy = actorId)
private fun CertificateTemplateEntity.templateResponse() = CertificateTemplateResponse(id, name, courseId, title, issuerName, bodyText, primaryColor, secondaryColor, logoUrl, signatureName, active, updatedAt)
private fun CertificateTemplateEntity.snapshot() = CertificateTemplateSnapshot(title, issuerName, bodyText, primaryColor, secondaryColor, logoUrl, signatureName)

private fun CertificateEntity.response() = CertificateResponse(id, enrollmentId, courseId, userId, verificationCode, generation, status, issuedAt, revokedAt, revokeReason)

@RestController
@RequestMapping("/api/v1/certificates")
class CertificateController(private val service: CertificateService) {
    @GetMapping @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_MANAGE}')") fun all() = service.all()
    @GetMapping("/me") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_READ_SELF}')") fun mine() = service.mine()
    @GetMapping("/{id}/print", produces = [MediaType.TEXT_HTML_VALUE]) @PreAuthorize("isAuthenticated()") fun print(@PathVariable id: UUID) = service.printable(id)
    @PutMapping("/{id}/revoke") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_MANAGE}')") fun revoke(@PathVariable id: UUID, @Valid @RequestBody input: RevokeRequest) = service.revoke(id, input)
    @PostMapping("/{id}/reissue") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATES_MANAGE}')") fun reissue(@PathVariable id: UUID, @Valid @RequestBody input: RevokeRequest) = service.reissue(id, input)
    @GetMapping("/templates") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATE_TEMPLATES_MANAGE}')") fun templates() = service.listTemplates()
    @PostMapping("/templates") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATE_TEMPLATES_MANAGE}')") fun createTemplate(@Valid @RequestBody input: CertificateTemplateRequest) = service.createTemplate(input)
    @PutMapping("/templates/{id}") @PreAuthorize("hasAuthority('${Permissions.CERTIFICATE_TEMPLATES_MANAGE}')") fun updateTemplate(@PathVariable id: UUID, @Valid @RequestBody input: CertificateTemplateRequest) = service.updateTemplate(id, input)
    @DeleteMapping("/templates/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('${Permissions.CERTIFICATE_TEMPLATES_MANAGE}')") fun disableTemplate(@PathVariable id: UUID) = service.disableTemplate(id)
}

@RestController
@RequestMapping("/public/v1/certificates")
class PublicCertificateController(private val service: CertificateService) {
    @GetMapping("/{code}") fun verify(@PathVariable code: String) = service.verify(code)
}
