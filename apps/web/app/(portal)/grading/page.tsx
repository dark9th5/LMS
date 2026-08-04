import { redirect } from "next/navigation";
import { GradingPage } from "@/components/GradingPage";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  if (!hasAnyPermission(user, ["grading:manage", "assessments:grade"])) redirect("/learning");
  return <GradingPage/>;
}
