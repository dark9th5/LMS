#!/usr/bin/env node
const baseUrl = (process.env.LMSPILOT_GATEWAY_PUBLIC_URL || "http://localhost:8080").replace(/\/$/, "");
const password = process.env.LMSPILOT_DEFAULT_ADMIN_PASSWORD || "ChangeMe-Immediately-123!";
const accounts = [
  ["admin", "ADMIN"],
  ["instructor", "INSTRUCTOR"],
  ["student", "STUDENT"],
];
let failures = 0;
for (const [username, expectedRole] of accounts) {
  const started = performance.now();
  try {
    const response = await fetch(`${baseUrl}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Correlation-ID": crypto.randomUUID() },
      body: JSON.stringify({ username, password }),
    });
    const elapsed = Math.round(performance.now() - started);
    const body = await response.json().catch(() => ({}));
    const roles = Array.isArray(body?.user?.roles) ? body.user.roles : [];
    const ok = response.ok && roles.length === 1 && roles[0] === expectedRole && body.user.primaryRole === expectedRole;
    console.log(`${ok ? "PASS" : "FAIL"} ${username.padEnd(10)} ${response.status} ${elapsed}ms role=${roles.join(",") || "-"}`);
    if (!ok) failures += 1;
  } catch (error) {
    console.error(`FAIL ${username}: ${error instanceof Error ? error.message : error}`);
    failures += 1;
  }
}
if (failures) process.exit(1);
