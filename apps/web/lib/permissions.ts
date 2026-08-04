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
  accountType: "SYSTEM_ADMIN" | "USER";
  permissions: ReadonlySet<string>;
  scopedPermissions?: readonly ScopedPermission[];
};

export function can(
  auth: SessionAuthorization,
  permission: string,
  scope?: { type: ScopeType; id?: string },
): boolean {
  if (auth.accountType === "SYSTEM_ADMIN") return true;

  const relevant = (auth.scopedPermissions ?? []).filter((grant) => {
    if (grant.permission !== permission) return false;
    if (grant.scopeType === "SYSTEM") return true;
    return scope != null && grant.scopeType === scope.type && grant.scopeId === scope.id;
  });

  if (relevant.some((grant) => grant.effect === "DENY")) return false;
  return auth.permissions.has(permission) || relevant.some((grant) => grant.effect === "ALLOW");
}

/** UI navigation must be permission-based; never branch on role code. */
export const navigationRules = {
  administration: ["users:read", "roles:read", "organization:read"],
  courseAuthoring: ["courses:create", "courses:update"],
  learning: ["courses:learn"],
  exams: ["assessments:take", "exams:manage"],
  reports: ["reports:read:self", "reports:read:scope"],
  newsManagement: ["news:manage"],
  branding: ["branding:manage"],
} as const;
