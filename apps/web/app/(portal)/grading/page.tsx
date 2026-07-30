import { redirect } from "next/navigation";
import { GradingPage } from "@/components/GradingPage";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!user.permissions.includes("grading:manage")) redirect(user.roles.includes("STUDENT") ? "/learning" : "/dashboard");
  return <GradingPage/>;
}
