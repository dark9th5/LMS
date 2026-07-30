import { redirect } from "next/navigation";
import { LearningPage } from "@/components/LearningPage";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.roles.includes("STUDENT")) redirect("/dashboard");
  return <LearningPage user={user}/>;
}
