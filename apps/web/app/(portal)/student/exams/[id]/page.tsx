import { ExamDetail } from "@/components/ExamDetail";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  const { id } = await params;
  return <ExamDetail examId={id} user={user} standaloneOnly />;
}
