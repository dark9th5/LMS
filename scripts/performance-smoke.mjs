#!/usr/bin/env node
const baseUrl = (process.env.LMSPILOT_GATEWAY_PUBLIC_URL || "http://localhost:8080").replace(/\/$/, "");
const username = process.env.LMSPILOT_BENCHMARK_USER || "admin";
const password = process.env.LMSPILOT_DEFAULT_ADMIN_PASSWORD || "ChangeMe-Immediately-123!";
const rounds = Math.max(1, Number(process.env.LMSPILOT_BENCHMARK_ROUNDS || 3));
const login = await fetch(`${baseUrl}/api/v1/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ username, password }) });
if (!login.ok) throw new Error(`Login failed: ${login.status}`);
const session = await login.json();
const token = session.accessToken;
const endpoints = [
  "/api/v1/users?page=0&size=20",
  "/api/v1/organization/units/tree",
  "/api/v1/courses?page=0&size=20",
  "/api/v1/exams?page=0&size=20",
  "/api/v1/branding",
];
const samples = new Map(endpoints.map((path) => [path, []]));
for (let round = 0; round < rounds; round += 1) {
  await Promise.all(endpoints.map(async (path) => {
    const started = performance.now();
    const response = await fetch(`${baseUrl}${path}`, { headers: { Authorization: `Bearer ${token}`, "X-Correlation-ID": crypto.randomUUID() } });
    const elapsed = performance.now() - started;
    samples.get(path).push(elapsed);
    if (!response.ok && response.status !== 403) throw new Error(`${path}: ${response.status}`);
    await response.arrayBuffer();
  }));
}
let slow = false;
for (const [path, values] of samples) {
  const sorted = [...values].sort((a,b) => a-b);
  const avg = values.reduce((a,b) => a+b,0) / values.length;
  const p95 = sorted[Math.min(sorted.length - 1, Math.ceil(sorted.length * .95) - 1)];
  console.log(`${path.padEnd(45)} avg=${avg.toFixed(0)}ms p95=${p95.toFixed(0)}ms`);
  if (p95 > 8000) slow = true;
}
if (slow) process.exit(2);
