from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


class PermissionFirstV015Tests(unittest.TestCase):
    def test_every_permission_has_human_readable_metadata(self) -> None:
        permissions = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/Permissions.kt")
        catalog = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/AccessProfiles.kt")
        permission_block = permissions.split("object Permissions {", 1)[1].split("object DefaultRolePermissions", 1)[0]
        constants = set(re.findall(r"const val\s+([A-Z0-9_]+)\s*=", permission_block))
        described = set(re.findall(r"item\(Permissions\.([A-Z0-9_]+)", catalog))
        self.assertEqual(constants, described)
        self.assertIn("allowedScopes", catalog)
        self.assertIn("PermissionRisk.CRITICAL", catalog)

    def test_account_types_are_not_teaching_roles(self) -> None:
        entities = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/domain/Entities.kt")
        profiles = text("backend/platform-contracts/src/main/kotlin/com/lmspilot/contracts/AccessProfiles.kt")
        models = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Models.kt")
        self.assertIn("enum class AccountType { SYSTEM_ADMIN, USER }", entities)
        codes = re.findall(r'AccessProfileDefinition\(\s*code = "([A-Z_]+)"', profiles)
        self.assertEqual(["ADMIN", "INSTRUCTOR", "STUDENT"], codes)
        self.assertIn("@field:Size(min = 1, max = 1) val roleCodes", models)

    def test_backend_services_do_not_gate_by_role_name(self) -> None:
        paths = list((ROOT / "backend/services").rglob("*.kt"))
        sources = "\n".join(path.read_text(encoding="utf-8") for path in paths)
        self.assertNotRegex(sources, r"@PreAuthorize\([^\n]*has(?:Any)?Role\(")
        role_gate_paths = [
            path.relative_to(ROOT).as_posix()
            for path in paths
            if "CurrentUser.roles()" in path.read_text(encoding="utf-8")
        ]
        self.assertEqual([
            "backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileStorageApi.kt"
        ], role_gate_paths)
        storage = text(role_gate_paths[0])
        self.assertIn("requirePurposeForRole", storage)
        self.assertIn("FILE_PURPOSE_ROLE_FORBIDDEN", storage)
        self.assertIn("CurrentUser.authorities()", sources)

    def test_permission_console_previews_explains_and_revokes(self) -> None:
        controllers = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/Controllers.kt")
        catalog_api = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/api/AuthorizationCatalogApi.kt")
        ui = text("apps/web/components/WorkspaceControlCenter.tsx")
        self.assertIn('@PostMapping("/grants/preview")', controllers)
        self.assertIn('@GetMapping("/explain")', controllers)
        self.assertIn('@GetMapping("/users/{userId}/assignments")', catalog_api)
        self.assertIn("Xem trước tác động", ui)
        self.assertIn("Giải thích nguồn quyền", ui)
        self.assertIn("Thu hồi", ui)

    def test_frontend_assignment_lists_use_permissions_not_legacy_roles(self) -> None:
        shell = text("apps/web/components/AppShell.tsx")
        learners = text("apps/web/components/CourseLearnersPanel.tsx")
        self.assertFalse((ROOT / "apps/web/components/ClassesPage.tsx").exists())
        self.assertFalse((ROOT / "apps/web/components/ClassDetail.tsx").exists())
        self.assertIn("ROLE_NAVIGATION", shell)
        self.assertIn("/api/v1/course-assignments", learners)
        self.assertIn('method: "POST"', learners)
        self.assertNotIn("roles.includes", shell)

    def test_scoped_profiles_filter_incompatible_permissions_and_global_claims(self) -> None:
        authorization = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/AuthorizationService.kt")
        token = text("backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/TokenService.kt")
        support = text("backend/service-support/src/main/kotlin/com/lmspilot/support/security/JwtSupport.kt")
        kpi = text("backend/services/reporting-service/src/main/kotlin/com/lmspilot/reporting/api/KpiReportingApi.kt")
        self.assertIn("compatiblePermissions", authorization)
        self.assertIn("PERMISSION_SCOPE_NOT_ALLOWED", authorization)
        self.assertIn('claim("globalPermissions"', token)
        self.assertIn("globalAuthorities()", support)
        self.assertIn("CurrentUser.globalAuthorities()", kpi)


if __name__ == "__main__":
    unittest.main()
