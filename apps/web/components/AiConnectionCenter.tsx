"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest } from "@/lib/api";
import { Icon } from "./Icon";

type ProviderType =
  "LOCAL_OPENAI_COMPATIBLE" | "REMOTE_OPENAI_COMPATIBLE" | "CUSTOM_ADAPTER";

type AiProvider = {
  id: string;
  code: string;
  providerType: ProviderType;
  baseUrl: string;
  model: string;
  enabled: boolean;
  apiKeyConfigured: boolean;
  requestTimeoutSeconds: number;
  maxOutputTokens?: number | null;
  config?: Record<string, unknown>;
  updatedAt: string;
};

type LocalRuntime = {
  available: boolean;
  managementUrl: string;
  openAiBaseUrl: string;
  models: string[];
  message: string;
};

type LocalPullJob = {
  id: string;
  model: string;
  status: "QUEUED" | "DOWNLOADING" | "CONFIGURING" | "COMPLETED" | "FAILED";
  progress: number;
  completedBytes: number;
  totalBytes: number;
  message: string;
  providerId?: string | null;
};

type ProviderDraft = {
  id?: string;
  code: string;
  providerType: ProviderType;
  baseUrl: string;
  model: string;
  enabled: boolean;
  apiKey: string;
  requestTimeoutSeconds: number;
  maxOutputTokens: number;
};

const QUICK_MODELS = [
  {
    model: "qwen3:4b",
    title: "Qwen3 4B",
    hint: "Nhẹ hơn, phù hợp máy 16 GB RAM và thử nghiệm nhanh.",
    badge: "Nhẹ",
  },
  {
    model: "qwen3:8b",
    title: "Qwen3 8B",
    hint: "Cân bằng chất lượng và tài nguyên cho sinh câu hỏi tiếng Việt.",
    badge: "Khuyên dùng",
  },
  {
    model: "llama3.1:8b",
    title: "Llama 3.1 8B",
    hint: "Lựa chọn tương thích rộng cho nội dung đa ngôn ngữ.",
    badge: "Phổ biến",
  },
];

function emptyDraft(
  type: ProviderType = "LOCAL_OPENAI_COMPATIBLE",
): ProviderDraft {
  return {
    code: type === "LOCAL_OPENAI_COMPATIBLE" ? "LOCAL_OLLAMA" : "REMOTE_AI",
    providerType: type,
    baseUrl: type === "LOCAL_OPENAI_COMPATIBLE" ? "http://ollama:11434/v1" : "",
    model: type === "LOCAL_OPENAI_COMPATIBLE" ? "qwen3:8b" : "",
    enabled: true,
    apiKey: "",
    requestTimeoutSeconds: 180,
    maxOutputTokens: 4096,
  };
}

function providerTypeLabel(type: ProviderType) {
  if (type === "LOCAL_OPENAI_COMPATIBLE") return "AI local";
  if (type === "REMOTE_OPENAI_COMPATIBLE") return "API riêng";
  return "Adapter tùy chỉnh";
}

