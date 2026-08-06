#!/usr/bin/env python3
"""Static JPA-to-Flyway column contract check for LMSPilot services.

This is intentionally conservative: it verifies columns explicitly mapped by
@Entity classes are present in that service's Flyway SQL. It does not replace
Hibernate schema validation against a real PostgreSQL instance.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SERVICES = ROOT / "backend" / "services"


def snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def matching_paren(text: str, opening: int) -> int:
    depth = 0
    quote = None
    i = opening
    while i < len(text):
        char = text[i]
        if quote:
            if char == quote and (i == 0 or text[i - 1] != "\\"):
                quote = None
        elif char in "'\"":
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def sql_schema(service: Path) -> dict[str, set[str]]:
    merged = "\n".join(p.read_text(encoding="utf-8") for p in sorted((service / "src/main/resources/db/migration").glob("*.sql")))
    tables: dict[str, set[str]] = {}
    for match in re.finditer(r"\bcreate\s+table\s+(?:if\s+not\s+exists\s+)?(?:\w+\.)?([\w\"]+)\s*\(", merged, re.I):
        table = match.group(1).strip('"').lower()
        start = merged.find("(", match.start())
        end = matching_paren(merged, start)
        if end < 0:
            continue
        body = merged[start + 1:end]
        columns = tables.setdefault(table, set())
        depth = 0
        chunk = []
        parts = []
        for char in body:
            if char == "(": depth += 1
            elif char == ")": depth -= 1
            if char == "," and depth == 0:
                parts.append("".join(chunk)); chunk = []
            else:
                chunk.append(char)
        parts.append("".join(chunk))
        for part in parts:
            stripped = re.sub(r"--.*", "", part).strip()
            if not stripped:
                continue
            first = re.match(r'"?([A-Za-z_][\w]*)"?\s+', stripped)
            if not first:
                continue
            name = first.group(1).lower()
            if name in {"constraint", "primary", "unique", "foreign", "check", "exclude"}:
                continue
            columns.add(name)
    for statement in re.finditer(r"\balter\s+table\s+(?:if\s+exists\s+)?(?:\w+\.)?([\w\"]+)\s+([^;]+);", merged, re.I | re.S):
        table = statement.group(1).strip('"').lower()
        for added in re.finditer(r"\badd\s+(?:column\s+)?(?:if\s+not\s+exists\s+)?\"?([A-Za-z_][\w]*)\"?", statement.group(2), re.I):
            tables.setdefault(table, set()).add(added.group(1).lower())
    return tables


def entity_columns(java: str) -> tuple[str, set[str]] | None:
    if "@Entity" not in java:
        return None
    table_match = re.search(r"@Table\s*\(\s*name\s*=\s*\"([^\"]+)\"", java, re.S)
    if not table_match:
        return None
    table = table_match.group(1).lower()
    columns: set[str] = set()
    # Capture annotation block followed by a field declaration. Methods are excluded by semicolon requirement.
    pattern = re.compile(r"((?:\s*@[A-Za-z_][\w.]*(?:\([^;]*?\))?\s*)+)\s*(?:public|protected|private)\s+(?:final\s+)?[\w<>, ?\[\].]+\s+(\w+)\s*(?:=[^;]*)?;", re.S)
    for annotations, field in pattern.findall(java):
        if not any(token in annotations for token in ("@Column", "@Id", "@Version", "@EmbeddedId")):
            continue
        if any(token in annotations for token in ("@Transient", "@ElementCollection", "@ManyToMany", "@OneToMany", "@ManyToOne", "@OneToOne")):
            continue
        named = re.search(r"@Column\s*\([^)]*\bname\s*=\s*\"([^\"]+)\"", annotations, re.S)
        columns.add((named.group(1) if named else snake(field)).lower())
    return table, columns


def main() -> int:
    issues: list[str] = []
    checked = 0
    for service in sorted(p for p in SERVICES.iterdir() if p.is_dir()):
        migration_dir = service / "src/main/resources/db/migration"
        if not migration_dir.exists():
            continue
        schema = sql_schema(service)
        for source in sorted((service / "src/main/java").rglob("*.java")):
            mapped = entity_columns(source.read_text(encoding="utf-8"))
            if not mapped:
                continue
            table, columns = mapped
            checked += 1
            if table not in schema:
                issues.append(f"{service.name}: {source.name}: table {table} not found in Flyway")
                continue
            missing = sorted(columns - schema[table])
            if missing:
                issues.append(f"{service.name}: {source.name}: missing columns {', '.join(missing)}")
    if issues:
        print(f"FAIL: checked {checked} entities; {len(issues)} contract issue(s)")
        for issue in issues:
            print("-", issue)
        return 1
    print(f"OK: {checked} JPA entities map to columns declared by their service Flyway migrations.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
