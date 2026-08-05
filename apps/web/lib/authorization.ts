import type { PortalUser } from "./types";

export function isSystemAdmin(user: PortalUser | null | undefined): boolean {
  return user?.accountType === "SYSTEM_ADMIN";
}

export function hasPermission(user: PortalUser | null | undefined, permission: string): boolean {
  return Boolean(user && (isSystemAdmin(user) || user.permissions.includes(permission)));
}

export function hasAnyPermission(user: PortalUser | null | undefined, permissions: string[]): boolean {
  return Boolean(user && (isSystemAdmin(user) || permissions.some((permission) => user.permissions.includes(permission))));
}

export function landingForUser(user: PortalUser | null | undefined): string {
  if (!user) return "/login";
  if (hasAnyPermission(user, [
    "reports:read:scope",
    "reports:kpi:read",
    "courses:create",
    "courses:update",
    "classes:manage",
    "users:read",
  ])) return "/dashboard";
  if (hasAnyPermission(user, ["courses:learn", "learning:read:self"])) return "/learning";
  if (hasAnyPermission(user, ["assessments:take", "assessment:take", "assessments:read"])) return "/exams";
  if (hasAnyPermission(user, ["grades:read:self", "reports:read:self"])) return "/results";
  // Every authenticated account receives a safe, non-retired landing page.
  return "/dashboard";
}
