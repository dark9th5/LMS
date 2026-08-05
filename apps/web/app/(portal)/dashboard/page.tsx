import { redirect } from "next/navigation";
import { landingForUser } from "@/lib/authorization";
import { requireAuthenticatedUser } from "@/lib/route-access";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export default async function Page() {
  const user = await requireAuthenticatedUser();
  redirect(landingForUser(user));
}
