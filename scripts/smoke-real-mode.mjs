#!/usr/bin/env node
import process from "node:process";

const baseUrl = (process.env.LMSPILOT_WEB_URL ?? "http://localhost:3000").replace(
  /\/+$/,
  "",
);

async function request(path, init = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10_000);
  try {
    return await fetch(`${baseUrl}${path}`, {
      redirect: "manual",
      ...init,
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timer);
  }
}

const failures = [];

try {
  const protectedResponse = await request("/api/gateway/api/v1/courses");
  if (protectedResponse.status !== 401) {
    failures.push(
      `API không có phiên phải trả 401, thực tế trả ${protectedResponse.status}.`,
    );
  }

  const invalidLogin = await request("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username: `invalid-${Date.now()}`,
      password: "definitely-invalid-password",
    }),
  });

  const invalidBody = await invalidLogin.text();
  if (invalidLogin.status === 200) {
    failures.push(
      `Sai tài khoản vẫn đăng nhập thành công: HTTP 200, body=${invalidBody.slice(0, 300)}`,
    );
  }
  if (/mock-access-token|mock-refresh-token|standalone/i.test(invalidBody)) {
    failures.push("Phản hồi đăng nhập còn dấu hiệu fallback mock.");
  }

  const username = process.env.LMSPILOT_SMOKE_USERNAME;
  const password = process.env.LMSPILOT_SMOKE_PASSWORD;
  if (username && password) {
    const validLogin = await request("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    if (!validLogin.ok) {
      failures.push(
        `Tài khoản smoke-test không đăng nhập được: HTTP ${validLogin.status}.`,
      );
    } else {
      console.log(
        "Đăng nhập thật bằng LMSPILOT_SMOKE_USERNAME/PASSWORD: PASSED",
      );
    }
  } else {
    console.log(
      "Bỏ qua login hợp lệ: chưa đặt LMSPILOT_SMOKE_USERNAME và LMSPILOT_SMOKE_PASSWORD.",
    );
  }
} catch (error) {
  failures.push(
    `Không kết nối được ${baseUrl}: ${
      error instanceof Error ? error.message : String(error)
    }`,
  );
}

if (failures.length) {
  console.error("Real-mode smoke test: FAILED");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("Real-mode smoke test: PASSED");
