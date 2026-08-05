import type { PortalUser } from "./types";
import { landingForRole, resolvePortalRole, type PortalRole } from "./role";

export function isSystemAdmin(user: PortalUser | null | undefined): boolean {
  return resolvePortalRole(user) === "ADMIN";
}

export function hasRole(
  user: PortalUser | null | undefined,
  role: PortalRole,
): boolean {
  return Boolean(user && resolvePortalRole(user) === role);
}

export function hasPermission(user: PortalUser | null | undefined, permission: string): boolean {
  return Boolean(user && user.permissions.includes(permission));
}

export function hasAnyPermission(user: PortalUser | null | undefined, permissions: string[]): boolean {
  return Boolean(user && permissions.some((permission) => user.permissions.includes(permission)));
}

export function landingForUser(user: PortalUser | null | undefined): string {
  if (!user) return "/login";
  return landingForRole(resolvePortalRole(user));
}
