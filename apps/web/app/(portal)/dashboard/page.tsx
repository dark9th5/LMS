import { redirect } from "next/navigation";
import { Dashboard } from "@/components/Dashboard";
import { getUser } from "@/lib/session";
export const dynamic = "force-dynamic";
export const revalidate = 0;
export default async function Page(){const user=await getUser();if(!user)redirect("/login");if(user.roles.includes("STUDENT")&&!user.roles.includes("ADMIN")&&!user.roles.includes("INSTRUCTOR"))redirect("/learning");return <Dashboard user={user}/>;}
