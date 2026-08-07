export type ApiProblem = {
  code?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
};

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly problem?: ApiProblem,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

type CacheEntry = { expiresAt: number; value: unknown };

const responseCache = new Map<string, CacheEntry>();
const requestsInFlight = new Map<string, Promise<unknown>>();
const DEFAULT_GET_TTL_MS = 10_000;
const RETRYABLE_STATUS = new Set([502, 503]);
const NO_CACHE_PATHS = [
  "/api/v1/auth/",
  "/api/v1/exam-sessions/",
  "/api/v1/ai/question-generation-jobs/",
  "/api/v1/ai/local-runtime/pull/",
  "/heartbeat",
  "/events",
];
function isLongRunningRequest(path: string, method: string): boolean {
  const pathname = path.split("?", 1)[0];
  if (method === "POST" && pathname === "/api/v1/ai/local-runtime/pull") return true;
  if (method === "POST" && pathname === "/api/v1/ai/question-generation-jobs") return true;
  if (method === "POST" && pathname === "/api/v1/files") return true;
  if (method === "POST" && /^\/api\/v1\/files\/edit-sessions\/[^/]+\/pdf$/.test(pathname)) return true;
  if (method === "GET" && /^\/api\/v1\/files\/[^/]+\/content$/.test(pathname)) return true;
  return false;
}

function cacheTtl(path: string): number {
  if (NO_CACHE_PATHS.some((item) => path.includes(item))) return 0;
  if (path.includes("/branding") || path.includes("/configuration")) return 30_000;
  if (path.includes("/courses") || path.includes("/exams")) return 15_000;
  return DEFAULT_GET_TTL_MS;
}

function requestTimeout(path: string, method: string): number {
  if (isLongRunningRequest(path, method)) return 300_000;
  if (path.includes("/auth/")) return 25_000;
  return method === "GET" ? 18_000 : 30_000;
}

function cacheKey(path: string, headers: Headers): string {
  return `${path}|${headers.get("Accept-Language") ?? ""}`;
}

function clearApiCache(prefix?: string) {
  if (!prefix) {
    responseCache.clear();
    return;
  }
  for (const key of responseCache.keys()) {
    if (key.startsWith(prefix)) responseCache.delete(key);
  }
}

function invalidateRelated(path: string) {
  const segments = path.split("?")[0].split("/").filter(Boolean);
  const resource = segments.length >= 3 ? `/${segments.slice(0, 3).join("/")}` : undefined;
  if (resource) clearApiCache(resource);
  // Course, assessment and learning screens share derived data. Clear these small groups
  // after a mutation instead of retaining stale cross-service projections.
  if (path.includes("exam") || path.includes("assessment") || path.includes("grade")) {
    clearApiCache("/api/v1/exams");
    clearApiCache("/api/v1/grades");
  }
  if (path.includes("course") || path.includes("learning") || path.includes("enrollment")) {
    clearApiCache("/api/v1/courses");
    clearApiCache("/api/v1/learning");
    clearApiCache("/api/v1/enrollments");
  }
}

function delay(milliseconds: number) {
  return new Promise((resolve) => globalThis.setTimeout(resolve, milliseconds));
}

async function performFetch(
  path: string,
  init: RequestInit,
  headers: Headers,
  attempt: number,
): Promise<Response> {
  const method = (init.method ?? "GET").toUpperCase();
  const controller = new AbortController();
  const timeout = globalThis.setTimeout(
    () => controller.abort("CLIENT_UPSTREAM_TIMEOUT"),
    requestTimeout(path, method),
  );
  const sourceSignal = init.signal;
  const abortFromSource = () => controller.abort(sourceSignal?.reason);
  sourceSignal?.addEventListener("abort", abortFromSource, { once: true });

  try {
    const response = await fetch(`/api/gateway${path}`, {
      ...init,
      method,
      headers,
      credentials: "same-origin",
      signal: controller.signal,
      cache: "no-store",
    });
    if (attempt === 0 && method === "GET" && response.status === 409) {
      const problem = (await response.clone().json().catch(() => undefined)) as ApiProblem | undefined;
      if (problem?.code === "SESSION_REFRESH_IN_PROGRESS") {
        await delay(300 + Math.floor(Math.random() * 150));
        return performFetch(path, init, headers, 1);
      }
    }
    if (
      attempt === 0 &&
      method === "GET" &&
      RETRYABLE_STATUS.has(response.status)
    ) {
      await delay(250 + Math.floor(Math.random() * 150));
      return performFetch(path, init, headers, 1);
    }
    return response;
  } catch (error) {
    // A timeout must fail immediately. Retrying an already slow request doubled the
    // perceived wait from 10 to 20 seconds in earlier builds.
    if (controller.signal.aborted && !sourceSignal?.aborted) {
      throw new ApiRequestError(
        "Dịch vụ phản hồi quá chậm. Vui lòng thử lại sau vài giây.",
        504,
        { code: "CLIENT_UPSTREAM_TIMEOUT" },
      );
    }
    if (
      attempt === 0 &&
      method === "GET" &&
      !sourceSignal?.aborted &&
      error instanceof TypeError
    ) {
      await delay(250 + Math.floor(Math.random() * 150));
      return performFetch(path, init, headers, 1);
    }
    throw error;
  } finally {
    globalThis.clearTimeout(timeout);
    sourceSignal?.removeEventListener("abort", abortFromSource);
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as ApiProblem | undefined;
    if (
      response.status === 401 &&
      typeof window !== "undefined" &&
      ["SESSION_EXPIRED", "EXPIRED_REFRESH_TOKEN", "INVALID_REFRESH_TOKEN", "REFRESH_TOKEN_REUSED"].includes(problem?.code ?? "")
    ) {
      window.location.replace("/login");
    }
    throw new ApiRequestError(
      problem?.message ?? `Yêu cầu thất bại (${response.status})`,
      response.status,
      problem,
    );
  }
  if (response.status === 204) return undefined as T;
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return (await response.text()) as T;
  return (await response.json()) as T;
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const method = (init.method ?? "GET").toUpperCase();
  if (method !== "GET") {
    const value = await parseResponse<T>(await performFetch(path, { ...init, method }, headers, 0));
    invalidateRelated(path);
    return value;
  }

  const ttl = cacheTtl(path);
  const key = cacheKey(path, headers);
  const cached = responseCache.get(key);
  if (ttl > 0 && cached && cached.expiresAt > Date.now()) return cached.value as T;

  const active = requestsInFlight.get(key);
  if (active) return active as Promise<T>;

  const request = (async () => {
    const value = await parseResponse<T>(await performFetch(path, { ...init, method }, headers, 0));
    if (ttl > 0) responseCache.set(key, { value, expiresAt: Date.now() + ttl });
    return value;
  })();

  requestsInFlight.set(key, request);
  void request.finally(() => requestsInFlight.delete(key));
  return request;
}

export function invalidateApiCache(prefix?: string) {
  clearApiCache(prefix);
}

export function unwrapItems<T>(value: T[] | { items?: T[] } | null | undefined): T[] {
  if (Array.isArray(value)) return value;
  return value?.items ?? [];
}

export function createIdempotencyKey(prefix = "web"): string {
  const uuid = globalThis.crypto?.randomUUID?.();
  if (uuid) return `${prefix}-${uuid}`;
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}
