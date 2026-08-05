package com.lmspilot.certificate.api;import jakarta.validation.constraints.NotBlank;public record RevokeRequest(@NotBlank String reason){}
