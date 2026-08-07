package com.lmspilot.identity.service;

import com.lmspilot.identity.api.IdentityModels.*;
import com.lmspilot.identity.domain.*;
import com.lmspilot.support.api.ApiException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserImportService {
    private final UserManagementService users;
    private final UserAccountRepository repository;

    public UserImportService(UserManagementService users, UserAccountRepository repository) {
        this.users = users;
        this.repository = repository;
    }

    private record Sheet(List<String> headers, List<Map<String, String>> rows) {}

    private Sheet read(MultipartFile file) {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("users.csv");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMPORT_FILE", "Bản Java hiện hỗ trợ nhập CSV UTF-8; hãy xuất XLSX thành CSV trước khi nhập");
        }
        try {
            List<String> lines = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .lines()
                .filter(s -> !s.isBlank())
                .toList();
            if (lines.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_IMPORT_FILE", "Tệp nhập trống");
            }
            List<String> h = parse(lines.get(0));
            List<Map<String, String>> rows = new ArrayList<>();
            for (int n = 1; n < lines.size(); n++) {
                List<String> v = parse(lines.get(n));
                Map<String, String> row = new LinkedHashMap<>();
                for (int x = 0; x < h.size(); x++) {
                    row.put(h.get(x), x < v.size() ? v.get(x).trim() : "");
                }
                rows.add(row);
            }
            return new Sheet(h, rows);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMPORT_READ_FAILED", "Không đọc được tệp nhập");
        }
    }

    private List<String> parse(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    b.append('"');
                    i++;
                } else {
                    q = !q;
                }
            } else if (c == ',' && !q) {
                out.add(b.toString());
                b.setLength(0);
            } else {
                b.append(c);
            }
        }
        out.add(b.toString());
        return out;
    }

    private String find(List<String> headers, String... names) {
        for (String n : names) {
            for (String h : headers) {
                if (h.equalsIgnoreCase(n)) return h;
            }
        }
        return null;
    }

    public UserImportInspectionResponse inspect(MultipartFile file) {
        Sheet s = read(file);
        UserImportDetectedMapping d = new UserImportDetectedMapping(
            find(s.headers, "code", "mã"),
            find(s.headers, "username", "tên đăng nhập"),
            find(s.headers, "fullName", "họ tên"),
            find(s.headers, "email"),
            find(s.headers, "organizationUnitId"),
            find(s.headers, "roles", "role"),
            find(s.headers, "password", "mật khẩu"),
            find(s.headers, "status", "trạng thái")
        );
        return new UserImportInspectionResponse(file.getOriginalFilename(), s.headers, s.rows.stream().limit(5).toList(), d);
    }

    public UserImportPreviewResponse preview(MultipartFile file, UserImportMappingRequest m) {
        Sheet s = read(file);
        List<UserImportRowPreview> rows = new ArrayList<>();
        int creates = 0, updates = 0;
        for (int n = 0; n < s.rows.size(); n++) {
            Map<String, String> r = s.rows.get(n);
            List<String> errors = new ArrayList<>();
            String code = r.getOrDefault(m.codeColumn(), "").trim();
            String username = r.getOrDefault(m.usernameColumn(), "").trim();
            String full = r.getOrDefault(m.fullNameColumn(), "").trim();
            if (code.isBlank()) errors.add("Thiếu mã tài khoản");
            if (username.isBlank()) errors.add("Thiếu tên đăng nhập");
            if (full.isBlank()) errors.add("Thiếu họ tên");
            Set<String> role = parseRoles(m.roleCodesColumn() == null ? "" : r.getOrDefault(m.roleCodesColumn(), ""), m.defaultRoleCodes());
            if (role.size() != 1 || !Set.of("ADMIN", "INSTRUCTOR", "STUDENT").contains(role.iterator().next())) {
                errors.add("Mỗi dòng phải có đúng một vai trò ADMIN/INSTRUCTOR/STUDENT");
            }
            boolean exists = repository.findByUsernameIgnoreCase(username).isPresent();
            UserImportAction action = exists ? (m.mode() == UserImportMode.UPSERT ? UserImportAction.UPDATE : UserImportAction.SKIP) : UserImportAction.CREATE;
            if (action == UserImportAction.CREATE) creates++;
            if (action == UserImportAction.UPDATE) updates++;
            rows.add(new UserImportRowPreview(n + 2, code, username, full, value(r, m.emailColumn()), uuid(value(r, m.organizationUnitIdColumn())), role, status(value(r, m.statusColumn())), action, errors.isEmpty(), errors));
        }
        int valid = (int) rows.stream().filter(UserImportRowPreview::valid).count();
        return new UserImportPreviewResponse(file.getOriginalFilename(), s.headers, rows.size(), valid, rows.size() - valid, creates, updates, rows);
    }

    public UserImportCommitResponse commit(MultipartFile file, UserImportMappingRequest m, String operationId) {
        UserImportPreviewResponse preview = preview(file, m);
        if (m.failurePolicy() == UserImportFailurePolicy.ATOMIC && preview.invalidRows() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMPORT_VALIDATION_FAILED", "Tệp có dòng không hợp lệ");
        }
        Sheet s = read(file);
        List<UserImportRowResult> result = new ArrayList<>();
        int c = 0, u = 0, skip = 0, fail = 0;
        for (UserImportRowPreview row : preview.rows()) {
            if (!row.valid()) {
                fail++;
                result.add(new UserImportRowResult(row.rowNumber(), null, row.code(), row.username(), row.action(), false, row.errors()));
                continue;
            }
            try {
                Map<String, String> raw = s.rows.get(row.rowNumber() - 2);
                String pass = value(raw, m.passwordColumn());
                if (pass == null || pass.isBlank()) pass = m.defaultPassword();
                if (row.action() == UserImportAction.CREATE) {
                    if (pass == null || pass.isBlank()) throw new IllegalArgumentException("Thiếu mật khẩu");
                    UserSummary made = users.create(new CreateUserRequest(row.code(), row.username(), pass, row.fullName(), row.email(), row.organizationUnitId(), row.roleCodes(), true));
                    c++;
                    result.add(new UserImportRowResult(row.rowNumber(), made.id(), row.code(), row.username(), row.action(), true, List.of()));
                } else if (row.action() == UserImportAction.UPDATE) {
                    UUID id = repository.findByUsernameIgnoreCase(row.username()).orElseThrow().id;
                    UserSummary made = users.update(id, new UpdateUserRequest(row.fullName(), row.email(), row.organizationUnitId(), row.roleCodes(), row.status()));
                    u++;
                    result.add(new UserImportRowResult(row.rowNumber(), made.id(), row.code(), row.username(), row.action(), true, List.of()));
                } else {
                    skip++;
                    result.add(new UserImportRowResult(row.rowNumber(), null, row.code(), row.username(), row.action(), true, List.of()));
                }
            } catch (Exception e) {
                fail++;
                result.add(new UserImportRowResult(row.rowNumber(), null, row.code(), row.username(), row.action(), false, List.of(e.getMessage())));
            }
        }
        return new UserImportCommitResponse(operationId, file.getOriginalFilename(), preview.totalRows(), c, u, skip, fail, fail == 0 || m.failurePolicy() == UserImportFailurePolicy.PARTIAL, result);
    }

    private Set<String> parseRoles(String raw, Set<String> defaults) {
        if (raw == null || raw.isBlank()) return defaults;
        Set<String> out = new LinkedHashSet<>();
        for (String s : raw.split("[;|,]")) {
            if (!s.isBlank()) out.add(s.trim().toUpperCase(Locale.ROOT));
        }
        return out;
    }

    private String value(Map<String, String> r, String col) {
        return col == null ? null : r.get(col);
    }

    private UUID uuid(String s) {
        try {
            return s == null || s.isBlank() ? null : UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    private AccountStatus status(String s) {
        try {
            return s == null || s.isBlank() ? AccountStatus.ACTIVE : AccountStatus.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return AccountStatus.ACTIVE;
        }
    }
}
