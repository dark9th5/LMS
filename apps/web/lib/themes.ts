export const THEME_CATEGORIES = {
  all: "Tất cả",
  minimal: "Thống nhất",
} as const;

export type ThemeCategory = "minimal";
export type ThemeMode = "dark" | "light";

export type ThemePalette = {
  primary: string;
  secondary: string;
  background: string;
  surface: string;
  text: string;
  muted: string;
};

export type ThemeDefinition = {
  key: string;
  name: string;
  shortName: string;
  description: string;
  mode: ThemeMode;
  category: ThemeCategory;
  tags: string[];
  palette: ThemePalette;
};

/**
 * LMS Unified Design System.
 * Light and dark use identical typography, spacing, components and layouts.
 * Only the semantic colour tokens change.
 */
export const THEMES = [
  {
    key: "unified-light",
    name: "Giao diện sáng",
    shortName: "Sáng",
    description:
      "Nền sáng trung tính, độ tương phản cao và mật độ thoải mái cho công việc hằng ngày.",
    mode: "light",
    category: "minimal",
    tags: ["Dễ đọc", "Thống nhất", "Mặc định"],
    palette: {
      primary: "#2563EB",
      secondary: "#475569",
      background: "#F6F7F9",
      surface: "#FFFFFF",
      text: "#172033",
      muted: "#5F6B7A",
    },
  },
  {
    key: "unified-dark",
    name: "Giao diện tối",
    shortName: "Tối",
    description:
      "Nền tối trung tính, bề mặt phân lớp rõ và cùng một cấu trúc với chế độ sáng.",
    mode: "dark",
    category: "minimal",
    tags: ["Dễ đọc", "Ít chói", "Thống nhất"],
    palette: {
      primary: "#7CB4FF",
      secondary: "#A8B3C2",
      background: "#10151D",
      surface: "#18212D",
      text: "#F3F6FA",
      muted: "#A8B3C2",
    },
  },
] as const satisfies readonly ThemeDefinition[];

export type ThemeKey = (typeof THEMES)[number]["key"];
export const DEFAULT_THEME_KEY: ThemeKey = "unified-light";

const LEGACY_DARK = new Set([
  "executive-midnight",
  "digital-grid",
  "unified-dark",
]);

export function normalizeThemeKey(value?: string | null): ThemeKey {
  if (value === "unified-light" || value === "unified-dark") return value;
  return LEGACY_DARK.has(value ?? "") ? "unified-dark" : "unified-light";
}

export function getTheme(value?: string | null): ThemeDefinition {
  const key = normalizeThemeKey(value);
  return THEMES.find((theme) => theme.key === key) ?? THEMES[0];
}
