import { normalizeHex, readableText } from "./color";

export type PublicBranding = {
  systemName: string;
  themeKey: string;
  introduction?: string | null;
  logoUrl?: string | null;
  faviconUrl?: string | null;
  backgroundUrl?: string | null;
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
  customDomain?: string | null;
};

export const defaultBranding: PublicBranding = {
  systemName: "LMSPilot",
  themeKey: "unified-light",
  introduction:
    "Không gian học tập và phát triển dành cho mọi thành viên trong tổ chức.",
  primaryColor: "#2563EB",
  secondaryColor: "#475569",
  backgroundColor: "#F6F7F9",
  textColor: "#172033",
};

function gatewayUrl(): string {
  return (process.env.LMSPILOT_GATEWAY_URL || "http://localhost:8080").replace(
    /\/+$/,
    "",
  );
}

function browserAsset(path?: string | null): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `/api/gateway/${path.replace(/^\/+/, "")}`;
}

export async function getPublicBranding(): Promise<PublicBranding> {
  try {
    const response = await fetch(`${gatewayUrl()}/public/v1/branding`, {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    if (!response.ok) return defaultBranding;
    const data = (await response.json()) as Partial<PublicBranding>;
    return {
      ...defaultBranding,
      ...data,
      primaryColor: normalizeHex(data.primaryColor ?? "", defaultBranding.primaryColor),
      secondaryColor: normalizeHex(data.secondaryColor ?? "", defaultBranding.secondaryColor),
      backgroundColor: normalizeHex(data.backgroundColor ?? "", defaultBranding.backgroundColor),
      logoUrl: browserAsset(data.logoUrl),
      faviconUrl: browserAsset(data.faviconUrl),
      backgroundUrl: browserAsset(data.backgroundUrl),
    };
  } catch {
    return defaultBranding;
  }
}

export function brandingStyle(
  branding: PublicBranding,
): Record<string, string> {
  const primary = normalizeHex(branding.primaryColor, defaultBranding.primaryColor);
  const background = normalizeHex(branding.backgroundColor, defaultBranding.backgroundColor);
  return {
    "--brand-primary": primary,
    "--brand-secondary": normalizeHex(branding.secondaryColor, defaultBranding.secondaryColor),
    "--brand-background": background,
    // Text colours are calculated, never trusted from an arbitrary saved value.
    "--brand-on-primary": readableText(primary),
    "--brand-on-background": readableText(background),
    ...(branding.backgroundUrl
      ? { "--brand-background-image": `url(${branding.backgroundUrl})` }
      : {}),
  };
}
