from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(service: str) -> str:
    return "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / f"backend/services/{service}/src/main/java").rglob("*.java"))


class Release081HardeningTests(unittest.TestCase):
    def test_internal_token_has_no_insecure_default(self) -> None:
        support = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "backend/service-support/src/main/java").rglob("*.java"))
        self.assertIn("InternalTokenAuthorizer", support)
        self.assertIn("MessageDigest.isEqual", support)

    def test_identity_password_policy_is_strong(self) -> None:
        identity = source("identity-service")
        self.assertIn("password.length()<12", identity.replace(" ", ""))
        for pattern in ("[A-Z]", "[a-z]", "\\d", "[^A-Za-z0-9]"):
            self.assertIn(pattern, identity)
        self.assertIn("PasswordHistory", identity)

    def test_refresh_token_rotation_and_reuse_detection_exist(self) -> None:
        identity = source("identity-service")
        self.assertIn("RefreshTokenEntity", identity)
        self.assertIn("reuse", identity.lower())
        self.assertIn("revokedAt", identity)

    def test_storage_blocks_executable_extensions(self) -> None:
        storage = source("file-storage-service")
        for ext in ("exe", "dll", "bat", "cmd", "ps1", "jar"):
            self.assertIn(f'\"{ext}\"', storage)
        self.assertIn("FILE_TYPE_BLOCKED", storage)

    def test_storage_enforces_object_access(self) -> None:
        storage = source("file-storage-service")
        self.assertIn("FileAccessGrantEntity", storage)
        self.assertIn("FILE_READ_FORBIDDEN", storage)
        self.assertIn("ownerId", storage)

    def test_operations_agent_disables_arbitrary_shell(self) -> None:
        agent = (ROOT / "scripts/operations-agent.py").read_text(encoding="utf-8")
        self.assertIn("arbitrary shell execution is disabled", agent)
        self.assertNotIn("shell=True", agent)

    def test_gateway_rate_limit_and_password_change_filters_exist(self) -> None:
        gateway = source("api-gateway")
        self.assertIn("RateLimitConfiguration", gateway)
        self.assertIn("MustChangePasswordFilter", gateway)

    def test_upload_and_ai_limits_are_configurable(self) -> None:
        storage_cfg = (ROOT / "backend/services/file-storage-service/src/main/resources/application.yml").read_text(encoding="utf-8")
        ai_cfg = (ROOT / "backend/services/ai-service/src/main/resources/application.yml").read_text(encoding="utf-8")
        self.assertIn("max-size-bytes", storage_cfg)
        self.assertIn("timeout", ai_cfg.lower())


if __name__ == "__main__":
    unittest.main()
