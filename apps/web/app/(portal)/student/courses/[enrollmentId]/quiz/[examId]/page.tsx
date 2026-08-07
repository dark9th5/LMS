import { ExamDetail } from "@/components/ExamDetail";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page({
  params,
}: {
  params: Promise<{ enrollmentId: string; examId: string }>;
}) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  const { enrollmentId, examId } = await params;
  return (
    <ExamDetail
      examId={examId}
      user={user}
      standaloneOnly={false}
      enrollmentIdOverride={enrollmentId}
    />
  );
}
