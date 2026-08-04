"use client";

import { usePathname } from "next/navigation";

export default function RouteTemplate({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  return (
    <div key={pathname} className="page-transition">
      {children}
    </div>
  );
}
