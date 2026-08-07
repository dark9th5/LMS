package com.lmspilot.contracts;

import java.util.*;

import java.util.function.Function;

import java.util.stream.Collectors;

/** Human-readable catalog used by administration APIs and the web client. */
public final class PermissionCatalog {
    private static final Set<String> SYSTEM_ONLY = Set.of("SYSTEM");
    private static final Set<String> ORGANIZATION_SCOPES = Set.of("SYSTEM", "BRANCH", "DEPARTMENT", "GROUP");
    private static final Set<String> COURSE_SCOPES = Set.of("SYSTEM", "BRANCH", "DEPARTMENT", "GROUP", "COURSE");
    private static final Set<String> EXAM_SCOPES = Set.of("SYSTEM", "BRANCH", "DEPARTMENT", "GROUP", "COURSE", "EXAM");
    private PermissionCatalog() {
    }
    private static PermissionDefinition item(String code, String group, String label, String description, Set<String> scopes, PermissionRisk risk, boolean legacy) {
        return new PermissionDefinition(code, group, label, description, scopes, risk, legacy);
    }
    public static final List<PermissionDefinition> DEFINITIONS = List.of(
    item(Permissions.USERS_READ, "Tài khoản", "Xem tài khoản", "Xem danh sách và hồ sơ tài khoản.", ORGANIZATION_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.USERS_CREATE, "Tài khoản", "Tạo tài khoản", "Tạo tài khoản USER và cấp thông tin đăng nhập.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.USERS_UPDATE, "Tài khoản", "Sửa tài khoản", "Cập nhật hồ sơ, trạng thái và gói quyền cơ sở.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.USERS_LOCK, "Tài khoản", "Khóa tài khoản", "Khóa hoặc mở khóa tài khoản người dùng.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.USERS_BULK_MANAGE, "Tài khoản", "Quản lý tài khoản hàng loạt", "Nhập và cập nhật nhiều tài khoản qua CSV/XLSX.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.USERS_WRITE, "Tài khoản", "Quản lý tài khoản (tương thích)", "Quyền tương thích cho API tài khoản cũ.", SYSTEM_ONLY, PermissionRisk.HIGH, true),
    item(Permissions.USERS_SESSIONS_MANAGE, "Tài khoản", "Quản lý phiên đăng nhập", "Xem và thu hồi phiên đăng nhập của người dùng.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.USERS_PASSWORD_POLICY_MANAGE, "Tài khoản", "Quản lý chính sách mật khẩu", "Thiết lập quy tắc mật khẩu và khóa đăng nhập.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.ROLES_READ, "Phân quyền", "Xem gói quyền", "Xem role, gói quyền và danh mục permission.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.ROLES_MANAGE, "Phân quyền", "Quản lý gói quyền", "Tạo và chỉnh sửa gói quyền tùy chỉnh.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.AUTHORIZATION_GRANT, "Phân quyền", "Cấp quyền", "Cấp gói quyền hoặc permission theo phạm vi.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.AUTHORIZATION_REVOKE, "Phân quyền", "Thu hồi quyền", "Thu hồi gói quyền hoặc permission đã cấp.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.ORGANIZATION_READ, "Tổ chức", "Xem cơ cấu tổ chức", "Xem chi nhánh, phòng ban, nhóm và thành viên.", ORGANIZATION_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.ORGANIZATION_MANAGE, "Tổ chức", "Quản lý cơ cấu tổ chức", "Tạo, sửa và sắp xếp đơn vị tổ chức.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.ORGANIZATION_MEMBERSHIP_MANAGE, "Tổ chức", "Quản lý thành viên đơn vị", "Gán hoặc gỡ người dùng khỏi đơn vị tổ chức.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.ORGANIZATION_WRITE, "Tổ chức", "Quản lý tổ chức (tương thích)", "Quyền tương thích cho API tổ chức cũ.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, true),
    item(Permissions.COURSES_READ, "Khóa học", "Xem khóa học", "Xem danh mục và nội dung khóa học được phép.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.COURSES_CREATE, "Khóa học", "Tạo khóa học", "Khởi tạo khóa học và nội dung ban đầu.", SYSTEM_ONLY, PermissionRisk.MEDIUM, false),
    item(Permissions.COURSES_UPDATE, "Khóa học", "Biên tập khóa học", "Sửa cấu trúc, bài học và tài nguyên khóa học.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.COURSES_WRITE, "Khóa học", "Quản lý khóa học (tương thích)", "Quyền tương thích cho API khóa học cũ.", COURSE_SCOPES, PermissionRisk.MEDIUM, true),
    item(Permissions.COURSES_PUBLISH, "Khóa học", "Xuất bản khóa học", "Chốt phiên bản và phát hành nội dung cho người học.", COURSE_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.COURSES_ASSIGN, "Khóa học", "Giao khóa học", "Giao khóa học trực tiếp cho người dùng hoặc đơn vị.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.COURSES_LEARN, "Khóa học", "Học khóa học", "Truy cập khóa học đã được ghi danh và lưu tiến độ.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.COURSE_CATEGORIES_MANAGE, "Khóa học", "Quản lý danh mục khóa học", "Tạo và sắp xếp nhóm chủ đề khóa học.", SYSTEM_ONLY, PermissionRisk.MEDIUM, false),
    item(Permissions.DISCUSSIONS_READ, "Khóa học", "Xem thảo luận", "Xem trao đổi trong khóa học.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.DISCUSSIONS_WRITE, "Khóa học", "Tham gia thảo luận", "Tạo chủ đề và trả lời thảo luận.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.DISCUSSIONS_MODERATE, "Khóa học", "Điều phối thảo luận", "Ghim, khóa và xử lý nội dung thảo luận.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.CLASSES_READ, "Tương thích", "Xem phân phối khóa học (cũ)", "Quyền nội bộ tương thích; không có giao diện lớp học.", COURSE_SCOPES, PermissionRisk.LOW, true),
    item(Permissions.CLASSES_WRITE, "Tương thích", "Cập nhật phân phối khóa học (cũ)", "Quyền nội bộ tương thích; không có giao diện lớp học.", COURSE_SCOPES, PermissionRisk.MEDIUM, true),
    item(Permissions.CLASSES_MANAGE, "Tương thích", "Quản lý phân phối khóa học (cũ)", "Quyền nội bộ tương thích; không có giao diện lớp học.", COURSE_SCOPES, PermissionRisk.HIGH, true),
    item(Permissions.ENROLLMENTS_WRITE, "Khóa học", "Quản lý người học", "Giao hoặc thu hồi khóa học và đặt hạn hoàn thành.", COURSE_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.LIVE_SESSIONS_MANAGE, "Khóa học", "Quản lý buổi học trực tiếp", "Tạo lịch trực tuyến trong khóa học.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.LIVE_SESSIONS_JOIN, "Khóa học", "Tham gia buổi học trực tiếp", "Mở liên kết trực tuyến trong khóa học được giao.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.LEARNING_PATHS_READ, "Lộ trình", "Xem lộ trình", "Xem lộ trình đào tạo được phép.", ORGANIZATION_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.LEARNING_PATHS_MANAGE, "Lộ trình", "Quản lý lộ trình", "Tạo và xuất bản lộ trình nhiều chặng.", ORGANIZATION_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.LEARNING_PATHS_ASSIGN, "Lộ trình", "Giao lộ trình", "Giao lộ trình cho cá nhân hoặc đơn vị.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.LEARNING_READ_SELF, "Học tập", "Xem tiến độ cá nhân", "Xem tiến độ học của chính mình.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.LEARNING_WRITE_SELF, "Học tập", "Cập nhật tiến độ cá nhân", "Ghi nhận hoàn thành bài học của chính mình.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.LEARNING_READ_SCOPE, "Học tập", "Xem tiến độ theo phạm vi", "Theo dõi tiến độ người học trong phạm vi phụ trách.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.XAPI_WRITE, "Học tập", "Ghi hoạt động xAPI", "Gửi sự kiện học tập vào LRS.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.XAPI_READ_SCOPE, "Học tập", "Xem hoạt động xAPI", "Đọc sự kiện học tập trong phạm vi phụ trách.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.ASSESSMENTS_READ, "Đánh giá", "Xem bài kiểm tra", "Xem bài kiểm tra và kỳ thi được phép.", EXAM_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.ASSESSMENTS_CREATE, "Đánh giá", "Tạo bài kiểm tra", "Tạo bài kiểm tra trong khóa học hoặc kỳ thi độc lập.", EXAM_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.ASSESSMENTS_UPDATE, "Đánh giá", "Biên tập bài kiểm tra", "Sửa câu hỏi, cấu hình và điều kiện thi.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.ASSESSMENT_MANAGE, "Đánh giá", "Quản lý đánh giá (tương thích)", "Quyền tương thích cho API đánh giá cũ.", EXAM_SCOPES, PermissionRisk.HIGH, true),
    item(Permissions.ASSESSMENT_TAKE, "Đánh giá", "Làm bài (tương thích)", "Quyền tương thích cho luồng làm bài cũ.", EXAM_SCOPES, PermissionRisk.LOW, true),
    item(Permissions.ASSESSMENTS_TAKE, "Đánh giá", "Làm bài kiểm tra", "Bắt đầu, tiếp tục và nộp bài được giao.", EXAM_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.ASSESSMENTS_GRADE, "Đánh giá", "Chấm bài", "Chấm câu tự luận và hoàn tất kết quả.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.GRADING_MANAGE, "Đánh giá", "Quản lý chấm điểm (tương thích)", "Quyền tương thích cho API chấm điểm cũ.", EXAM_SCOPES, PermissionRisk.HIGH, true),
    item(Permissions.GRADES_READ_SELF, "Đánh giá", "Xem điểm cá nhân", "Xem kết quả của chính mình.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.GRADE_APPEALS_CREATE, "Đánh giá", "Gửi phúc khảo", "Tạo yêu cầu phúc khảo kết quả.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.GRADE_APPEALS_MANAGE, "Đánh giá", "Xử lý phúc khảo", "Duyệt và điều chỉnh kết quả phúc khảo.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.EXAMS_MANAGE, "Kỳ thi", "Quản lý kỳ thi độc lập", "Tạo và vận hành kỳ thi không thuộc khóa học.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.EXAMS_ASSIGN, "Kỳ thi", "Giao kỳ thi", "Giao kỳ thi cho người dùng hoặc đơn vị.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.COMPETITIONS_MANAGE, "Cuộc thi", "Quản lý cuộc thi", "Cấu hình cuộc thi, thời gian và bảng xếp hạng.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.COMPETITIONS_PARTICIPATE, "Cuộc thi", "Tham gia cuộc thi", "Tham gia cuộc thi được giao.", EXAM_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.COMPETITIONS_REWARD, "Cuộc thi", "Trao thưởng cuộc thi", "Xác nhận và ghi nhận phần thưởng cho người thắng.", EXAM_SCOPES, PermissionRisk.CRITICAL, false),
    item(Permissions.QUESTIONS_READ, "Ngân hàng câu hỏi", "Xem câu hỏi", "Xem ngân hàng câu hỏi được phép.", EXAM_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.QUESTIONS_MANAGE, "Ngân hàng câu hỏi", "Quản lý câu hỏi", "Tạo, sửa và nhập câu hỏi.", EXAM_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.QUESTIONS_GENERATE_AI, "Ngân hàng câu hỏi", "Sinh câu hỏi bằng AI", "Tạo bản nháp câu hỏi từ tài liệu bằng model local hoặc API.", EXAM_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.QUESTIONS_APPROVE_AI, "Ngân hàng câu hỏi", "Duyệt câu hỏi AI", "Kiểm duyệt và nhập câu hỏi AI vào ngân hàng chính thức.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.REPORTS_READ_SELF, "Báo cáo", "Xem báo cáo cá nhân", "Xem báo cáo học tập của chính mình.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.REPORTS_READ_SCOPE, "Báo cáo", "Xem báo cáo theo phạm vi", "Xem báo cáo trong đơn vị, khóa học hoặc kỳ thi phụ trách.", EXAM_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.REPORTS_READ, "Báo cáo", "Xem báo cáo (tương thích)", "Quyền tương thích cho API báo cáo cũ.", EXAM_SCOPES, PermissionRisk.MEDIUM, true),
    item(Permissions.REPORTS_EXPORT, "Báo cáo", "Xuất báo cáo", "Xuất dữ liệu báo cáo ra tệp.", EXAM_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.REPORTS_SCHEDULE, "Báo cáo", "Lập lịch báo cáo", "Tạo lịch gửi hoặc xuất báo cáo tự động.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.REPORTS_KPI_READ, "Báo cáo", "Xem KPI", "Xem chỉ số tổng hợp đào tạo.", ORGANIZATION_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.FILES_READ, "Tài liệu", "Xem tài liệu", "Xem metadata tài liệu được phép.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.FILES_UPLOAD, "Tài liệu", "Tải tài liệu lên", "Tải DOCX, PDF và tài nguyên học tập lên hệ thống.", COURSE_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.FILES_DOWNLOAD, "Tài liệu", "Tải tài liệu xuống", "Tải nội dung tài liệu được phép.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.FILES_EDIT, "Tài liệu", "Chỉnh sửa tài liệu", "Mở phiên sửa DOCX hoặc tạo bản hiệu chỉnh PDF.", COURSE_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.FILES_PUBLISH, "Tài liệu", "Phát hành tài liệu", "Đưa phiên bản tài liệu đã duyệt vào sử dụng.", COURSE_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.FILES_VERSION_READ, "Tài liệu", "Xem lịch sử phiên bản", "Xem các phiên bản và thay đổi của tài liệu.", COURSE_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.NEWS_READ, "Tin tức", "Xem tin tức", "Xem thông báo tập thể được phép.", ORGANIZATION_SCOPES, PermissionRisk.LOW, false),
    item(Permissions.NEWS_MANAGE, "Tin tức", "Biên tập tin tức", "Tạo, sửa và quản lý bản nháp tin tức.", ORGANIZATION_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.NEWS_PUBLISH, "Tin tức", "Xuất bản tin tức", "Phát hành thông báo tới đối tượng đã chọn.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.NOTIFICATION_TEMPLATES_MANAGE, "Thông báo", "Quản lý mẫu thông báo", "Tạo và sửa mẫu email/thông báo trong hệ thống.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.NOTIFICATION_REMINDERS_MANAGE, "Thông báo", "Quản lý nhắc hạn", "Thiết lập quy tắc nhắc hạn học và thi.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.BRANDING_MANAGE, "Cấu hình", "Cá nhân hóa thương hiệu", "Đổi tên, logo, giới thiệu, màu sắc và giao diện.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.CONFIGURATION_MANAGE, "Cấu hình", "Quản lý cấu hình", "Thay đổi cấu hình nghiệp vụ cấp hệ thống.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.INTEGRATIONS_MANAGE, "Cấu hình", "Quản lý tích hợp", "Cấu hình Redis và dịch vụ bên thứ ba.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.AI_USE, "Cấu hình", "Sử dụng AI (tương thích)", "Quyền tương thích cho AI service cũ.", SYSTEM_ONLY, PermissionRisk.MEDIUM, true),
    item(Permissions.CERTIFICATES_MANAGE, "Chứng chỉ", "Quản lý chứng chỉ", "Cấp, thu hồi và cấp lại chứng chỉ.", COURSE_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.CERTIFICATES_READ_SELF, "Chứng chỉ", "Xem chứng chỉ cá nhân", "Xem chứng chỉ của chính mình.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.CERTIFICATE_TEMPLATES_MANAGE, "Chứng chỉ", "Quản lý mẫu chứng chỉ", "Tạo và phát hành mẫu chứng chỉ.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.COMPETENCIES_READ_SELF, "Năng lực", "Xem năng lực cá nhân", "Xem hồ sơ năng lực của chính mình.", SYSTEM_ONLY, PermissionRisk.LOW, false),
    item(Permissions.COMPETENCIES_READ_SCOPE, "Năng lực", "Xem năng lực theo phạm vi", "Xem hồ sơ năng lực trong phạm vi phụ trách.", ORGANIZATION_SCOPES, PermissionRisk.MEDIUM, false),
    item(Permissions.COMPETENCIES_MANAGE, "Năng lực", "Quản lý khung năng lực", "Tạo khung, nhóm và tiêu chí năng lực.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.COMPETENCIES_ASSESS, "Năng lực", "Đánh giá năng lực", "Ghi nhận mức năng lực cho người dùng.", ORGANIZATION_SCOPES, PermissionRisk.HIGH, false),
    item(Permissions.AUDIT_READ, "Vận hành", "Xem nhật ký kiểm toán", "Xem lịch sử thao tác nhạy cảm.", SYSTEM_ONLY, PermissionRisk.HIGH, false),
    item(Permissions.AUDIT_EXPORT, "Vận hành", "Xuất nhật ký kiểm toán", "Xuất dữ liệu audit phục vụ kiểm tra.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.OPERATIONS_MANAGE, "Vận hành", "Quản lý vận hành", "Thực hiện tác vụ vận hành được cho phép.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false),
    item(Permissions.LICENSE_MANAGE, "Vận hành", "Quản lý giấy phép", "Nạp và kiểm tra license hệ thống.", SYSTEM_ONLY, PermissionRisk.CRITICAL, false)
    );
    private static final Map<String, PermissionDefinition> BY_CODE = DEFINITIONS.stream()
    .collect(Collectors.toUnmodifiableMap(PermissionDefinition::code, Function.identity()));
    public static List<PermissionDefinition> all() {
        return DEFINITIONS.stream().sorted(Comparator.comparing(PermissionDefinition::group).thenComparing(PermissionDefinition::label)).toList();
    }
    public static PermissionDefinition find(String code) {
        return BY_CODE.get(code);
    }
    public static Set<String> codes() {
        return BY_CODE.keySet();
    }

}
