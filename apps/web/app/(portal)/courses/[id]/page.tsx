import { redirect } from "next/navigation";
import { instructorCoursePath } from "@/lib/portal-paths";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";
export const dynamic = "force-dynamic";
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "INSTRUCTOR");
  const { id } = await params;
  redirect(instructorCoursePath(id));
}
