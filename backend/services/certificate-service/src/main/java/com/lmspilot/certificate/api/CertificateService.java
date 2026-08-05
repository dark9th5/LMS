package com.lmspilot.certificate.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmspilot.certificate.domain.CertificateEntity;
import com.lmspilot.certificate.domain.CertificateRepository;
import com.lmspilot.certificate.domain.CertificateStatus;
import com.lmspilot.certificate.domain.CertificateTemplateEntity;
import com.lmspilot.certificate.domain.CertificateTemplateRepository;
import com.lmspilot.contracts.CourseCompletedPayload;
import com.lmspilot.contracts.DomainEventEnvelope;
import com.lmspilot.contracts.EventTypes;
import com.lmspilot.contracts.Permissions;
import com.lmspilot.support.api.ApiException;
import com.lmspilot.support.events.DomainEventPublisher;
import com.lmspilot.support.security.CurrentUser;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateService {
    private final CertificateRepository repository;
    private final CertificateTemplateRepository templates;
    private final ObjectMapper mapper;
    private final DomainEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public CertificateService(
        CertificateRepository repository,
        CertificateTemplateRepository templates,
        ObjectMapper mapper,
        DomainEventPublisher events
    ) {
        this.repository = repository;
        this.templates = templates;
        this.mapper = mapper;
        this.events = events;
    }

    @RabbitListener(queues = "certificate.course-completed")
    @Transactional
    public void consume(DomainEventEnvelope event) {
        CourseCompletedPayload payload = mapper.convertValue(event.payload(), CourseCompletedPayload.class);
        boolean alreadyActive = repository.findAllByEnrollmentIdOrderByGenerationDesc(payload.enrollmentId()).stream()
            .anyMatch(certificate -> certificate.getStatus() == CertificateStatus.ACTIVE);
        if (!alreadyActive) issue(payload.enrollmentId(), payload.courseId(), payload.userId(), null);
    }

    @Transactional
    public CertificateResponse issue(UUID enrollmentId, UUID courseId, UUID userId, UUID replacesCertificateId) {
        int generation = repository.findAllByEnrollmentIdOrderByGenerationDesc(enrollmentId).stream()
            .mapToInt(CertificateEntity::getGeneration)
            .max()
            .orElse(0) + 1;
        CertificateTemplateEntity template = templates.findFirstByCourseIdAndActiveTrueOrderByUpdatedAtDesc(courseId);
        if (template == null) template = templates.findFirstByCourseIdIsNullAndActiveTrueOrderByUpdatedAtDesc();
        CertificateTemplateSnapshot snapshot = template == null ? defaults() : snapshot(template);
        String snapshotJson;
        try { snapshotJson = mapper.writeValueAsString(snapshot); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
        CertificateEntity certificate = repository.save(new CertificateEntity(
            enrollmentId,
            courseId,
            userId,
            nextCode(),
            generation,
            replacesCertificateId,
            template == null ? null : template.getId(),
            snapshotJson
        ));
        events.publish(
            EventTypes.CERTIFICATE_ISSUED,
            "certificate-service",
            certificate.getId().toString(),
            new CertificateIssuedEvent(certificate.getId(), enrollmentId, courseId, userId, certificate.getVerificationCode(), certificate.getIssuedAt())
        );
        return response(certificate);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> all() {
        return repository.findAllByOrderByIssuedAtDesc().stream().map(CertificateService::response).toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> mine() {
        return repository.findAllByUserIdOrderByIssuedAtDesc(CurrentUser.id()).stream().map(CertificateService::response).toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse verify(String code) {
        CertificateEntity certificate = repository.findByVerificationCode(code.toUpperCase());
        if (certificate == null) throw notFound();
        return response(certificate);
    }

    @Transactional
    public CertificateResponse revoke(UUID id, RevokeRequest request) {
        CertificateEntity certificate = repository.findById(id).orElseThrow(CertificateService::notFound);
        if (certificate.getStatus() != CertificateStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "CERTIFICATE_NOT_ACTIVE", "Chứng chỉ không còn hiệu lực");
        }
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setRevokedAt(Instant.now());
        certificate.setRevokeReason(request.reason().trim());
        return response(certificate);
    }

    @Transactional
    public CertificateResponse reissue(UUID id, RevokeRequest request) {
        CertificateEntity old = repository.findById(id).orElseThrow(CertificateService::notFound);
        if (old.getStatus() == CertificateStatus.ACTIVE) {
            old.setStatus(CertificateStatus.REISSUED);
            old.setRevokedAt(Instant.now());
            old.setRevokeReason(request.reason().trim());
        }
        return issue(old.getEnrollmentId(), old.getCourseId(), old.getUserId(), old.getId());
    }

    @Transactional(readOnly = true)
    public List<CertificateTemplateResponse> listTemplates() {
        return templates.findAllByOrderByUpdatedAtDesc().stream().map(CertificateService::templateResponse).toList();
    }

    @Transactional
    public CertificateTemplateResponse createTemplate(CertificateTemplateRequest request) {
        validateTemplate(request);
        return templateResponse(templates.save(toEntity(request, CurrentUser.id())));
    }

    @Transactional
    public CertificateTemplateResponse updateTemplate(UUID id, CertificateTemplateRequest request) {
        validateTemplate(request);
        CertificateTemplateEntity template = templates.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu chứng chỉ"));
        template.setName(request.name().trim());
        template.setCourseId(request.courseId());
        template.setTitle(request.title().trim());
        template.setIssuerName(request.issuerName().trim());
        template.setBodyText(request.bodyText().trim());
        template.setPrimaryColor(request.primaryColor().toLowerCase());
        template.setSecondaryColor(request.secondaryColor().toLowerCase());
        template.setLogoUrl(clean(request.logoUrl()));
        template.setSignatureName(clean(request.signatureName()));
        template.setActive(request.active());
        template.setUpdatedAt(Instant.now());
        return templateResponse(template);
    }

    @Transactional
    public void disableTemplate(UUID id) {
        CertificateTemplateEntity template = templates.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu chứng chỉ"));
        template.setActive(false);
        template.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public String printable(UUID id) {
        CertificateEntity certificate = repository.findById(id).orElseThrow(CertificateService::notFound);
        if (!certificate.getUserId().equals(CurrentUser.id()) && !CurrentUser.authorities().contains(Permissions.CERTIFICATES_MANAGE)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CERTIFICATE_SCOPE", "Bạn không có quyền xem chứng chỉ");
        }
        CertificateTemplateSnapshot template;
        try { template = mapper.readValue(certificate.getTemplateSnapshotJson(), CertificateTemplateSnapshot.class); }
        catch (Exception exception) { template = defaults(); }
        String logo = template.logoUrl() == null
            ? ""
            : "<img class=\"logo\" src=\"" + escapeHtml(template.logoUrl()) + "\" alt=\"Logo\">";
        String signature = template.signatureName() == null
            ? ""
            : "<p class=\"signature\">" + escapeHtml(template.signatureName()) + "</p>";
        return "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<title>" + escapeHtml(template.title()) + "</title><style>" +
            "body{font-family:Arial,sans-serif;background:#f4f7fb;padding:40px}" +
            ".certificate{max-width:900px;margin:auto;background:white;border:12px solid " + template.primaryColor() +
            ";box-shadow:inset 0 0 0 3px " + template.secondaryColor() + ";padding:70px;text-align:center}" +
            ".logo{max-height:84px;max-width:220px}.issuer{letter-spacing:.18em;text-transform:uppercase;color:" + template.secondaryColor() + "}" +
            ".title{font-size:44px;color:" + template.primaryColor() + ";margin:24px 0}.body{font-size:20px;line-height:1.7}" +
            ".code{font-family:monospace;font-size:20px}.signature{margin-top:44px;font-weight:700}</style></head>" +
            "<body><main class=\"certificate\">" + logo +
            "<p class=\"issuer\">" + escapeHtml(template.issuerName()) + "</p>" +
            "<div class=\"title\">" + escapeHtml(template.title()) + "</div>" +
            "<p class=\"body\">" + escapeHtml(template.bodyText()) + "</p>" +
            "<p>Xác nhận học viên <b>" + certificate.getUserId() + "</b></p>" +
            "<p>đã hoàn thành khóa học <b>" + certificate.getCourseId() + "</b></p>" +
            "<p>Ngày cấp: " + certificate.getIssuedAt() + "</p>" +
            "<p class=\"code\">Mã xác minh: " + certificate.getVerificationCode() + "</p>" +
            "<p>Trạng thái: " + certificate.getStatus() + "</p>" + signature + "</main></body></html>";
    }

    private void validateTemplate(CertificateTemplateRequest request) {
        if (!request.primaryColor().matches("^#[0-9a-fA-F]{6}$") || !request.secondaryColor().matches("^#[0-9a-fA-F]{6}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_COLOR", "Màu mẫu chứng chỉ phải ở dạng #RRGGBB");
        }
        if (request.name().length() > 180 || request.title().length() > 240 || request.issuerName().length() > 240 || request.bodyText().length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TEMPLATE_TOO_LONG", "Nội dung mẫu chứng chỉ quá dài");
        }
        if (request.logoUrl() != null && !request.logoUrl().startsWith("/api/v1/files/") && !request.logoUrl().startsWith("/public/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_LOGO", "Logo phải là tài nguyên nội bộ");
        }
    }

    private String nextCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder raw = new StringBuilder();
            for (int index = 0; index < 16; index++) raw.append(alphabet.charAt(random.nextInt(alphabet.length())));
            String code = raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12);
            if (repository.findByVerificationCode(code) == null) return code;
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CERTIFICATE_CODE_FAILURE", "Không thể tạo mã chứng chỉ");
    }

    private static String clean(String value) { return value == null || value.trim().isBlank() ? null : value.trim(); }
    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private static CertificateTemplateSnapshot defaults() {
        return new CertificateTemplateSnapshot("CHỨNG CHỈ HOÀN THÀNH", "LMSPilot", "Xác nhận người học đã hoàn thành chương trình đào tạo.", "#173b65", "#b99044", null, null);
    }
    private static CertificateResponse response(CertificateEntity certificate) {
        return new CertificateResponse(certificate.getId(), certificate.getEnrollmentId(), certificate.getCourseId(), certificate.getUserId(), certificate.getVerificationCode(), certificate.getGeneration(), certificate.getStatus(), certificate.getIssuedAt(), certificate.getRevokedAt(), certificate.getRevokeReason());
    }
    private static CertificateTemplateResponse templateResponse(CertificateTemplateEntity template) {
        return new CertificateTemplateResponse(template.getId(), template.getName(), template.getCourseId(), template.getTitle(), template.getIssuerName(), template.getBodyText(), template.getPrimaryColor(), template.getSecondaryColor(), template.getLogoUrl(), template.getSignatureName(), template.isActive(), template.getUpdatedAt());
    }
    private static CertificateTemplateSnapshot snapshot(CertificateTemplateEntity template) {
        return new CertificateTemplateSnapshot(template.getTitle(), template.getIssuerName(), template.getBodyText(), template.getPrimaryColor(), template.getSecondaryColor(), template.getLogoUrl(), template.getSignatureName());
    }
    private static CertificateTemplateEntity toEntity(CertificateTemplateRequest request, UUID actor) {
        return new CertificateTemplateEntity(request.name().trim(), request.courseId(), request.title().trim(), request.issuerName().trim(), request.bodyText().trim(), request.primaryColor().toLowerCase(), request.secondaryColor().toLowerCase(), clean(request.logoUrl()), clean(request.signatureName()), request.active(), actor);
    }
    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "CERTIFICATE_NOT_FOUND", "Không tìm thấy chứng chỉ"); }
}
