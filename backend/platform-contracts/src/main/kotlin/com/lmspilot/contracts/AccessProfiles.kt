package com.lmspilot.contracts

/**
 * Human-readable permission catalog shared by backend and web clients.
 * Product access is defined by exactly one canonical role per account:
 * ADMIN, INSTRUCTOR or STUDENT. Permissions never cross those role boundaries.
 */
enum class PermissionRisk { LOW, MEDIUM, HIGH, CRITICAL }

data class PermissionDefinition(
    val code: String,
    val group: String,
    val label: String,
    val description: String,
    val allowedScopes: Set<String>,
    val risk: PermissionRisk = PermissionRisk.LOW,
    val legacy: Boolean = false,
)

data class AccessProfileDefinition(
    val code: String,
    val name: String,
    val description: String,
    val permissions: Set<String>,
    val recommendedScopes: Set<String>,
    val risk: PermissionRisk = PermissionRisk.LOW,
)

object PermissionCatalog {
    private val systemOnly = setOf("SYSTEM")
    private val organizationScopes = setOf("SYSTEM", "BRANCH", "DEPARTMENT", "GROUP")
    private val courseScopes = setOf("SYSTEM", "BRANCH", "DEPARTMENT", "GROUP", "COURSE")
    private val examScopes = setOf("SYSTEM", "BRANCH", "DEPARTMENT", "GROUP", "COURSE", "EXAM")

    private fun item(
        code: String,
        group: String,
        label: String,
        description: String,
        scopes: Set<String> = systemOnly,
        risk: PermissionRisk = PermissionRisk.LOW,
        legacy: Boolean = false,
    ) = PermissionDefinition(code, group, label, description, scopes, risk, legacy)

