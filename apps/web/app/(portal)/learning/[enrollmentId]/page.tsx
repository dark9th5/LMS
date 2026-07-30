import { redirect } from "next/navigation";
import { LearningPlayer } from "@/components/LearningPlayer";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ enrollmentId: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.roles.includes("STUDENT")) redirect("/dashboard");
  const { enrollmentId } = await params;
  return <LearningPlayer enrollmentId={enrollmentId} user={user}/>;
}
