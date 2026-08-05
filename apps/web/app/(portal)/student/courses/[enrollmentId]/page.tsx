import { LearningPlayer } from "@/components/LearningPlayer";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ enrollmentId: string }> }) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "STUDENT");
  const { enrollmentId } = await params;
  return <LearningPlayer enrollmentId={enrollmentId} user={user} />;
}
