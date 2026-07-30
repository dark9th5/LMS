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
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`/api/gateway${path}`, {
    ...init,
    headers,
    cache: "no-store",
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => undefined)) as ApiProblem | undefined;
    if (response.status === 401 && typeof window !== "undefined") {
      window.location.replace("/login");
    }
    throw new ApiRequestError(problem?.message ?? `Yêu cầu thất bại (${response.status})`, response.status, problem);
  }
  if (response.status === 204) return undefined as T;
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return (await response.text()) as T;
  return (await response.json()) as T;
}

export function unwrapItems<T>(value: T[] | { items?: T[] } | null | undefined): T[] {
  if (Array.isArray(value)) return value;
  return value?.items ?? [];
}

export function createIdempotencyKey(prefix = "web"): string {
  const uuid = globalThis.crypto?.randomUUID?.();
  if (uuid) return `${prefix}-${uuid}`;
  // randomUUID may be unavailable when the portal is opened from a plain HTTP LAN IP.
  // The key only needs request uniqueness; it is not used as an authentication secret.
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}
