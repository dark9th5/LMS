import { redirect } from "next/navigation";
import { CourseDetail } from "@/components/CourseDetail";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.roles.some((role) => role === "ADMIN" || role === "INSTRUCTOR")) redirect("/learning");
  const { id } = await params;
  return <CourseDetail courseId={id} user={user}/>;
}
