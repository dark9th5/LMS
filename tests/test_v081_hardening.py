from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class Release081HardeningTests(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_typescript_checker_performs_semantic_project_validation(self) -> None:
        checker = self.read("scripts/check-typescript.js")
        self.assertIn("ts.createProgram", checker)
        self.assertIn("ts.getPreEmitDiagnostics", checker)
        self.assertNotIn("transpileModule", checker)

    def test_frontend_dependency_fixes_are_locked(self) -> None:
        package = json.loads(self.read("apps/web/package.json"))
        self.assertEqual("16.2.12", package["dependencies"]["next"])
        self.assertEqual("8.5.25", package["overrides"]["postcss"])
        self.assertEqual("0.35.3", package["overrides"]["sharp"])
        self.assertIn("npm ci", self.read("apps/web/Dockerfile"))

    def test_spring_boot_and_cloud_release_trains_are_aligned(self) -> None:
        build = self.read("backend/build.gradle.kts")
        gateway = self.read("backend/services/api-gateway/build.gradle.kts")
        all_runtime = build + gateway + self.read("docker-compose.yml") + self.read(
            "backend/services/api-gateway/src/main/resources/application.yml"
        )
        self.assertIn('version "3.5.16"', build)
        self.assertIn('"2025.0.3"', gateway)
        self.assertNotIn("COMPATIBILITY_VERIFIER_ENABLED", all_runtime)
        self.assertNotIn("compatibility-verifier", all_runtime)

    def test_bulk_operation_replays_are_serialized_and_actor_bound(self) -> None:
        guard = self.read(
            "backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/BulkOperationGuard.kt"
        )
        migration = self.read(
            "backend/services/identity-service/src/main/resources/db/migration/V5__bulk_operation_serialization.sql"
        )
        services = "\n".join(
            self.read(path)
            for path in (
                "backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/AuthorizationService.kt",
                "backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/UserManagementService.kt",
                "backend/services/identity-service/src/main/kotlin/com/lmspilot/identity/service/UserImportService.kt",
            )
        )
        self.assertIn("saved.operationType != expectedType", guard)
        self.assertIn("saved.requestedBy != requestedBy", guard)
        self.assertIn("IDENTITY_BULK_OPERATION_SERIALIZATION", guard)
        self.assertIn("IDENTITY_BULK_OPERATION_SERIALIZATION", migration)
        for operation_type in (
            "BULK_CREATE_USERS",
            "BULK_GRANT",
            "BULK_REVOKE",
            "USER_FILE_IMPORT",
        ):
            self.assertIn(f'bulkGuard.replay(', services)
            self.assertIn(f'"{operation_type}"', services)

    def test_cookie_backed_mutations_reject_cross_site_requests(self) -> None:
        origin = self.read("apps/web/lib/request-origin.ts")
        self.assertIn('fetchSite === "cross-site"', origin)
        self.assertIn("supplied.host === expected.host", origin)
        for route in (
            "apps/web/app/api/auth/login/route.ts",
            "apps/web/app/api/auth/change-password/route.ts",
            "apps/web/app/api/auth/logout/route.ts",
            "apps/web/app/api/gateway/[...path]/route.ts",
        ):
            self.assertIn("isSameOriginMutation", self.read(route), route)
        self.assertNotIn("LOGIN ROUTE DEBUG", self.read("apps/web/app/api/auth/login/route.ts"))

    def test_generated_frontend_artifacts_are_excluded_from_validation(self) -> None:
        validator = self.read("scripts/validate-repository.py")
        gitignore = self.read(".gitignore")
        for name in ("node_modules", ".next", "build", "coverage"):
            self.assertIn(f'"{name}"', validator)
        self.assertIn("*.tsbuildinfo", gitignore)

    def test_file_routes_enforce_object_access_beyond_coarse_authority(self) -> None:
        storage = self.read(
            "backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileStorageApi.kt"
        )
        editing = self.read(
            "backend/services/file-storage-service/src/main/kotlin/com/lmspilot/filestorage/api/FileEditingApi.kt"
        )
        self.assertIn("fun metadata(id: UUID): StoredFileResponse = readable(id).response()", storage)
        self.assertIn("val entity = readable(id)", storage)
        self.assertIn('"FILE_READ_FORBIDDEN"', storage)
        self.assertIn("file.ownerId != CurrentUser.id()", editing)
        self.assertIn('"FILE_READ_FORBIDDEN"', editing)
        self.assertIn("effectivePort(uri) != effectivePort(configured)", editing)
        self.assertIn("followRedirects(HttpClient.Redirect.NEVER)", editing)
        self.assertIn("if (total > maxSizeBytes)", editing)

if __name__ == "__main__":
    unittest.main()