export function AiConnectionCenter() {
  const [providers, setProviders] = useState<AiProvider[]>([]);
  const [runtime, setRuntime] = useState<LocalRuntime | null>(null);
  const [mode, setMode] = useState<"quick" | "local" | "remote">("quick");
  const [draft, setDraft] = useState<ProviderDraft>(emptyDraft());
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState("");
  const [pullJob, setPullJob] = useState<LocalPullJob | null>(null);
  const [message, setMessage] = useState<{
    tone: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [providerData, runtimeData] = await Promise.all([
        apiRequest<AiProvider[]>("/api/v1/ai/providers"),
        apiRequest<LocalRuntime>("/api/v1/ai/local-runtime").catch(() => null),
      ]);
      setProviders(providerData);
      setRuntime(runtimeData);
    } catch (error) {
      setMessage({
        tone: "error",
        text:
          error instanceof Error ? error.message : "Không thể tải cấu hình AI",
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const stored = window.localStorage.getItem("lmspilot.ai.pullJob");
    if (!stored) return;
    try {
      const pending = JSON.parse(stored) as { id?: string; model?: string };
      if (pending.id && pending.model)
        void resumePull(pending.id, pending.model);
    } catch {
      window.localStorage.removeItem("lmspilot.ai.pullJob");
    }
  }, [load]);

  const enabledProvider = useMemo(
    () => providers.find((item) => item.enabled),
    [providers],
  );

  async function monitorPull(job: LocalPullJob, model: string) {
    const deadline = Date.now() + 2 * 60 * 60 * 1000;
    let current = job;
    setPullJob(current);
    while (!["COMPLETED", "FAILED"].includes(current.status)) {
      if (Date.now() > deadline)
        throw new Error("Tải model quá thời gian cho phép.");
      setMessage({ tone: "info", text: current.message });
      await new Promise((resolve) => window.setTimeout(resolve, 1_200));
      current = await apiRequest<LocalPullJob>(
        `/api/v1/ai/local-runtime/pull/${current.id}`,
      );
      setPullJob(current);
    }
    if (current.status === "FAILED")
      throw new Error(current.message || "Không thể tải model");
    window.localStorage.removeItem("lmspilot.ai.pullJob");
    setMessage({
      tone: "success",
      text: `Đã tải ${model} và tự động bật kết nối AI local.`,
    });
    setPullJob(null);
    await load();
  }

  async function resumePull(id: string, model: string) {
    setWorking(`pull:${model}`);
    setMessage({ tone: "info", text: `Đang tiếp tục theo dõi tải ${model}…` });
    try {
      const job = await apiRequest<LocalPullJob>(
        `/api/v1/ai/local-runtime/pull/${id}`,
      );
      await monitorPull(job, model);
    } catch (error) {
      window.localStorage.removeItem("lmspilot.ai.pullJob");
      setPullJob(null);
      setMessage({
        tone: "error",
        text:
          error instanceof Error
            ? error.message
            : "Không thể tiếp tục tác vụ tải model",
      });
    } finally {
      setWorking("");
    }
  }

  async function install(model: string) {
    setWorking(`pull:${model}`);
    setMessage({ tone: "info", text: `Đang khởi tạo tải ${model}…` });
    try {
      const job = await apiRequest<LocalPullJob>(
        "/api/v1/ai/local-runtime/pull",
        {
          method: "POST",
          body: JSON.stringify({ model }),
        },
      );
      window.localStorage.setItem(
        "lmspilot.ai.pullJob",
        JSON.stringify({ id: job.id, model }),
      );
      await monitorPull(job, model);
    } catch (error) {
      window.localStorage.removeItem("lmspilot.ai.pullJob");
      setPullJob(null);
      setMessage({
        tone: "error",
        text: error instanceof Error ? error.message : "Không thể tải model",
      });
    } finally {
      setWorking("");
    }
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    setWorking("save");
    setMessage(null);
    try {
      const payload = {
        code: draft.code.trim().toUpperCase(),
        providerType: draft.providerType,
        baseUrl: draft.baseUrl.trim().replace(/\/+$/, ""),
        model: draft.model.trim(),
        enabled: draft.enabled,
        apiKey: draft.apiKey,
        requestTimeoutSeconds: draft.requestTimeoutSeconds,
        maxOutputTokens: draft.maxOutputTokens,
        config: {
          source:
            draft.providerType === "LOCAL_OPENAI_COMPATIBLE"
              ? "ADMIN_LOCAL_FORM"
              : "ADMIN_REMOTE_FORM",
        },
      };
      const saved = await apiRequest<AiProvider>(
        draft.id ? `/api/v1/ai/providers/${draft.id}` : "/api/v1/ai/providers",
        { method: draft.id ? "PUT" : "POST", body: JSON.stringify(payload) },
      );
      setMessage({ tone: "success", text: `Đã lưu kết nối ${saved.code}.` });
      setDraft(emptyDraft(draft.providerType));
      await load();
    } catch (error) {
      setMessage({
        tone: "error",
        text:
          error instanceof Error ? error.message : "Không thể lưu kết nối AI",
      });
    } finally {
      setWorking("");
    }
  }

  async function testProvider(provider: AiProvider) {
    setWorking(`test:${provider.id}`);
    setMessage(null);
    try {
      const result = await apiRequest<{ latencyMs?: number; message?: string }>(
        `/api/v1/ai/providers/${provider.id}/test`,
        { method: "POST" },
      );
      setMessage({
        tone: "success",
        text: `${result.message ?? "Kết nối thành công"}${result.latencyMs !== undefined ? ` · ${result.latencyMs} ms` : ""}`,
      });
    } catch (error) {
      setMessage({
        tone: "error",
        text:
          error instanceof Error
            ? error.message
            : "Kết nối AI không thành công",
      });
    } finally {
      setWorking("");
    }
  }

  function edit(provider: AiProvider) {
    const nextMode =
      provider.providerType === "LOCAL_OPENAI_COMPATIBLE" ? "local" : "remote";
    setMode(nextMode);
    setDraft({
      id: provider.id,
      code: provider.code,
      providerType: provider.providerType,
      baseUrl: provider.baseUrl,
      model: provider.model,
      enabled: provider.enabled,
      apiKey: "",
      requestTimeoutSeconds: provider.requestTimeoutSeconds,
      maxOutputTokens: provider.maxOutputTokens ?? 4096,
    });
    setMessage(null);
  }

  return (
    <div className="ai-admin-center">
      <section className="ai-status-banner">
        <span
          className={`ai-status-light ${enabledProvider ? "online" : ""}`}
        />
        <div>
          <small>TRUNG TÂM AI</small>
          <h2>
            {enabledProvider
              ? `${enabledProvider.model} đã sẵn sàng`
              : "Chưa có model AI đang bật"}
          </h2>
          <p>
            {enabledProvider
              ? `${providerTypeLabel(enabledProvider.providerType)} · ${enabledProvider.baseUrl}`
              : "Cài model local mẫu hoặc kết nối endpoint OpenAI-compatible của riêng bạn."}
          </p>
        </div>
        <button
          type="button"
          className="workspace-button secondary"
          onClick={() => void load()}
          disabled={loading}
        >
          <Icon name="refresh" size={16} /> Kiểm tra lại
        </button>
      </section>

      {message && (
        <div className={`ai-admin-message ${message.tone}`} role="status">
          <Icon
            name={
              message.tone === "error"
                ? "warning"
                : message.tone === "success"
                  ? "check"
                  : "question"
            }
            size={18}
          />
          <span>{message.text}</span>
          <button
            type="button"
            onClick={() => setMessage(null)}
            aria-label="Đóng"
          >
            <Icon name="close" size={16} />
          </button>
        </div>
      )}
      {pullJob && (
        <div
          className="ai-pull-progress"
          aria-label={`Tiến độ tải ${pullJob.model}`}
        >
          <div>
            <strong>{pullJob.model}</strong>
            <span>{pullJob.progress}%</span>
          </div>
          <div className="progress-track">
            <span style={{ width: `${pullJob.progress}%` }} />
          </div>
          <small>{pullJob.message}</small>
        </div>
      )}

      <nav className="ai-mode-tabs" aria-label="Kiểu kết nối AI">
        <button
          type="button"
          className={mode === "quick" ? "active" : ""}
          onClick={() => setMode("quick")}
        >
          <Icon name="download" size={17} />
          Cài nhanh model local
        </button>
        <button
          type="button"
          className={mode === "local" ? "active" : ""}
          onClick={() => {
            setMode("local");
            setDraft(emptyDraft("LOCAL_OPENAI_COMPATIBLE"));
          }}
        >
          <Icon name="operations" size={17} />
          Kết nối AI local có sẵn
        </button>
        <button
          type="button"
          className={mode === "remote" ? "active" : ""}
          onClick={() => {
            setMode("remote");
            setDraft(emptyDraft("REMOTE_OPENAI_COMPATIBLE"));
          }}
        >
          <Icon name="link" size={17} />
          Kết nối bằng API key
        </button>
      </nav>

      {mode === "quick" && (
        <div className="ai-quick-layout">
          <section className="ai-runtime-card">
            <header>
              <span
                className={`runtime-icon ${runtime?.available ? "ready" : ""}`}
              >
                <Icon name="operations" />
              </span>
              <div>
                <h3>Ollama trong Docker</h3>
                <p>{runtime?.message ?? "Đang kiểm tra runtime local…"}</p>
              </div>
              <span
                className={`runtime-badge ${runtime?.available ? "ready" : ""}`}
              >
                {runtime?.available ? "Sẵn sàng" : "Chưa chạy"}
              </span>
            </header>
            <dl>
              <div>
                <dt>Management URL</dt>
                <dd>{runtime?.managementUrl ?? "http://ollama:11434"}</dd>
              </div>
              <div>
                <dt>OpenAI endpoint</dt>
                <dd>{runtime?.openAiBaseUrl ?? "http://ollama:11434/v1"}</dd>
              </div>
              <div>
                <dt>Model đã có</dt>
                <dd>{runtime?.models.length ?? 0}</dd>
              </div>
            </dl>
            {!runtime?.available && (
              <div className="ai-runtime-help">
                <Icon name="warning" size={17} />
                <p>
                  Khởi động bằng{" "}
                  <code>
                    docker compose up -d ollama ai-service api-gateway web
                  </code>
                  , sau đó quay lại nhấn “Kiểm tra lại”.
                </p>
              </div>
            )}
          </section>

          <section className="ai-model-grid">
            {QUICK_MODELS.map((item) => {
              const installed = runtime?.models.some(
                (model) =>
                  model === item.model || model.startsWith(`${item.model}:`),
              );
              const active = providers.some(
                (provider) => provider.model === item.model && provider.enabled,
              );
              return (
                <article
                  className={`ai-model-card ${active ? "active" : ""}`}
                  key={item.model}
                >
                  <header>
                    <span className="ai-model-mark">AI</span>
                    <span className="ai-model-badge">
                      {active ? "Đang dùng" : installed ? "Đã tải" : item.badge}
                    </span>
                  </header>
                  <h3>{item.title}</h3>
                  <code>{item.model}</code>
                  <p>{item.hint}</p>
                  <button
                    type="button"
                    className="workspace-button primary"
                    disabled={!runtime?.available || Boolean(working)}
                    onClick={() => void install(item.model)}
                  >
                    <Icon name={installed ? "check" : "download"} size={16} />
                    {working === `pull:${item.model}`
                      ? "Đang tải model…"
                      : installed
                        ? "Thiết lập và sử dụng"
                        : "Tải và tự thiết lập"}
                  </button>
                </article>
              );
            })}
          </section>
        </div>
      )}

      {(mode === "local" || mode === "remote") && (
        <div className="ai-provider-form-layout">
          <form className="ai-provider-form" onSubmit={save}>
            <header>
              <div>
                <small>
                  {mode === "local"
                    ? "AI LOCAL CÓ SẴN"
                    : "OPENAI-COMPATIBLE API"}
                </small>
                <h2>{draft.id ? "Chỉnh sửa kết nối" : "Tạo kết nối mới"}</h2>
                <p>
                  {mode === "local"
                    ? "Dùng Ollama, LM Studio, vLLM hoặc server model trong mạng nội bộ."
                    : "Dùng API key riêng từ OpenAI hoặc nhà cung cấp tương thích."}
                </p>
              </div>
            </header>
            <div className="ai-form-grid">
              <label>
                <span>Tên cấu hình</span>
                <input
                  required
                  value={draft.code}
                  onChange={(event) =>
                    setDraft({ ...draft, code: event.target.value })
                  }
                  placeholder="LOCAL_OLLAMA"
                />
              </label>
              <label>
                <span>Tên model</span>
                <input
                  required
                  value={draft.model}
                  onChange={(event) =>
                    setDraft({ ...draft, model: event.target.value })
                  }
                  placeholder={mode === "local" ? "qwen3:8b" : "gpt-4.1-mini"}
                />
              </label>
              <label className="wide">
                <span>Base URL</span>
                <input
                  required
                  type="url"
                  value={draft.baseUrl}
                  onChange={(event) =>
                    setDraft({ ...draft, baseUrl: event.target.value })
                  }
                  placeholder={
                    mode === "local"
                      ? "http://host.docker.internal:11434/v1"
                      : "https://api.openai.com/v1"
                  }
                />
                <small>
                  Không nhập phần <code>/chat/completions</code>.
                </small>
              </label>
              {mode === "remote" && (
                <label className="wide">
                  <span>API key</span>
                  <input
                    type="password"
                    autoComplete="new-password"
                    value={draft.apiKey}
                    onChange={(event) =>
                      setDraft({ ...draft, apiKey: event.target.value })
                    }
                    placeholder={
                      draft.id ? "Để trống để giữ key hiện tại" : "sk-…"
                    }
                  />
                  <small>
                    Key được mã hóa ở backend và không trả lại trình duyệt.
                  </small>
                </label>
              )}
              <label>
                <span>Timeout</span>
                <div className="input-with-unit">
                  <input
                    type="number"
                    min={5}
                    max={3600}
                    value={draft.requestTimeoutSeconds}
                    onChange={(event) =>
                      setDraft({
                        ...draft,
                        requestTimeoutSeconds: Number(event.target.value),
                      })
                    }
                  />
                  <b>giây</b>
                </div>
              </label>
              <label>
                <span>Output token tối đa</span>
                <input
                  type="number"
                  min={256}
                  max={65536}
                  value={draft.maxOutputTokens}
                  onChange={(event) =>
                    setDraft({
                      ...draft,
                      maxOutputTokens: Number(event.target.value),
                    })
                  }
                />
              </label>
            </div>
            <label className="workspace-check">
              <input
                type="checkbox"
                checked={draft.enabled}
                onChange={(event) =>
                  setDraft({ ...draft, enabled: event.target.checked })
                }
              />
              <span>Bật kết nối ngay sau khi lưu</span>
            </label>
            <div className="ai-form-actions">
              <button
                type="button"
                className="workspace-button secondary"
                onClick={() => setDraft(emptyDraft(draft.providerType))}
              >
                Đặt lại
              </button>
              <button
                className="workspace-button primary"
                disabled={Boolean(working)}
              >
                <Icon name="save" size={16} />
                {working === "save" ? "Đang lưu…" : "Lưu kết nối"}
              </button>
            </div>
          </form>

          <aside className="ai-connection-guide">
            <h3>Kiểm tra trước khi lưu</h3>
            <ol>
              <li>
                <span>1</span>
                <p>
                  Endpoint phải truy cập được từ container{" "}
                  <code>ai-service</code>.
                </p>
              </li>
              <li>
                <span>2</span>
                <p>
                  Model phải tồn tại tại nhà cung cấp và hỗ trợ chat completion.
                </p>
              </li>
              <li>
                <span>3</span>
                <p>
                  Ưu tiên API trả JSON để bộ kiểm tra câu hỏi hoạt động ổn định.
                </p>
              </li>
            </ol>
            <div className="ai-security-note">
              <Icon name="lock" size={18} />
              <p>
                Không lưu API key trong frontend, Git hoặc file ảnh chụp màn
                hình.
              </p>
            </div>
          </aside>
        </div>
      )}

      <section className="configured-ai-list">
        <header>
          <div>
            <h2>Kết nối đã cấu hình</h2>
            <p>
              {providers.length} kết nối ·{" "}
              {providers.filter((item) => item.enabled).length} đang bật
            </p>
          </div>
        </header>
        {loading ? (
          <div className="ai-list-loading">Đang tải cấu hình…</div>
        ) : providers.length ? (
          <div className="ai-provider-list">
            {providers.map((provider) => (
              <article key={provider.id}>
                <span
                  className={`provider-state ${provider.enabled ? "online" : ""}`}
                />
                <div>
                  <strong>{provider.code}</strong>
                  <p>{provider.model}</p>
                  <small>
                    {providerTypeLabel(provider.providerType)} ·{" "}
                    {provider.baseUrl}
                  </small>
                </div>
                <span className="provider-secret">
                  <Icon
                    name={provider.apiKeyConfigured ? "lock" : "operations"}
                    size={15}
                  />
                  {provider.apiKeyConfigured ? "Có API key" : "Không cần key"}
                </span>
                <div className="provider-actions">
                  <button type="button" onClick={() => edit(provider)}>
                    Sửa
                  </button>
                  <button
                    type="button"
                    disabled={Boolean(working)}
                    onClick={() => void testProvider(provider)}
                  >
                    {working === `test:${provider.id}`
                      ? "Đang thử…"
                      : "Kiểm tra"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="ai-empty-provider">
            <Icon name="operations" size={28} />
            <strong>Chưa có kết nối AI</strong>
            <p>Chọn một cách kết nối phía trên để bắt đầu.</p>
          </div>
        )}
      </section>
    </div>
  );
}
