import type { CSSProperties, ReactNode } from "react";
import type { Metadata, Viewport } from "next";
import { brandingStyle, getPublicBranding } from "@/lib/branding";
import "./globals.css";
import "./astral-v3.css";

export async function generateMetadata(): Promise<Metadata> {
  const branding = await getPublicBranding();
  return {
    title: {
      default: `${branding.systemName} · Học viện Huyền Tri`,
      template: `%s · ${branding.systemName}`,
    },
    description:
      branding.introduction ||
      "Nền tảng quản trị học tập, kỳ thi và tri thức nội bộ.",
    icons: { icon: branding.faviconUrl || "/mystic-mark.svg" },
  };
}

export const viewport: Viewport = {
  colorScheme: "dark",
  themeColor: "#080611",
};

export default async function RootLayout({
  children,
}: {
  children: ReactNode;
}) {
  const branding = await getPublicBranding();
  return (
    <html lang="vi" suppressHydrationWarning>
      <body style={brandingStyle(branding) as CSSProperties}>{children}</body>
    </html>
  );
}
