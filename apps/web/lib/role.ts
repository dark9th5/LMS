import type { PortalUser } from "./types";
import { roleHome } from "./portal-paths";

export type PortalRole = "ADMIN" | "INSTRUCTOR" | "STUDENT";

export const PORTAL_ROLES: readonly PortalRole[] = [
  "ADMIN",
  "INSTRUCTOR",
  "STUDENT",
] as const;

export function isPortalRole(value: unknown): value is PortalRole {
  return typeof value === "string" && PORTAL_ROLES.includes(value as PortalRole);
}

/**
 * Resolve the single product role used by the portal. New installations always
 * return exactly one canonical role. Unknown or malformed role sets never gain
 * elevated access and fall back to the student landing page.
 */
export function resolvePortalRole(
  user: Pick<PortalUser, "roles" | "primaryRole"> | null | undefined,
): PortalRole {
  if (!user) return "STUDENT";
  if (isPortalRole(user.primaryRole)) return user.primaryRole;
  const normalized = (user.roles ?? []).map((role) => role.toUpperCase());
  const found = normalized.find(isPortalRole);
  return found ?? "STUDENT";
}

export function roleLabel(role: PortalRole): string {
  switch (role) {
    case "ADMIN":
      return "Quản trị viên";
    case "INSTRUCTOR":
      return "Giảng viên";
    case "STUDENT":
      return "Học viên";
  }
}

export function landingForRoles(roles: string[] | undefined): string {
  return landingForRole(resolvePortalRole({ roles: roles ?? [] }));
}

export function landingForRole(role: PortalRole): string {
  return roleHome(role);
}

export function landingForSession(user: PortalUser): string {
  return landingForRole(resolvePortalRole(user));
}

const OWNED_PREFIXES: Record<PortalRole, readonly string[]> = {
  ADMIN: ["/admin"],
  INSTRUCTOR: ["/instructor"],
  STUDENT: ["/student"],
};

export function roleOwnsPath(role: PortalRole, path: string): boolean {
  return OWNED_PREFIXES[role].some(
    (prefix) => path === prefix || path.startsWith(`${prefix}/`),
  );
}

export function allowedRolesForPath(path: string): readonly PortalRole[] {
  return PORTAL_ROLES.filter((role) => roleOwnsPath(role, path));
}
