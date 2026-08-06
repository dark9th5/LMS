package com.lmspilot.competency.config;

import com.lmspilot.competency.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class DevelopmentSeed implements ApplicationRunner {
    private final CompetencyRepository competencies;
    private final CompetencyProfileRepository profiles;
    private final CompetencyProfileRequirementRepository requirements;
    private final boolean enabled;

    public DevelopmentSeed(CompetencyRepository c, CompetencyProfileRepository p, CompetencyProfileRequirementRepository r,
                           @Value("${lmspilot.seed-demo:true}") boolean e) {
        competencies = c;
        profiles = p;
        requirements = r;
        enabled = e;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || competencies.count() > 0) return;
        var d = competencies.save(new CompetencyEntity("DIGITAL-LITERACY", "Năng lực số", "Sử dụng công cụ số an toàn và hiệu quả", "Năng lực cốt lõi", 5, CompetencyStatus.ACTIVE));
        var s = competencies.save(new CompetencyEntity("INFOSEC-AWARENESS", "Nhận thức an toàn thông tin", "Nhận diện và xử lý rủi ro an toàn thông tin", "Tuân thủ", 5, CompetencyStatus.ACTIVE));
        var p = profiles.save(new CompetencyProfileEntity("ALL-EMPLOYEES", "Khung năng lực nhân viên", "Mức năng lực tối thiểu dùng cho dữ liệu mẫu", null, null, true));
        requirements.saveAll(List.of(new CompetencyProfileRequirementEntity(p, d, 2, 1), new CompetencyProfileRequirementEntity(p, s, 2, 1)));
    }
}
