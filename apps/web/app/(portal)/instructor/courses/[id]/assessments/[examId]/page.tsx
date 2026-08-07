import { ExamDetail } from "@/components/ExamDetail";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page({
  params,
}: {
  params: Promise<{ id: string; examId: string }>;
}) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "INSTRUCTOR");
  const { examId } = await params;
  return <ExamDetail examId={examId} user={user} standaloneOnly={false} />;
}
