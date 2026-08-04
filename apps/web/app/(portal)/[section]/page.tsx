import { notFound, redirect } from "next/navigation";
import { SectionPage } from "@/components/SectionPage";
import { getUser } from "@/lib/session";

const CORE_SECTIONS = new Set([
  "users",
  "organization",
  "results",
  "reports",
  "settings",
]);

const RETIRED_SECTIONS = new Set([
  "learning-paths",
  "live-sessions",
  "news",
  "competitions",
  "ai-lab",
  "documents",
  "competencies",
  "certificates",
  "notification-automation",
  "operations",
]);

export default async function Page({ params }: { params: Promise<{ section: string }> }) {
  const user = await getUser();
  if (!user) redirect("/login");
  const { section } = await params;
  if (RETIRED_SECTIONS.has(section)) notFound();
  if (!CORE_SECTIONS.has(section)) notFound();
  return <SectionPage section={section} user={user} />;
}
