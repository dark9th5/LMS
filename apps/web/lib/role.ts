export function landingForRoles(roles: string[] | undefined): string {
  if (!roles?.length) return "/dashboard";
  if (roles.includes("ADMIN")) return "/dashboard";
  if (roles.includes("INSTRUCTOR")) return "/dashboard";
  return "/learning";
}
