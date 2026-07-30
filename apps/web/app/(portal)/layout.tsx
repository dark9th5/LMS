import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { getUser } from "@/lib/session";
export const dynamic = "force-dynamic";
export const revalidate = 0;
export default async function PortalLayout({children}:{children:React.ReactNode}){const user=await getUser();if(!user)redirect("/login");return <PortalShell user={user}>{children}</PortalShell>}
