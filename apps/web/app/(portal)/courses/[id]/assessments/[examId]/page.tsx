import { redirect } from "next/navigation";
import { instructorCourseAssessmentPath } from "@/lib/portal-paths";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";
export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string; examId: string }> }) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "INSTRUCTOR");
  const { id, examId } = await params;
  redirect(instructorCourseAssessmentPath(id, examId));
}
