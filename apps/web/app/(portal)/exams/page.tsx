import { redirect } from "next/navigation";
import { ExamsPage } from "@/components/ExamsPage";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page() {
  const user = await getUser();
  if (!user) redirect("/login");
  return <ExamsPage user={user}/>;
}
