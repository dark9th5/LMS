package com.lmspilot.certificate.api;

import java.time.Instant;

import java.util.UUID;
public record CertificateTemplateResponse(UUID id,String name,UUID courseId,String title,String issuerName,String bodyText,String primaryColor,String secondaryColor,String logoUrl,String signatureName,boolean active,Instant updatedAt){
}
