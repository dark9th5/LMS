package com.lmspilot.notification.config;

import com.lmspilot.notification.domain.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class DevelopmentSeed {
    @Bean
    CommandLineRunner seedNotification(NewsArticleRepository news, NotificationRepository notifications,
                                       @Value("${lmspilot.seed-demo:true}") boolean enabled) {
        return args -> {
            if (!enabled) return;

            UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000003");

            if (news.count() == 0) {
                NewsArticleEntity n1 = new NewsArticleEntity();
                n1.title = "Chào mừng bạn đến với Hệ thống LMSPilot 0.21.0";
                n1.summary = "Hệ thống nâng cấp toàn diện với hiệu năng vượt trội";
                n1.content = "<p>Chào mừng bạn đến với phiên bản LMSPilot 0.21.0 chạy trên nền tảng Java 21 LTS và Spring Boot 3.5!</p>";
                n1.status = NewsStatus.PUBLISHED;
                n1.audienceType = NewsAudienceType.SYSTEM;
                n1.pinned = true;
                n1.priority = 10;
                n1.createdBy = adminId;
                news.save(n1);
            }

            if (notifications.count() == 0) {
                NotificationEntity notif = new NotificationEntity();
                notif.userId = studentId;
                notif.title = "Khóa học mới đã được giao";
                notif.body = "Bạn đã được phân công tham gia khóa học: Bắt đầu với LMSPilot 0.21.0";
                notif.type = "SYSTEM";
                notif.read = false;
                notif.createdAt = Instant.now();
                notifications.save(notif);
            }
        };
    }
}
