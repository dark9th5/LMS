import type { CSSProperties, ReactNode } from "react";
import type { Metadata, Viewport } from "next";
import { brandingStyle, getPublicBranding } from "@/lib/branding";
import { getTheme, normalizeThemeKey } from "@/lib/themes";
import "./globals.css";
import "./unified.css";

export async function generateMetadata(): Promise<Metadata> {
  const branding = await getPublicBranding();
  return {
    title: {
      default: `${branding.systemName} · Learning Management`,
      template: `%s · ${branding.systemName}`,
    },
    description:
      branding.introduction ||
      "Nền tảng quản trị học tập, kỳ thi và tri thức nội bộ.",
    icons: { icon: branding.faviconUrl || "/orbit-mark.svg" },
  };
}

export async function generateViewport(): Promise<Viewport> {
  const branding = await getPublicBranding();
  const theme = getTheme(branding.themeKey);
  return {
    colorScheme: theme.mode,
    themeColor: theme.palette.background,
  };
}

export default async function RootLayout({
  children,
}: {
  children: ReactNode;
}) {
  const branding = await getPublicBranding();
  const themeKey = normalizeThemeKey(branding.themeKey);
  return (
    <html lang="vi" data-theme={themeKey} suppressHydrationWarning>
      <body style={brandingStyle(branding) as CSSProperties}>{children}</body>
    </html>
  );
}
