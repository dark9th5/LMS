import type { PortalRole } from "./role";

export type ScopeType =
  | "SYSTEM"
  | "BRANCH"
  | "DEPARTMENT"
  | "GROUP"
  | "COURSE"
  | "EXAM";

export type ScopedPermission = {
  permission: string;
  scopeType: ScopeType;
  scopeId?: string;
  effect: "ALLOW" | "DENY";
};

export type SessionAuthorization = {
  role: PortalRole;
  permissions: ReadonlySet<string>;
  scopedPermissions?: readonly ScopedPermission[];
};

export function can(
  auth: SessionAuthorization,
  permission: string,
  scope?: { type: ScopeType; id?: string },
): boolean {
  const relevant = (auth.scopedPermissions ?? []).filter((grant) => {
    if (grant.permission !== permission) return false;
    if (grant.scopeType === "SYSTEM") return true;
    return scope != null && grant.scopeType === scope.type && grant.scopeId === scope.id;
  });

  if (relevant.some((grant) => grant.effect === "DENY")) return false;
  return auth.permissions.has(permission) || relevant.some((grant) => grant.effect === "ALLOW");
}

/** Product navigation is role-owned; permissions only narrow actions inside that role. */
export const navigationRules = {
  ADMIN: ["users:read", "organization:read", "reports:read:scope", "configuration:manage"],
  INSTRUCTOR: ["courses:create", "assessments:create", "assessments:grade", "reports:read:scope"],
  STUDENT: ["courses:learn", "assessments:take", "grades:read:self", "certificates:read:self"],
} as const;
