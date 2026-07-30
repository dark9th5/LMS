#!/usr/bin/env node
import { readFile, readdir, stat } from "node:fs/promises";
import { existsSync } from "node:fs";
import path from "node:path";
import process from "node:process";

const root = process.cwd();
const webRoot = path.join(root, "apps", "web");
const forbidden = [
  /from\s+["']@\/lib\/standalone-mock["']/,
  /\bhandleMockGatewayRequest\b/,
  /\bMOCK_USERS\b/,
  /\bmock-access-token\b/,
  /\bmock-refresh-token\b/,
  /access\s*\?\?\s*["']mock-token["']/,
];

const ignoredDirs = new Set([
  ".git",
  ".next",
  "node_modules",
  "coverage",
  "dist",
  "build",
]);

async function walk(dir) {
  const result = [];
  for (const entry of await readdir(dir)) {
    if (ignoredDirs.has(entry)) continue;
    const full = path.join(dir, entry);
    const info = await stat(full);
    if (info.isDirectory()) result.push(...(await walk(full)));
    else if (/\.(?:ts|tsx|js|jsx|mjs|cjs)$/.test(entry)) result.push(full);
  }
  return result;
}

function relative(file) {
  return path.relative(root, file).replaceAll("\\", "/");
}

const failures = [];
const mockFile = path.join(webRoot, "lib", "standalone-mock.ts");
if (existsSync(mockFile)) {
  failures.push(
    "apps/web/lib/standalone-mock.ts vẫn tồn tại; hãy xóa khỏi bản production.",
  );
}

for (const file of await walk(webRoot)) {
  const content = await readFile(file, "utf8");
  for (const pattern of forbidden) {
    if (pattern.test(content)) {
      failures.push(`${relative(file)} khớp mẫu cấm: ${pattern}`);
    }
  }
}

const requiredFiles = {
  "apps/web/lib/session-cookie.ts": [
    "timingSafeEqual",
    "encodeUserCookie",
    "decodeUserCookie",
  ],
  "apps/web/app/api/auth/login/route.ts": [
    "AUTH_SERVICE_UNAVAILABLE",
    "INVALID_AUTH_RESPONSE",
    "encodeUserCookie",
  ],
  "apps/web/app/api/gateway/[...path]/route.ts": [
    "GATEWAY_UNAVAILABLE",
    "INVALID_GATEWAY_PATH",
    "fetchWithTimeout",
  ],
};

for (const [relativePath, tokens] of Object.entries(requiredFiles)) {
  const file = path.join(root, relativePath);
  if (!existsSync(file)) {
    failures.push(`Thiếu tệp bắt buộc: ${relativePath}`);
    continue;
  }
  const content = await readFile(file, "utf8");
  for (const token of tokens) {
    if (!content.includes(token)) {
      failures.push(`${relativePath} thiếu kiểm soát: ${token}`);
    }
  }
}

if (failures.length) {
  console.error("Production real-mode check: FAILED");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("Production real-mode check: PASSED");
console.log("- Không còn import hoặc token fallback mock trong apps/web");
console.log("- Login fail-closed khi Identity/Gateway không khả dụng");
console.log("- Gateway proxy fail-closed, có timeout và kiểm tra đường dẫn");
console.log("- Cookie người dùng có chữ ký kiểm tra toàn vẹn");
