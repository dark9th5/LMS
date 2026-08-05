package com.lmspilot.license.api;import jakarta.validation.constraints.NotBlank;public record ActivateLicenseRequest(@NotBlank String payload,@NotBlank String signature){}
