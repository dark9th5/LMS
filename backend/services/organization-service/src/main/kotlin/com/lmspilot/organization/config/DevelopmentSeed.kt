package com.lmspilot.organization.config

import com.lmspilot.organization.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DevelopmentSeed(
    private val repository: OrganizationUnitRepository,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || repository.count() > 0) return
        val root = repository.save(OrganizationUnitEntity(code = "ORG", name = "Tổ chức mẫu", type = OrganizationUnitType.ORGANIZATION, materializedPath = "/"))
        val tech = repository.save(OrganizationUnitEntity(code = "TECH", name = "Khối Công nghệ", type = OrganizationUnitType.DIVISION, parentId = root.id, materializedPath = "/${root.id}/"))
        repository.save(OrganizationUnitEntity(code = "DEV", name = "Phòng Phát triển", type = OrganizationUnitType.DEPARTMENT, parentId = tech.id, materializedPath = "/${root.id}/${tech.id}/"))
        repository.save(OrganizationUnitEntity(code = "TRAIN", name = "Trung tâm Đào tạo", type = OrganizationUnitType.DEPARTMENT, parentId = root.id, materializedPath = "/${root.id}/"))
    }
}