    val definitions: List<PermissionDefinition> = listOf(
        item(Permissions.USERS_READ, "Tài khoản", "Xem tài khoản", "Xem danh sách và hồ sơ tài khoản.", organizationScopes),
        item(Permissions.USERS_CREATE, "Tài khoản", "Tạo tài khoản", "Tạo tài khoản USER và cấp thông tin đăng nhập.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.USERS_UPDATE, "Tài khoản", "Sửa tài khoản", "Cập nhật hồ sơ, trạng thái và gói quyền cơ sở.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.USERS_LOCK, "Tài khoản", "Khóa tài khoản", "Khóa hoặc mở khóa tài khoản người dùng.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.USERS_BULK_MANAGE, "Tài khoản", "Quản lý tài khoản hàng loạt", "Nhập và cập nhật nhiều tài khoản qua CSV/XLSX.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.USERS_WRITE, "Tài khoản", "Quản lý tài khoản (tương thích)", "Quyền tương thích cho API tài khoản cũ.", systemOnly, PermissionRisk.HIGH, true),
        item(Permissions.USERS_SESSIONS_MANAGE, "Tài khoản", "Quản lý phiên đăng nhập", "Xem và thu hồi phiên đăng nhập của người dùng.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.USERS_PASSWORD_POLICY_MANAGE, "Tài khoản", "Quản lý chính sách mật khẩu", "Thiết lập quy tắc mật khẩu và khóa đăng nhập.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.ROLES_READ, "Phân quyền", "Xem gói quyền", "Xem role, gói quyền và danh mục permission.", systemOnly),
        item(Permissions.ROLES_MANAGE, "Phân quyền", "Quản lý gói quyền", "Tạo và chỉnh sửa gói quyền tùy chỉnh.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.AUTHORIZATION_GRANT, "Phân quyền", "Cấp quyền", "Cấp gói quyền hoặc permission theo phạm vi.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.AUTHORIZATION_REVOKE, "Phân quyền", "Thu hồi quyền", "Thu hồi gói quyền hoặc permission đã cấp.", systemOnly, PermissionRisk.CRITICAL),

        item(Permissions.ORGANIZATION_READ, "Tổ chức", "Xem cơ cấu tổ chức", "Xem chi nhánh, phòng ban, nhóm và thành viên.", organizationScopes),
        item(Permissions.ORGANIZATION_MANAGE, "Tổ chức", "Quản lý cơ cấu tổ chức", "Tạo, sửa và sắp xếp đơn vị tổ chức.", organizationScopes, PermissionRisk.HIGH),
        item(Permissions.ORGANIZATION_MEMBERSHIP_MANAGE, "Tổ chức", "Quản lý thành viên đơn vị", "Gán hoặc gỡ người dùng khỏi đơn vị tổ chức.", organizationScopes, PermissionRisk.HIGH),
        item(Permissions.ORGANIZATION_WRITE, "Tổ chức", "Quản lý tổ chức (tương thích)", "Quyền tương thích cho API tổ chức cũ.", organizationScopes, PermissionRisk.HIGH, true),

        item(Permissions.COURSES_READ, "Khóa học", "Xem khóa học", "Xem danh mục và nội dung khóa học được phép.", courseScopes),
        item(Permissions.COURSES_CREATE, "Khóa học", "Tạo khóa học", "Khởi tạo khóa học và nội dung ban đầu.", systemOnly, PermissionRisk.MEDIUM),
        item(Permissions.COURSES_UPDATE, "Khóa học", "Biên tập khóa học", "Sửa cấu trúc, bài học và tài nguyên khóa học.", courseScopes, PermissionRisk.MEDIUM),
        item(Permissions.COURSES_WRITE, "Khóa học", "Quản lý khóa học (tương thích)", "Quyền tương thích cho API khóa học cũ.", courseScopes, PermissionRisk.MEDIUM, true),
        item(Permissions.COURSES_PUBLISH, "Khóa học", "Xuất bản khóa học", "Chốt phiên bản và phát hành nội dung cho người học.", courseScopes, PermissionRisk.HIGH),
        item(Permissions.COURSES_ASSIGN, "Khóa học", "Giao khóa học", "Giao khóa học trực tiếp cho người dùng hoặc đơn vị.", courseScopes, PermissionRisk.MEDIUM),
        item(Permissions.COURSES_LEARN, "Khóa học", "Học khóa học", "Truy cập khóa học đã được ghi danh và lưu tiến độ.", courseScopes),
        item(Permissions.COURSE_CATEGORIES_MANAGE, "Khóa học", "Quản lý danh mục khóa học", "Tạo và sắp xếp nhóm chủ đề khóa học.", systemOnly, PermissionRisk.MEDIUM),
        item(Permissions.DISCUSSIONS_READ, "Khóa học", "Xem thảo luận", "Xem trao đổi trong khóa học.", courseScopes),
        item(Permissions.DISCUSSIONS_WRITE, "Khóa học", "Tham gia thảo luận", "Tạo chủ đề và trả lời thảo luận.", courseScopes),
        item(Permissions.DISCUSSIONS_MODERATE, "Khóa học", "Điều phối thảo luận", "Ghim, khóa và xử lý nội dung thảo luận.", courseScopes, PermissionRisk.MEDIUM),

        item(Permissions.CLASSES_READ, "Tương thích", "Xem phân phối khóa học (cũ)", "Quyền nội bộ tương thích; không có giao diện lớp học.", courseScopes, legacy = true),
        item(Permissions.CLASSES_WRITE, "Tương thích", "Cập nhật phân phối khóa học (cũ)", "Quyền nội bộ tương thích; không có giao diện lớp học.", courseScopes, PermissionRisk.MEDIUM, true),
        item(Permissions.CLASSES_MANAGE, "Tương thích", "Quản lý phân phối khóa học (cũ)", "Quyền nội bộ tương thích; không có giao diện lớp học.", courseScopes, PermissionRisk.HIGH, true),
        item(Permissions.ENROLLMENTS_WRITE, "Khóa học", "Quản lý người học", "Giao hoặc thu hồi khóa học và đặt hạn hoàn thành.", courseScopes, PermissionRisk.HIGH),
        item(Permissions.LIVE_SESSIONS_MANAGE, "Khóa học", "Quản lý buổi học trực tiếp", "Tạo lịch trực tuyến trong khóa học.", courseScopes, PermissionRisk.MEDIUM),
        item(Permissions.LIVE_SESSIONS_JOIN, "Khóa học", "Tham gia buổi học trực tiếp", "Mở liên kết trực tuyến trong khóa học được giao.", courseScopes),
        item(Permissions.LEARNING_PATHS_READ, "Lộ trình", "Xem lộ trình", "Xem lộ trình đào tạo được phép.", organizationScopes),
        item(Permissions.LEARNING_PATHS_MANAGE, "Lộ trình", "Quản lý lộ trình", "Tạo và xuất bản lộ trình nhiều chặng.", organizationScopes, PermissionRisk.MEDIUM),
        item(Permissions.LEARNING_PATHS_ASSIGN, "Lộ trình", "Giao lộ trình", "Giao lộ trình cho cá nhân hoặc đơn vị.", organizationScopes, PermissionRisk.HIGH),

        item(Permissions.LEARNING_READ_SELF, "Học tập", "Xem tiến độ cá nhân", "Xem tiến độ học của chính mình.", systemOnly),
        item(Permissions.LEARNING_WRITE_SELF, "Học tập", "Cập nhật tiến độ cá nhân", "Ghi nhận hoàn thành bài học của chính mình.", systemOnly),
        item(Permissions.LEARNING_READ_SCOPE, "Học tập", "Xem tiến độ theo phạm vi", "Theo dõi tiến độ người học trong phạm vi phụ trách.", courseScopes, PermissionRisk.MEDIUM),
        item(Permissions.XAPI_WRITE, "Học tập", "Ghi hoạt động xAPI", "Gửi sự kiện học tập vào LRS.", courseScopes),
        item(Permissions.XAPI_READ_SCOPE, "Học tập", "Xem hoạt động xAPI", "Đọc sự kiện học tập trong phạm vi phụ trách.", courseScopes, PermissionRisk.MEDIUM),

        item(Permissions.ASSESSMENTS_READ, "Đánh giá", "Xem bài kiểm tra", "Xem bài kiểm tra và kỳ thi được phép.", examScopes),
        item(Permissions.ASSESSMENTS_CREATE, "Đánh giá", "Tạo bài kiểm tra", "Tạo bài kiểm tra trong khóa học hoặc kỳ thi độc lập.", examScopes, PermissionRisk.MEDIUM),
        item(Permissions.ASSESSMENTS_UPDATE, "Đánh giá", "Biên tập bài kiểm tra", "Sửa câu hỏi, cấu hình và điều kiện thi.", examScopes, PermissionRisk.HIGH),
        item(Permissions.ASSESSMENT_MANAGE, "Đánh giá", "Quản lý đánh giá (tương thích)", "Quyền tương thích cho API đánh giá cũ.", examScopes, PermissionRisk.HIGH, true),
        item(Permissions.ASSESSMENT_TAKE, "Đánh giá", "Làm bài (tương thích)", "Quyền tương thích cho luồng làm bài cũ.", examScopes, legacy = true),
        item(Permissions.ASSESSMENTS_TAKE, "Đánh giá", "Làm bài kiểm tra", "Bắt đầu, tiếp tục và nộp bài được giao.", examScopes),
        item(Permissions.ASSESSMENTS_GRADE, "Đánh giá", "Chấm bài", "Chấm câu tự luận và hoàn tất kết quả.", examScopes, PermissionRisk.HIGH),
        item(Permissions.GRADING_MANAGE, "Đánh giá", "Quản lý chấm điểm (tương thích)", "Quyền tương thích cho API chấm điểm cũ.", examScopes, PermissionRisk.HIGH, true),
        item(Permissions.GRADES_READ_SELF, "Đánh giá", "Xem điểm cá nhân", "Xem kết quả của chính mình.", systemOnly),
        item(Permissions.GRADE_APPEALS_CREATE, "Đánh giá", "Gửi phúc khảo", "Tạo yêu cầu phúc khảo kết quả.", systemOnly),
        item(Permissions.GRADE_APPEALS_MANAGE, "Đánh giá", "Xử lý phúc khảo", "Duyệt và điều chỉnh kết quả phúc khảo.", examScopes, PermissionRisk.HIGH),
        item(Permissions.EXAMS_MANAGE, "Kỳ thi", "Quản lý kỳ thi độc lập", "Tạo và vận hành kỳ thi không thuộc khóa học.", examScopes, PermissionRisk.HIGH),
        item(Permissions.EXAMS_ASSIGN, "Kỳ thi", "Giao kỳ thi", "Giao kỳ thi cho người dùng hoặc đơn vị.", examScopes, PermissionRisk.HIGH),
        item(Permissions.COMPETITIONS_MANAGE, "Cuộc thi", "Quản lý cuộc thi", "Cấu hình cuộc thi, thời gian và bảng xếp hạng.", examScopes, PermissionRisk.HIGH),
        item(Permissions.COMPETITIONS_PARTICIPATE, "Cuộc thi", "Tham gia cuộc thi", "Tham gia cuộc thi được giao.", examScopes),
        item(Permissions.COMPETITIONS_REWARD, "Cuộc thi", "Trao thưởng cuộc thi", "Xác nhận và ghi nhận phần thưởng cho người thắng.", examScopes, PermissionRisk.CRITICAL),
        item(Permissions.QUESTIONS_READ, "Ngân hàng câu hỏi", "Xem câu hỏi", "Xem ngân hàng câu hỏi được phép.", examScopes),
        item(Permissions.QUESTIONS_MANAGE, "Ngân hàng câu hỏi", "Quản lý câu hỏi", "Tạo, sửa và nhập câu hỏi.", examScopes, PermissionRisk.MEDIUM),
        item(Permissions.QUESTIONS_GENERATE_AI, "Ngân hàng câu hỏi", "Sinh câu hỏi bằng AI", "Tạo bản nháp câu hỏi từ tài liệu bằng model local hoặc API.", examScopes, PermissionRisk.MEDIUM),
        item(Permissions.QUESTIONS_APPROVE_AI, "Ngân hàng câu hỏi", "Duyệt câu hỏi AI", "Kiểm duyệt và nhập câu hỏi AI vào ngân hàng chính thức.", examScopes, PermissionRisk.HIGH),

        item(Permissions.REPORTS_READ_SELF, "Báo cáo", "Xem báo cáo cá nhân", "Xem báo cáo học tập của chính mình.", systemOnly),
        item(Permissions.REPORTS_READ_SCOPE, "Báo cáo", "Xem báo cáo theo phạm vi", "Xem báo cáo trong đơn vị, khóa học hoặc kỳ thi phụ trách.", examScopes, PermissionRisk.MEDIUM),
        item(Permissions.REPORTS_READ, "Báo cáo", "Xem báo cáo (tương thích)", "Quyền tương thích cho API báo cáo cũ.", examScopes, PermissionRisk.MEDIUM, true),
        item(Permissions.REPORTS_EXPORT, "Báo cáo", "Xuất báo cáo", "Xuất dữ liệu báo cáo ra tệp.", examScopes, PermissionRisk.HIGH),
        item(Permissions.REPORTS_SCHEDULE, "Báo cáo", "Lập lịch báo cáo", "Tạo lịch gửi hoặc xuất báo cáo tự động.", organizationScopes, PermissionRisk.HIGH),
        item(Permissions.REPORTS_KPI_READ, "Báo cáo", "Xem KPI", "Xem chỉ số tổng hợp đào tạo.", organizationScopes, PermissionRisk.MEDIUM),

        item(Permissions.FILES_READ, "Tài liệu", "Xem tài liệu", "Xem metadata tài liệu được phép.", courseScopes),
        item(Permissions.FILES_UPLOAD, "Tài liệu", "Tải tài liệu lên", "Tải DOCX, PDF và tài nguyên học tập lên hệ thống.", courseScopes, PermissionRisk.MEDIUM),
        item(Permissions.FILES_DOWNLOAD, "Tài liệu", "Tải tài liệu xuống", "Tải nội dung tài liệu được phép.", courseScopes),
        item(Permissions.FILES_EDIT, "Tài liệu", "Chỉnh sửa tài liệu", "Mở phiên sửa DOCX hoặc tạo bản hiệu chỉnh PDF.", courseScopes, PermissionRisk.HIGH),
        item(Permissions.FILES_PUBLISH, "Tài liệu", "Phát hành tài liệu", "Đưa phiên bản tài liệu đã duyệt vào sử dụng.", courseScopes, PermissionRisk.HIGH),
        item(Permissions.FILES_VERSION_READ, "Tài liệu", "Xem lịch sử phiên bản", "Xem các phiên bản và thay đổi của tài liệu.", courseScopes),

        item(Permissions.NEWS_READ, "Tin tức", "Xem tin tức", "Xem thông báo tập thể được phép.", organizationScopes),
        item(Permissions.NEWS_MANAGE, "Tin tức", "Biên tập tin tức", "Tạo, sửa và quản lý bản nháp tin tức.", organizationScopes, PermissionRisk.MEDIUM),
        item(Permissions.NEWS_PUBLISH, "Tin tức", "Xuất bản tin tức", "Phát hành thông báo tới đối tượng đã chọn.", organizationScopes, PermissionRisk.HIGH),
        item(Permissions.NOTIFICATION_TEMPLATES_MANAGE, "Thông báo", "Quản lý mẫu thông báo", "Tạo và sửa mẫu email/thông báo trong hệ thống.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.NOTIFICATION_REMINDERS_MANAGE, "Thông báo", "Quản lý nhắc hạn", "Thiết lập quy tắc nhắc hạn học và thi.", organizationScopes, PermissionRisk.HIGH),

        item(Permissions.BRANDING_MANAGE, "Cấu hình", "Cá nhân hóa thương hiệu", "Đổi tên, logo, giới thiệu, màu sắc và giao diện.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.CONFIGURATION_MANAGE, "Cấu hình", "Quản lý cấu hình", "Thay đổi cấu hình nghiệp vụ cấp hệ thống.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.INTEGRATIONS_MANAGE, "Cấu hình", "Quản lý tích hợp", "Cấu hình Redis và dịch vụ bên thứ ba.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.AI_USE, "Cấu hình", "Sử dụng AI (tương thích)", "Quyền tương thích cho AI service cũ.", systemOnly, PermissionRisk.MEDIUM, true),
        item(Permissions.CERTIFICATES_MANAGE, "Chứng chỉ", "Quản lý chứng chỉ", "Cấp, thu hồi và cấp lại chứng chỉ.", courseScopes, PermissionRisk.HIGH),
        item(Permissions.CERTIFICATES_READ_SELF, "Chứng chỉ", "Xem chứng chỉ cá nhân", "Xem chứng chỉ của chính mình.", systemOnly),
        item(Permissions.CERTIFICATE_TEMPLATES_MANAGE, "Chứng chỉ", "Quản lý mẫu chứng chỉ", "Tạo và phát hành mẫu chứng chỉ.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.COMPETENCIES_READ_SELF, "Năng lực", "Xem năng lực cá nhân", "Xem hồ sơ năng lực của chính mình.", systemOnly),
        item(Permissions.COMPETENCIES_READ_SCOPE, "Năng lực", "Xem năng lực theo phạm vi", "Xem hồ sơ năng lực trong phạm vi phụ trách.", organizationScopes, PermissionRisk.MEDIUM),
        item(Permissions.COMPETENCIES_MANAGE, "Năng lực", "Quản lý khung năng lực", "Tạo khung, nhóm và tiêu chí năng lực.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.COMPETENCIES_ASSESS, "Năng lực", "Đánh giá năng lực", "Ghi nhận mức năng lực cho người dùng.", organizationScopes, PermissionRisk.HIGH),
        item(Permissions.AUDIT_READ, "Vận hành", "Xem nhật ký kiểm toán", "Xem lịch sử thao tác nhạy cảm.", systemOnly, PermissionRisk.HIGH),
        item(Permissions.AUDIT_EXPORT, "Vận hành", "Xuất nhật ký kiểm toán", "Xuất dữ liệu audit phục vụ kiểm tra.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.OPERATIONS_MANAGE, "Vận hành", "Quản lý vận hành", "Thực hiện tác vụ vận hành được cho phép.", systemOnly, PermissionRisk.CRITICAL),
        item(Permissions.LICENSE_MANAGE, "Vận hành", "Quản lý giấy phép", "Nạp và kiểm tra license hệ thống.", systemOnly, PermissionRisk.CRITICAL),
    )

    private val byCode = definitions.associateBy { it.code }

    fun all(): List<PermissionDefinition> = definitions.sortedWith(compareBy({ it.group }, { it.label }))
    fun find(code: String): PermissionDefinition? = byCode[code]
    fun codes(): Set<String> = byCode.keys
}

object DefaultAccessProfiles {
    /** Exactly three product roles. They are mutually exclusive at account level. */
    val profiles: List<AccessProfileDefinition> = listOf(
        AccessProfileDefinition(
            code = "ADMIN",
            name = "Quản trị viên",
            description = "Quản trị tài khoản, tổ chức, thương hiệu, tích hợp, vận hành và báo cáo hệ thống.",
            permissions = DefaultRolePermissions.ADMIN,
            recommendedScopes = setOf("SYSTEM"),
            risk = PermissionRisk.CRITICAL,
        ),
        AccessProfileDefinition(
            code = "INSTRUCTOR",
            name = "Giảng viên",
            description = "Biên soạn khóa học, tạo bài kiểm tra từ tài liệu, vận hành bài thi và chấm điểm.",
            permissions = DefaultRolePermissions.INSTRUCTOR,
            recommendedScopes = setOf("SYSTEM", "COURSE", "EXAM"),
            risk = PermissionRisk.HIGH,
        ),
        AccessProfileDefinition(
            code = "STUDENT",
            name = "Học viên",
            description = "Học khóa được giao, làm bài kiểm tra trong khóa học, thi độc lập và xem kết quả cá nhân.",
            permissions = DefaultRolePermissions.STUDENT,
            recommendedScopes = setOf("SYSTEM"),
        ),
    )

    private val byCode = profiles.associateBy { it.code }
    fun find(code: String): AccessProfileDefinition? = byCode[code.uppercase()]
}
