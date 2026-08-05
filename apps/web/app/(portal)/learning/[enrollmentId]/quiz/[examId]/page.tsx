import { redirect } from "next/navigation";
import { studentCourseQuizPath } from "@/lib/portal-paths";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";
export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ enrollmentId: string; examId: string }> }) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  const { enrollmentId, examId } = await params;
  redirect(studentCourseQuizPath(enrollmentId, examId));
}
