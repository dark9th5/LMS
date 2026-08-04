#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json
import re
import subprocess
import sys
import shutil
import zipfile

try:
    import yaml
except ImportError as exc:
    raise SystemExit("PyYAML is required for repository validation") from exc

ROOT = Path(__file__).resolve().parents[1]
IGNORED_DIRECTORY_NAMES = {
    ".git",
    ".gradle",
    ".next",
    ".runtime",
    "__pycache__",
    "build",
    "coverage",
    "node_modules",
}


def is_repository_source(path: Path) -> bool:
    return not any(part in IGNORED_DIRECTORY_NAMES for part in path.relative_to(ROOT).parts)


def source_files(pattern: str) -> list[Path]:
    return [path for path in ROOT.rglob(pattern) if is_repository_source(path)]


class UniqueKeyLoader(yaml.SafeLoader):
    pass


def construct_mapping(loader: UniqueKeyLoader, node: yaml.MappingNode, deep: bool = False):
    # Reject duplicate keys written in the same mapping, while allowing YAML merge
    # keys (<<) to be overridden by explicit service settings.
    explicit = set()
    for key_node, _ in node.value:
        if key_node.tag == "tag:yaml.org,2002:merge":
            continue
        key = loader.construct_object(key_node, deep=False)
        if key in explicit:
            raise ValueError(f"duplicate YAML key: {key!r} at line {key_node.start_mark.line + 1}")
        explicit.add(key)
    loader.flatten_mapping(node)
    return yaml.SafeLoader.construct_mapping(loader, node, deep=deep)


UniqueKeyLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
    construct_mapping,
)


def fail(message: str) -> None:
    raise SystemExit(message)


def read(path: str | Path) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


required = [
    "README.md",
    "docker-compose.yml",
    ".env.example",
    "apps/web/package.json",
    "apps/web/public/.gitkeep",
    "backend/settings.gradle.kts",
    "backend/build.gradle.kts",
    "backend/gradlew",
    "backend/gradlew.bat",
    "backend/gradle/wrapper/gradle-wrapper.jar",
    "backend/gradle/wrapper/gradle-wrapper.properties",
    "docs/requirement-traceability.md",
    "scripts/smoke-test.sh",
    "scripts/smoke-test.ps1",
    "scripts/setup.sh",
    "scripts/setup.ps1",
    "scripts/preflight.sh",
    "scripts/preflight.ps1",
    "scripts/test-static.sh",
    "scripts/test-static.ps1",
    "infrastructure/postgres/init.sh",
]
missing = [path for path in required if not (ROOT / path).exists()]
if missing:
    fail("Missing required files: " + ", ".join(missing))

# JSON syntax.
json_files = source_files("*.json")
for path in json_files:
    with path.open(encoding="utf-8") as stream:
        json.load(stream)

# YAML syntax and duplicate-key protection.
yaml_files = [*source_files("*.yml"), *source_files("*.yaml")]
for path in yaml_files:
    try:
        with path.open(encoding="utf-8") as stream:
            yaml.load(stream, Loader=UniqueKeyLoader)
    except Exception as exc:
        fail(f"Invalid YAML {path.relative_to(ROOT)}: {exc}")

# Gradle structure.
settings = read("backend/settings.gradle.kts")
services_root = ROOT / "backend/services"
services = sorted(path for path in services_root.iterdir() if path.is_dir())
for service in services:
    if not (service / "build.gradle.kts").exists():
        fail(f"Missing build.gradle.kts: {service.name}")
    if not list((service / "src/main/kotlin").rglob("*Application.kt")):
        fail(f"Missing application entry point: {service.name}")
    if f'":services:{service.name}"' not in settings:
        fail(f"Service is not included in Gradle settings: {service.name}")
    migration_dir = service / "src/main/resources/db/migration"
    versions: dict[str, list[str]] = {}
    if migration_dir.exists():
        for migration in migration_dir.glob("V*__*.sql"):
            match = re.match(r"V([^_]+)__", migration.name)
            if match:
                versions.setdefault(match.group(1), []).append(migration.name)
    duplicates = {version: names for version, names in versions.items() if len(names) > 1}
    if duplicates:
        fail(f"Duplicate Flyway versions in {service.name}: {duplicates}")

