package com.lmspilot.organization.config;

import com.lmspilot.organization.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DevelopmentSeed implements ApplicationRunner {
    private final OrganizationUnitRepository repository;
    private final boolean enabled;

    public DevelopmentSeed(OrganizationUnitRepository r, @Value("${lmspilot.seed-demo:true}") boolean e) {
        repository = r;
        enabled = e;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || repository.count() > 0) return;
        var root = repository.save(new OrganizationUnitEntity("ORG", "Tổ chức chính", OrganizationUnitType.ORGANIZATION, null, OrganizationUnitStatus.ACTIVE, 0, "/"));
        var tech = repository.save(new OrganizationUnitEntity("TECH", "Khối Công nghệ & Phần mềm", OrganizationUnitType.DIVISION, root.getId(), OrganizationUnitStatus.ACTIVE, 0, "/" + root.getId() + "/"));
        repository.save(new OrganizationUnitEntity("DEV", "Phòng Phát triển Hệ thống", OrganizationUnitType.DEPARTMENT, tech.getId(), OrganizationUnitStatus.ACTIVE, 0, "/" + root.getId() + "/" + tech.getId() + "/"));
        repository.save(new OrganizationUnitEntity("TRAIN", "Trung tâm Đào tạo & Khảo thí", OrganizationUnitType.DEPARTMENT, root.getId(), OrganizationUnitStatus.ACTIVE, 0, "/" + root.getId() + "/"));
    }
}
