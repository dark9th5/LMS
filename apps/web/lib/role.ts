import type { PortalUser } from "./types";
import { landingForUser } from "./authorization";

/** @deprecated Use landingForUser. Kept for older imports during migration. */
export function landingForRoles(roles: string[] | undefined): string {
  if (!roles?.length) return "/news";
  return roles.includes("ADMIN") || roles.includes("INSTRUCTOR") ? "/dashboard" : "/learning";
}

export function landingForSession(user: PortalUser): string {
  return landingForUser(user);
}
