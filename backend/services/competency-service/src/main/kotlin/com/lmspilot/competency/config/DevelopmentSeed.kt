package com.lmspilot.competency.config

import com.lmspilot.competency.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DevelopmentSeed(
    private val competencies: CompetencyRepository,
    private val profiles: CompetencyProfileRepository,
    private val requirements: CompetencyProfileRequirementRepository,
    @Value("\${lmspilot.seed-demo:false}") private val enabled: Boolean,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!enabled || competencies.count() > 0) return
        val digital = competencies.save(CompetencyEntity(code = "DIGITAL-LITERACY", name = "Năng lực số", category = "Năng lực cốt lõi", description = "Sử dụng công cụ số an toàn và hiệu quả"))
        val security = competencies.save(CompetencyEntity(code = "INFOSEC-AWARENESS", name = "Nhận thức an toàn thông tin", category = "Tuân thủ", description = "Nhận diện và xử lý rủi ro an toàn thông tin"))
        val profile = profiles.save(CompetencyProfileEntity(code = "ALL-EMPLOYEES", name = "Khung năng lực nhân viên", description = "Mức năng lực tối thiểu dùng cho dữ liệu mẫu"))
        requirements.saveAll(listOf(CompetencyProfileRequirementEntity(profile = profile, competency = digital, requiredLevel = 2), CompetencyProfileRequirementEntity(profile = profile, competency = security, requiredLevel = 2)))
    }
}