contracts_build = read("backend/platform-contracts/build.gradle.kts")
if "io.spring.dependency-management" not in contracts_build or "spring-boot-dependencies" not in contracts_build:
    fail("platform-contracts does not have dependency version management")

wrapper = ROOT / "backend/gradle/wrapper/gradle-wrapper.jar"
if not zipfile.is_zipfile(wrapper):
    fail("Gradle wrapper JAR is missing or corrupt")
wrapper_props = read("backend/gradle/wrapper/gradle-wrapper.properties")
if "https\\://services.gradle.org/distributions/gradle-8.14.5-bin.zip" not in wrapper_props:
    fail("Unexpected Gradle wrapper distribution")
if "distributionSha256Sum=9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14" not in wrapper_props:
    fail("Gradle distribution checksum is missing or unexpected")

# Internal API authorization.
for source in services_root.rglob("*.kt"):
    text = source.read_text(encoding="utf-8")
    if '@RequestMapping("/internal/v1' in text:
        if "InternalTokenAuthorizer" not in text or "internal.require(" not in text:
            fail(f"Internal endpoint is missing service-token enforcement: {source.relative_to(ROOT)}")


# Public API routes must be reachable through the gateway, and literal frontend API paths
# must point to an implemented controller base path.
gateway_yaml = yaml.safe_load((ROOT / "backend/services/api-gateway/src/main/resources/application.yml").read_text(encoding="utf-8"))
routes = gateway_yaml["spring"]["cloud"]["gateway"]["server"]["webflux"]["routes"]
gateway_prefixes = []
for route in routes:
    predicates = route.get("predicates", [])
    path_mode = False
    for predicate in predicates:
        value = predicate if isinstance(predicate, str) else ""
        if value.startswith("Path="):
            path_mode = True
            value = value.removeprefix("Path=")
        elif not (path_mode and value.startswith("/")):
            path_mode = False
            continue
        for pattern in value.split(","):
            gateway_prefixes.append(pattern.strip().removesuffix("/**"))

controller_bases = set()
controller_pattern = re.compile(r'@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\(\s*"((?:/api/v1|/public/v1)[^"]*)"')
for source in services_root.rglob("*.kt"):
    for match in controller_pattern.finditer(source.read_text(encoding="utf-8")):
        path = match.group(2)
        # Keep the static prefix before path variables.
        controller_bases.add(path.split("/{", 1)[0].rstrip("/") or "/")

def gateway_matches(path: str) -> bool:
    return any(path == prefix or path.startswith(prefix + "/") for prefix in gateway_prefixes)

for base in sorted(controller_bases):
    if not gateway_matches(base):
        fail(f"Controller route is not exposed by API Gateway: {base}")

frontend_api_paths = set()
frontend_pattern = re.compile(r'["`](/api/v1/[A-Za-z0-9_?=&${}/:.\-]+)')
for source in source_files("*.ts*"):
    if ROOT / "apps/web" not in source.parents:
        continue
    frontend_api_paths.update(frontend_pattern.findall(source.read_text(encoding="utf-8")))
for path in sorted(frontend_api_paths):
    static = path.split("?", 1)[0].split("${", 1)[0].rstrip("/")
    if not gateway_matches(static):
        fail(f"Frontend API path is not exposed by API Gateway: {path}")
    if not any(static == base or static.startswith(base + "/") or base.startswith(static + "/") for base in controller_bases):
        fail(f"Frontend API path has no matching controller: {path}")

# Frontend/backend version alignment.
web_package = json.loads(read("apps/web/package.json"))
backend_build = read("backend/build.gradle.kts")
backend_version = re.search(r'version\s*=\s*"([^"]+)"', backend_build)
if not backend_version or backend_version.group(1) != web_package.get("version"):
    fail("Frontend and backend versions are not aligned")


# Security-sensitive runtime settings must not silently fall back to development secrets.
for app_config in (ROOT / "backend/services").glob("*/src/main/resources/application.yml"):
    text = app_config.read_text(encoding="utf-8")
    if "development-secret-must-be-at-least-thirty-two-bytes" in text or "development-internal-token" in text:
        fail(f"Insecure runtime secret fallback in {app_config.relative_to(ROOT)}")

