import { redirect } from "next/navigation";
import { Dashboard } from "@/components/Dashboard";
import { getUser } from "@/lib/session";
import { hasAnyPermission } from "@/lib/authorization";
export const dynamic = "force-dynamic";
export const revalidate = 0;
export default async function Page(){const user=await getUser();if(!user)redirect("/login");if(!hasAnyPermission(user,["reports:read:scope","courses:create","classes:manage","users:read"]))redirect("/learning");return <Dashboard user={user}/>;}
