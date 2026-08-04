export function normalizeHex(value: string, fallback: string): string {
  const candidate = value.trim();
  return /^#[0-9a-f]{6}$/i.test(candidate) ? candidate.toUpperCase() : fallback;
}

function relativeLuminance(hex: string): number {
  const channels = [1, 3, 5].map(
    (index) => parseInt(hex.slice(index, index + 2), 16) / 255,
  );
  const linear = channels.map((value) =>
    value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4,
  );
  return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
}

export function readableText(background: string): "#FFFFFF" | "#111827" {
  const safe = normalizeHex(background, "#2563EB");
  const luminance = relativeLuminance(safe);
  const whiteContrast = 1.05 / (luminance + 0.05);
  const darkContrast = (luminance + 0.05) / 0.057;
  return whiteContrast >= darkContrast ? "#FFFFFF" : "#111827";
}
