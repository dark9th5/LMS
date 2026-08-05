import { redirect } from "next/navigation";
import { studentCoursePath } from "@/lib/portal-paths";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";
export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ enrollmentId: string }> }) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  const { enrollmentId } = await params;
  redirect(studentCoursePath(enrollmentId));
}
