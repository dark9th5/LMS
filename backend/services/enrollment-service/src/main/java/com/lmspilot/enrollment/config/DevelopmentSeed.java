package com.lmspilot.enrollment.config;

import com.lmspilot.enrollment.domain.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class DevelopmentSeed {
    @Bean
    CommandLineRunner seedEnrollments(CourseAssignmentRepository assignments, EnrollmentRepository enrollments,
                                     @Value("${lmspilot.seed-demo:true}") boolean enabled) {
        return args -> {
            if (!enabled) return;

            UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000003");
            UUID courseId1 = UUID.fromString("00000000-0000-0000-0002-000000000001");
            UUID courseId2 = UUID.fromString("00000000-0000-0000-0002-000000000002");
            UUID classId = UUID.fromString("00000000-0000-0000-0000-000000000100");

            if (assignments.count() == 0) {
                CourseAssignmentEntity a1 = new CourseAssignmentEntity();
                a1.courseId = courseId1;
                a1.classId = classId;
                a1.assigneeType = AssignmentTargetType.USER;
                a1.assigneeId = studentId;
                a1.required = true;
                a1.assignedBy = UUID.fromString("00000000-0000-0000-0000-000000000001");
                a1.assignedAt = Instant.now();
                assignments.save(a1);

                CourseAssignmentEntity a2 = new CourseAssignmentEntity();
                a2.courseId = courseId2;
                a2.classId = classId;
                a2.assigneeType = AssignmentTargetType.USER;
                a2.assigneeId = studentId;
                a2.required = false;
                a2.assignedBy = UUID.fromString("00000000-0000-0000-0000-000000000001");
                a2.assignedAt = Instant.now();
                assignments.save(a2);
            }

            if (enrollments.count() == 0) {
                EnrollmentEntity e1 = new EnrollmentEntity();
                e1.courseId = courseId1;
                e1.classId = classId;
                e1.userId = studentId;
                e1.status = EnrollmentStatus.ENROLLED;
                e1.enrolledAt = Instant.now();
                enrollments.save(e1);

                EnrollmentEntity e2 = new EnrollmentEntity();
                e2.courseId = courseId2;
                e2.classId = classId;
                e2.userId = studentId;
                e2.status = EnrollmentStatus.ENROLLED;
                e2.enrolledAt = Instant.now();
                enrollments.save(e2);
            }
        };
    }
}
