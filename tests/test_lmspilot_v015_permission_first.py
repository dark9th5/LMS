from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def all_backend() -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in sorted((ROOT / "backend").rglob("*.java")))


class PermissionFirstTests(unittest.TestCase):
    def test_permission_catalog_is_centralized(self) -> None:
        source = (ROOT / "backend/platform-contracts/src/main/java/com/lmspilot/contracts/Permissions.java").read_text(encoding="utf-8")
        self.assertIn("COURSES_CREATE", source)
        self.assertIn("EXAMS_MANAGE", source)
        self.assertIn("USERS_CREATE", source)

    def test_runtime_authorization_uses_permissions_and_scopes(self) -> None:
        source = all_backend()
        self.assertIn("hasAuthority", source)
        self.assertIn("ScopedAuthorizationClient", source)
        self.assertIn("scopeType", source)
        self.assertIn("scopeId", source)

    def test_backend_does_not_use_role_name_as_business_gate(self) -> None:
        source = all_backend()
        self.assertNotRegex(source, r"@PreAuthorize\([^\n]*hasRole")
        
    def test_exclusive_role_model_is_enforced_at_identity_boundary(self) -> None:
        source = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "backend/services/identity-service/src/main/java").rglob("*.java"))
        self.assertIn("EXCLUSIVE_ROLE_MODEL", source)
        self.assertIn("codes.size()!=1", source.replace(" ", ""))

    def test_frontend_portals_are_role_specific(self) -> None:
        role = (ROOT / "apps/web/lib/role.ts").read_text(encoding="utf-8")
        for value in ("ADMIN", "INSTRUCTOR", "STUDENT"):
            self.assertIn(value, role)


if __name__ == "__main__":
    unittest.main()
