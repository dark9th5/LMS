import { cookies } from "next/headers";
import type { PortalUser } from "./types";

function validUser(value: unknown): value is PortalUser {
  if (!value || typeof value !== "object") return false;
  const user = value as Partial<PortalUser>;
  return typeof user.id === "string" && typeof user.username === "string" && typeof user.fullName === "string" && Array.isArray(user.roles) && Array.isArray(user.permissions);
}

export async function getUser(): Promise<PortalUser | null> {
  const value = (await cookies()).get("lmspilot_user")?.value;
  if (!value) return null;
  try {
    const user = JSON.parse(Buffer.from(value, "base64url").toString("utf8")) as unknown;
    return validUser(user) ? user : null;
  } catch { return null; }
}
