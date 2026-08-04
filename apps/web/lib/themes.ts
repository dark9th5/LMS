export const THEME_CATEGORIES = {
  all: "Tất cả",
  business: "Doanh nghiệp",
  education: "Giáo dục",
  institution: "Tổ chức",
  creative: "Sáng tạo",
  minimal: "Tối giản",
} as const;

export type ThemeCategory = Exclude<keyof typeof THEME_CATEGORIES, "all">;
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

export const THEMES = [
  {
    key: "soft-spectrum",
    name: "Sắc màu Cân bằng",
    shortName: "Spectrum",
    description:
      "Tươi sáng, giàu năng lượng nhưng đã tiết chế độ bão hòa; phù hợp làm giao diện mặc định cho doanh nghiệp và trường học.",
    mode: "light",
    category: "business",
    tags: ["Tươi sáng", "Cân bằng", "Phổ dụng"],
    palette: {
      primary: "#B95547",
      secondary: "#5967B8",
      background: "#F6F3EF",
      surface: "#FFFDF9",
      text: "#20232E",
      muted: "#707383",
    },
  },
  {
    key: "executive-midnight",
    name: "Điều hành Cao cấp",
    shortName: "Executive",
    description:
      "Navy đậm, điểm nhấn champagne và bề mặt tinh gọn dành cho lãnh đạo hoặc thương hiệu cao cấp.",
    mode: "dark",
    category: "business",
    tags: ["Cao cấp", "Lãnh đạo", "Trang trọng"],
    palette: {
      primary: "#D2B478",
      secondary: "#6F8EF6",
      background: "#0C1018",
      surface: "#151B26",
      text: "#F7F4ED",
      muted: "#9CA5B5",
    },
  },
  {
    key: "heritage-academy",
    name: "Học viện Di sản",
    shortName: "Heritage",
    description:
      "Giấy ấm, burgundy và kiểu chữ serif cổ điển cho đại học, học viện hoặc trường có lịch sử lâu đời.",
    mode: "light",
    category: "education",
    tags: ["Cổ điển", "Đại học", "Học thuật"],
    palette: {
      primary: "#7A1F3D",
      secondary: "#A47A3C",
      background: "#F3EBDD",
      surface: "#FFFDF8",
      text: "#2D241F",
      muted: "#75685D",
    },
  },
  {
    key: "bright-school",
    name: "Trường học Năng động",
    shortName: "School",
    description:
      "Hình khối thân thiện, màu vui nhưng có trật tự; phù hợp trường phổ thông và môi trường học tập trẻ.",
    mode: "light",
    category: "education",
    tags: ["Thân thiện", "Trẻ trung", "Năng động"],
    palette: {
      primary: "#F05252",
      secondary: "#3867D6",
      background: "#FFF8EA",
      surface: "#FFFFFF",
      text: "#24304A",
      muted: "#6D7482",
    },
  },
  {
    key: "civic-trust",
    name: "Tổ chức Tin cậy",
    shortName: "Civic",
    description:
      "Xanh công vụ, bố cục chắc chắn và tương phản rõ cho cơ quan, tổ chức xã hội hoặc đơn vị quy mô lớn.",
    mode: "light",
    category: "institution",
    tags: ["Tổ chức", "Công vụ", "Tin cậy"],
    palette: {
      primary: "#0B6E99",
      secondary: "#2F7D62",
      background: "#EEF4F5",
      surface: "#FFFFFF",
      text: "#17313A",
      muted: "#677D84",
    },
  },
  {
    key: "creative-pop",
    name: "Xưởng Sáng tạo",
    shortName: "Creative",
    description:
      "Tím, hồng và nhịp editorial táo bạo cho agency, truyền thông, thiết kế hoặc cộng đồng sáng tạo.",
    mode: "light",
    category: "creative",
    tags: ["Sáng tạo", "Rực rỡ", "Editorial"],
    palette: {
      primary: "#7138D0",
      secondary: "#E5488F",
      background: "#FBF7FF",
      surface: "#FFFFFF",
      text: "#241A35",
      muted: "#746C80",
    },
  },
  {
    key: "nature-learning",
    name: "Giáo dục Xanh",
    shortName: "Nature",
    description:
      "Sage, đất nung và đường nét hữu cơ cho đào tạo bền vững, sức khỏe, cộng đồng hoặc phi lợi nhuận.",
    mode: "light",
    category: "education",
    tags: ["Tự nhiên", "Bền vững", "Nhẹ nhàng"],
    palette: {
      primary: "#397A52",
      secondary: "#B86F35",
      background: "#F2F4E9",
      surface: "#FCFFF9",
      text: "#243528",
      muted: "#6E796F",
    },
  },
  {
    key: "editorial-burgundy",
    name: "Tạp chí Cổ điển",
    shortName: "Editorial",
    description:
      "Bố cục kiểu tạp chí, tiêu đề serif và màu rượu vang cho viện nghiên cứu, xuất bản hoặc thương hiệu văn hóa.",
    mode: "light",
    category: "creative",
    tags: ["Tạp chí", "Văn hóa", "Thanh lịch"],
    palette: {
      primary: "#9A2454",
      secondary: "#C56B24",
      background: "#F6F1EC",
      surface: "#FFFBF8",
      text: "#2B2024",
      muted: "#776A70",
    },
  },
  {
    key: "minimal-calm",
    name: "Tối giản An nhiên",
    shortName: "Minimal",
    description:
      "Trắng xám, ít bóng đổ và gần như không trang trí; ưu tiên tập trung, tốc độ đọc và thời gian làm việc dài.",
    mode: "light",
    category: "minimal",
    tags: ["Tối giản", "Điềm tĩnh", "Dễ đọc"],
    palette: {
      primary: "#334155",
      secondary: "#8B7E74",
      background: "#F5F5F3",
      surface: "#FFFFFF",
      text: "#1C1D20",
      muted: "#777A80",
    },
  },
  {
    key: "digital-grid",
    name: "Trung tâm Công nghệ",
    shortName: "Digital",
    description:
      "Nền tối, lưới kỹ thuật, góc vuông và chữ mono cho công ty công nghệ, đội IT hoặc trung tâm vận hành.",
    mode: "dark",
    category: "minimal",
    tags: ["Công nghệ", "Kỹ thuật", "Dữ liệu"],
    palette: {
      primary: "#22D3B6",
      secondary: "#60A5FA",
      background: "#071011",
      surface: "#0D1B1D",
      text: "#E6FFFC",
      muted: "#83A8A5",
    },
  },
] as const satisfies readonly ThemeDefinition[];

export type ThemeKey = (typeof THEMES)[number]["key"];

export const DEFAULT_THEME_KEY: ThemeKey = "soft-spectrum";

const THEME_KEY_SET = new Set<string>(THEMES.map((theme) => theme.key));

export function normalizeThemeKey(value?: string | null): ThemeKey {
  return THEME_KEY_SET.has(value ?? "")
    ? (value as ThemeKey)
    : DEFAULT_THEME_KEY;
}

export function getTheme(value?: string | null): ThemeDefinition {
  const key = normalizeThemeKey(value);
  return THEMES.find((theme) => theme.key === key) ?? THEMES[0];
}
