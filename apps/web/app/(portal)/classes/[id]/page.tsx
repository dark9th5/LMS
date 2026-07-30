import { redirect } from "next/navigation";
import { ClassDetail } from "@/components/ClassDetail";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.roles.some((role) => role === "ADMIN" || role === "INSTRUCTOR")) redirect("/learning");
  const { id } = await params;
  return <ClassDetail classId={id} user={user}/>;
}
