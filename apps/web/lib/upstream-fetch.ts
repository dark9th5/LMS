import "server-only";

function positiveInteger(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function gatewayBaseUrl(): string {
  const configured = process.env.LMSPILOT_GATEWAY_URL?.trim();
  if (configured) return configured.replace(/\/+$/, "");
  return process.env.NODE_ENV === "production"
    ? "http://api-gateway:8080"
    : "http://127.0.0.1:8080";
}

const defaultTimeoutMs = positiveInteger(process.env.LMSPILOT_UPSTREAM_TIMEOUT_MS, 7_000);
const mutationTimeoutMs = positiveInteger(process.env.LMSPILOT_UPSTREAM_MUTATION_TIMEOUT_MS, 12_000);
const aiTimeoutMs = positiveInteger(process.env.LMSPILOT_AI_UPSTREAM_TIMEOUT_MS, 300_000);

function timeoutFor(target: string, method: string): number {
  if (
    target.includes("/ai/local-runtime/pull") ||
    target.includes("/ai/question-generation-jobs") ||
    target.includes("/files/upload")
  ) return aiTimeoutMs;
  if (target.includes("/auth/")) return Math.max(defaultTimeoutMs, 15_000);
  return ["GET", "HEAD"].includes(method) ? defaultTimeoutMs : mutationTimeoutMs;
}

export async function fetchGateway(
  pathOrUrl: string | URL,
  init: RequestInit,
): Promise<Response> {
  const raw = typeof pathOrUrl === "string" ? pathOrUrl : pathOrUrl.toString();
  const target = raw.startsWith("http://") || raw.startsWith("https://")
    ? raw
    : `${gatewayBaseUrl()}${raw.startsWith("/") ? raw : `/${raw}`}`;
  const method = (init.method ?? "GET").toUpperCase();

  const controller = new AbortController();
  const timer = setTimeout(
    () => controller.abort("SERVER_UPSTREAM_TIMEOUT"),
    timeoutFor(target, method),
  );
  const sourceSignal = init.signal;
  const abortFromSource = () => controller.abort(sourceSignal?.reason);
  sourceSignal?.addEventListener("abort", abortFromSource, { once: true });

  try {
    return await fetch(target, {
      ...init,
      method,
      signal: controller.signal,
      cache: "no-store",
    });
  } finally {
    clearTimeout(timer);
    sourceSignal?.removeEventListener("abort", abortFromSource);
  }
}
