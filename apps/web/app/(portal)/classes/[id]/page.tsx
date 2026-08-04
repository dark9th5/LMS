import { redirect } from "next/navigation";
import { ClassDetail } from "@/components/ClassDetail";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["classes:read", "classes:manage", "live-sessions:join"])) redirect("/learning");
  const { id } = await params;
  return <ClassDetail classId={id} user={user}/>;
}
