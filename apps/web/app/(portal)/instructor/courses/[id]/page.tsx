import { CourseDetail } from "@/components/CourseDetail";
import { requireAuthenticatedUser, requireRole } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await requireAuthenticatedUser();
  requireRole(user, "INSTRUCTOR");
  const { id } = await params;
  return <CourseDetail courseId={id} user={user} />;
}
