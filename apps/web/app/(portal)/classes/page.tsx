import { redirect } from "next/navigation";
import { ClassesPage } from "@/components/ClassesPage";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.roles.some((role) => role === "ADMIN" || role === "INSTRUCTOR")) redirect("/learning");
  return <ClassesPage user={user}/>;
}
