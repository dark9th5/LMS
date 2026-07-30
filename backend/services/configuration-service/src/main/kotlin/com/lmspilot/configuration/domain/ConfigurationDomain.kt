package com.lmspilot.configuration.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_configuration")
class ProductConfigurationEntity(
    @Id var id: UUID = UUID(0, 1),
    @Column(nullable = false, length = 160) var productName: String = "LMSPilot",
    @Column(nullable = false, length = 500) var logoUrl: String = "",
    @Column(nullable = false, length = 20) var primaryColor: String = "#1457D9",
    @Column(nullable = false, length = 20) var accentColor: String = "#15A37B",
    @Column(nullable = false, length = 20) var defaultLocale: String = "vi",
    @Column(nullable = false, columnDefinition = "text") var featureFlagsJson: String = "{}",
    @Column(nullable = false, columnDefinition = "text") var terminologyJson: String = "{}",
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
    var updatedBy: UUID? = null,
    @Version var rowVersion: Long = 0,
)
interface ProductConfigurationRepository : org.springframework.data.jpa.repository.JpaRepository<ProductConfigurationEntity, UUID>
