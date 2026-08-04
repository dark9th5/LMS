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
        self.assertIn("enum class AccountType { SYSTEM_ADMIN, USER }", entities)
        for code in ["BASIC_USER", "COURSE_AUTHOR", "TRAINING_MANAGER", "EXAM_MANAGER", "GRADER"]:
            self.assertIn(f'code = "{code}"', profiles)

    def test_backend_services_do_not_gate_by_role_name(self) -> None:
        sources = "\n".join(
            path.read_text(encoding="utf-8")
            for path in (ROOT / "backend/services").rglob("*.kt")
        )
        self.assertNotIn("CurrentUser.roles()", sources)
        self.assertNotRegex(sources, r"has(?:Any)?Role\(")
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
        classes = text("apps/web/components/ClassesPage.tsx")
        detail = text("apps/web/components/ClassDetail.tsx")
        shell = text("apps/web/components/CosmicShell.tsx")
        self.assertNotIn("role=INSTRUCTOR", classes)
        self.assertNotIn("role=LEARNER", detail)
        self.assertIn('"classes:manage"', classes)
        self.assertIn('includes("courses:learn")', detail)
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
