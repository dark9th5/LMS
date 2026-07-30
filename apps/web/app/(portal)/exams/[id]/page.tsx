import { redirect } from "next/navigation";
import { ExamDetail } from "@/components/ExamDetail";
import { getUser } from "@/lib/session";

export const dynamic = "force-dynamic";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  const { id } = await params;
  return <ExamDetail examId={id} user={user}/>;
}
