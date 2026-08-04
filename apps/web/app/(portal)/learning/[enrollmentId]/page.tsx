import { redirect } from "next/navigation";
import { LearningPlayer } from "@/components/LearningPlayer";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ enrollmentId: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["courses:learn", "learning:read:self"])) redirect("/dashboard");
  const { enrollmentId } = await params;
  return <LearningPlayer enrollmentId={enrollmentId} user={user}/>;
}
