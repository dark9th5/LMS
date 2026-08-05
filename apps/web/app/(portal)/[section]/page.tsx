import { notFound, redirect } from "next/navigation";
import { resolvePortalRole } from "@/lib/role";
import { PORTAL_PATHS } from "@/lib/portal-paths";
import { getUser } from "@/lib/session";

const LEGACY_TARGETS = {
  users: PORTAL_PATHS.ADMIN.users,
  organization: PORTAL_PATHS.ADMIN.organization,
  settings: PORTAL_PATHS.ADMIN.settings,
  results: PORTAL_PATHS.STUDENT.results,
  certificates: PORTAL_PATHS.STUDENT.certificates,
} as const;

export default async function Page({ params }: { params: Promise<{ section: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  const { section } = await params;
  const role = resolvePortalRole(user);
  if (section === "reports") {
    if (role === "ADMIN") redirect(PORTAL_PATHS.ADMIN.reports);
    if (role === "INSTRUCTOR") redirect(PORTAL_PATHS.INSTRUCTOR.reports);
    redirect(PORTAL_PATHS.STUDENT.home);
  }
  const target = LEGACY_TARGETS[section as keyof typeof LEGACY_TARGETS];
  if (!target) notFound();
  if (section === "users" || section === "organization" || section === "settings") {
    if (role !== "ADMIN") redirect(PORTAL_PATHS[role].home);
  } else if (role !== "STUDENT") {
    redirect(PORTAL_PATHS[role].home);
  }
  redirect(target);
}
