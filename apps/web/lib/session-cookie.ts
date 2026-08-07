import "server-only";

import { createHmac, timingSafeEqual } from "node:crypto";
import type { PortalUser } from "./types";

const COOKIE_VERSION = "v1";
const COOKIE_CONTEXT = "lmspilot:web-user-cookie:v1";

function validUser(value: unknown): value is PortalUser {
  if (!value || typeof value !== "object") return false;
  const user = value as Partial<PortalUser>;
  return (
    typeof user.id === "string" &&
    typeof user.username === "string" &&
    typeof user.fullName === "string" &&
    Array.isArray(user.roles) &&
    user.roles.length >= 1 &&
    user.roles.every((role) => ["ADMIN", "INSTRUCTOR", "STUDENT"].includes(String(role))) &&
    typeof user.primaryRole === "string" &&
    ["ADMIN", "INSTRUCTOR", "STUDENT"].includes(user.primaryRole) &&
    Array.isArray(user.permissions) &&
    user.permissions.every((permission) => typeof permission === "string")
  );
}

function secret(): Buffer {
  const configured =
    process.env.LMSPILOT_SESSION_COOKIE_SECRET ??
    process.env.LMSPILOT_JWT_SECRET;

  if (configured && configured.length >= 32) {
    return createHmac("sha256", configured).update(COOKIE_CONTEXT).digest();
  }

  if (process.env.NODE_ENV !== "production") {
    return createHmac("sha256", "lmspilot-development-only-change-me")
      .update(COOKIE_CONTEXT)
      .digest();
  }

  throw new Error(
    "Missing LMSPILOT_SESSION_COOKIE_SECRET (or LMSPILOT_JWT_SECRET) with at least 32 characters",
  );
}

function sign(payload: string): string {
  return createHmac("sha256", secret()).update(payload).digest("base64url");
}

export function encodeUserCookie(user: PortalUser): string {
  if (!validUser(user)) {
    throw new TypeError("Cannot encode an invalid portal user");
  }

  const payload = Buffer.from(JSON.stringify(user), "utf8").toString("base64url");
  return `${COOKIE_VERSION}.${payload}.${sign(`${COOKIE_VERSION}.${payload}`)}`;
}

export function decodeUserCookie(value: string): PortalUser | null {
  const [version, payload, signature, extra] = value.split(".");
  if (extra !== undefined || version !== COOKIE_VERSION || !payload || !signature) {
    return null;
  }

  const expected = sign(`${version}.${payload}`);
  const suppliedBytes = Buffer.from(signature);
  const expectedBytes = Buffer.from(expected);

  if (
    suppliedBytes.length !== expectedBytes.length ||
    !timingSafeEqual(suppliedBytes, expectedBytes)
  ) {
    return null;
  }

  try {
    const decoded = JSON.parse(
      Buffer.from(payload, "base64url").toString("utf8"),
    ) as unknown;
    return validUser(decoded) ? decoded : null;
  } catch {
    return null;
  }
}
