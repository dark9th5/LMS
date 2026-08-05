import "server-only";

import { redirect } from "next/navigation";
import { landingForUser } from "./authorization";
import { resolvePortalRole, type PortalRole } from "./role";
import { getUser } from "./session";
import type { PortalUser } from "./types";

export async function requireAuthenticatedUser(): Promise<PortalUser> {
  const user = await getUser();
  if (!user) {
    redirect("/login");
    throw new Error("UNREACHABLE_AFTER_REDIRECT");
  }
  return user;
}

export function requireRole(user: PortalUser, ...allowed: PortalRole[]): PortalRole {
  const role = resolvePortalRole(user);
  if (!allowed.includes(role)) {
    redirect(landingForUser(user));
    throw new Error("UNREACHABLE_AFTER_ROLE_REDIRECT");
  }
  return role;
}
