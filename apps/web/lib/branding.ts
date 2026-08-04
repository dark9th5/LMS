export type PublicBranding = {
  systemName: string;
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
  introduction: "Mỗi khóa học là một cánh cổng. Mỗi thành tựu là một vì sao.",
  primaryColor: "#8B5CF6",
  secondaryColor: "#21D4B4",
  backgroundColor: "#080611",
  textColor: "#F4F1FF",
};

function gatewayUrl(): string {
  return (process.env.LMSPILOT_GATEWAY_URL || "http://localhost:8080").replace(/\/+$/, "");
}

function browserAsset(path?: string | null): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `/api/gateway/${path.replace(/^\/+/, "")}`;
}

export async function getPublicBranding(): Promise<PublicBranding> {
  try {
    const response = await fetch(`${gatewayUrl()}/public/v1/branding`, { cache: "no-store", signal: AbortSignal.timeout(2500) });
    if (!response.ok) return defaultBranding;
    const data = await response.json() as Partial<PublicBranding>;
    return {
      ...defaultBranding,
      ...data,
      logoUrl: browserAsset(data.logoUrl),
      faviconUrl: browserAsset(data.faviconUrl),
      backgroundUrl: browserAsset(data.backgroundUrl),
    };
  } catch {
    return defaultBranding;
  }
}

export function brandingStyle(branding: PublicBranding): Record<string, string> {
  return {
    "--brand-primary": branding.primaryColor,
    "--brand-secondary": branding.secondaryColor,
    "--brand-background": branding.backgroundColor,
    "--brand-text": branding.textColor,
    ...(branding.backgroundUrl ? { "--brand-background-image": `url(${branding.backgroundUrl})` } : {}),
  };
}
