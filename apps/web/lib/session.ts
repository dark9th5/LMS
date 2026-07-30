import { cookies } from "next/headers";
import type { PortalUser } from "./types";
import { decodeUserCookie } from "./session-cookie";

export async function getUser(): Promise<PortalUser | null> {
  const value = (await cookies()).get("lmspilot_user")?.value;
  return value ? decodeUserCookie(value) : null;
}