react_version = tuple(int(part) for part in web_package["dependencies"]["react"].split(".")[:3])
react_dom_version = tuple(int(part) for part in web_package["dependencies"]["react-dom"].split(".")[:3])
if react_version < (19, 2, 6) or react_dom_version != react_version:
    fail("React/React DOM must use the aligned patched 19.2.6+ release")

# Known API wiring that previously broke first-run flows.
web_source = "\n".join(
    path.read_text(encoding="utf-8")
    for path in source_files("*.tsx")
    if ROOT / "apps/web" in path.parents
)
if '"/api/v1/organization"' in web_source:
    fail("Frontend still calls obsolete /api/v1/organization endpoint")
if '"/api/v1/organization/units"' not in web_source:
    fail("Frontend organization endpoint is missing")

compose_text = read("docker-compose.yml")
for required_setting in (
    "LMSPILOT_COOKIE_SECURE",
    "LMSPILOT_SEED_DEMO",
    "LMSPILOT_DEFAULT_ADMIN_PASSWORD",
    "LMSPILOT_INTERNAL_TOKEN",
    "POSTGRES_SERVICE_PASSWORD",
):
    if required_setting not in compose_text:
        fail(f"Runtime setting is missing from docker-compose.yml: {required_setting}")

# Every JPA service must have Flyway scripts, unique versions, schema initialization and compose credentials.
migration_services = []
init_script = read("infrastructure/postgres/init.sh")
for service in services:
    build = (service / "build.gradle.kts").read_text(encoding="utf-8")
    if "spring-boot-starter-data-jpa" not in build:
        continue
    migration_services.append(service.name)
    migration_dir = service / "src/main/resources/db/migration"
    migrations = sorted(migration_dir.glob("V*.sql")) if migration_dir.exists() else []
    if not migrations:
        fail(f"JPA service has no Flyway migration: {service.name}")
    versions = []
    for migration in migrations:
        match = re.match(r"V([^_]+)__", migration.name)
        if not match:
            fail(f"Invalid Flyway migration name: {migration.relative_to(ROOT)}")
        versions.append(match.group(1))
        if not migration.read_text(encoding="utf-8").strip().endswith(";"):
            fail(f"Migration does not end with semicolon: {migration.relative_to(ROOT)}")
    if len(versions) != len(set(versions)):
        fail(f"Duplicate Flyway version in {service.name}")
    schema = service.name.removesuffix("-service").replace("-", "_")
    if schema == "file_storage":
        db_user = "file_storage_user"
    else:
        db_user = f"{schema}_user"
    if schema not in init_script:
        fail(f"Missing PostgreSQL schema initialization for {service.name}")
    if f"DB_USERNAME: {db_user}" not in compose_text:
        fail(f"Missing DB_USERNAME wiring for {service.name}")
    if "DB_PASSWORD: ${POSTGRES_SERVICE_PASSWORD}" not in compose_text:
        fail("Service database passwords are not wired from environment")

# Shell scripts must parse.
for path in (ROOT / "scripts").glob("*.sh"):
    if shutil.which("bash"):
        result = subprocess.run(["bash", "-n", str(path)], capture_output=True, text=True)
        if result.returncode:
            fail(f"Shell syntax error in {path.relative_to(ROOT)}: {result.stderr.strip()}")

# Dockerfiles must include the build outputs they later copy.
web_dockerfile = read("apps/web/Dockerfile")
if "/app/public" not in web_dockerfile or not (ROOT / "apps/web/public").is_dir():
    fail("Web Dockerfile public directory wiring is incomplete")
if "./gradlew" not in read("backend/Dockerfile"):
    fail("Backend Dockerfile is not using the repository Gradle wrapper")

# Basic secret safety in examples: placeholders are allowed, but weak literal defaults are not.
env = {}
for line in read(".env.example").splitlines():
    if line and not line.startswith("#") and "=" in line:
        key, value = line.split("=", 1)
        env[key] = value
for key in ("LMSPILOT_JWT_SECRET", "LMSPILOT_INTERNAL_TOKEN"):
    if len(env.get(key, "")) < 32:
        fail(f"{key} example must be at least 32 characters")

print(
    f"OK: {len(json_files)} JSON, {len(yaml_files)} YAML, {len(services)} services, "
    f"{len(migration_services)} Flyway services, API wiring, wrapper, scripts and Docker layout validated."
)
