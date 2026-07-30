import { redirect } from "next/navigation";
import { SectionPage } from "@/components/SectionPage";
import { getUser } from "@/lib/session";

export default async function Page({ params }: { params: Promise<{ section: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  const { section } = await params;
  return <SectionPage section={section} user={user} />;
}
