import { redirect } from "next/navigation";
import { CourseDetail } from "@/components/CourseDetail";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["courses:read", "courses:create", "courses:update", "courses:learn"])) redirect("/learning");
  const { id } = await params;
  return <CourseDetail courseId={id} user={user}/>;
}
