package com.lmspilot.configuration.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.lmspilot.configuration.domain.*
import com.lmspilot.contracts.Permissions
import com.lmspilot.support.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

data class ProductConfigurationRequest(@field:NotBlank val productName: String, val logoUrl: String = "", @field:Pattern(regexp="^#[0-9A-Fa-f]{6}$") val primaryColor: String, @field:Pattern(regexp="^#[0-9A-Fa-f]{6}$") val accentColor: String, val defaultLocale: String = "vi", val featureFlags: Map<String, Boolean> = emptyMap(), val terminology: Map<String, String> = emptyMap())
data class ProductConfigurationResponse(val productName: String, val logoUrl: String, val primaryColor: String, val accentColor: String, val defaultLocale: String, val featureFlags: Map<String, Boolean>, val terminology: Map<String, String>, val updatedAt: Instant)

@Service
class ProductConfigurationService(private val repository: ProductConfigurationRepository, private val mapper: ObjectMapper) {
    private val singleton = UUID(0, 1)
    @Transactional(readOnly = true) fun get(): ProductConfigurationResponse = (repository.findById(singleton).orElse(ProductConfigurationEntity())).response(mapper)
    @Transactional fun update(input: ProductConfigurationRequest): ProductConfigurationResponse {
        val entity = repository.findById(singleton).orElse(ProductConfigurationEntity())
        entity.productName = input.productName.trim(); entity.logoUrl = input.logoUrl.trim(); entity.primaryColor = input.primaryColor; entity.accentColor = input.accentColor; entity.defaultLocale = input.defaultLocale
        entity.featureFlagsJson = mapper.writeValueAsString(input.featureFlags); entity.terminologyJson = mapper.writeValueAsString(input.terminology); entity.updatedAt = Instant.now(); entity.updatedBy = CurrentUser.id()
        return repository.save(entity).response(mapper)
    }
}
private fun ProductConfigurationEntity.response(mapper: ObjectMapper) = ProductConfigurationResponse(productName, logoUrl, primaryColor, accentColor, defaultLocale, mapper.readValue(featureFlagsJson, object: TypeReference<Map<String,Boolean>>(){}), mapper.readValue(terminologyJson, object: TypeReference<Map<String,String>>(){}), updatedAt)

@RestController
class ProductConfigurationController(private val service: ProductConfigurationService) {
    @GetMapping("/public/v1/configuration") fun publicConfig() = service.get()
    @GetMapping("/api/v1/configuration") @PreAuthorize("hasAuthority('${Permissions.CONFIGURATION_MANAGE}')") fun get() = service.get()
    @PutMapping("/api/v1/configuration") @PreAuthorize("hasAuthority('${Permissions.CONFIGURATION_MANAGE}')") fun update(@Valid @RequestBody input: ProductConfigurationRequest) = service.update(input)
}
