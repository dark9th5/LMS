import { redirect } from "next/navigation";
import { LearningPage } from "@/components/LearningPage";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["courses:learn", "learning:read:self"])) redirect("/dashboard");
  return <LearningPage user={user}/>;
}
