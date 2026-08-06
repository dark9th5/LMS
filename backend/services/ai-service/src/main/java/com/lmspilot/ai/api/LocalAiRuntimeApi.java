package com.lmspilot.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmspilot.ai.platform.AiProviderConfigEntity;
import com.lmspilot.ai.platform.AiProviderConfigRepository;
import com.lmspilot.ai.platform.AiProviderType;
import com.lmspilot.support.api.ApiException;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

record LocalModelInstallRequest(
    @NotBlank @Size(max = 240) String model,
    @Size(max = 80) String providerCode
) {}

record LocalModelRuntimeResponse(
    boolean available,
    String managementUrl,
    String openAiBaseUrl,
    List<String> models,
    String message
) {}

record LocalModelPullJobResponse(
    UUID id,
    String model,
    String status,
    int progress,
    long completedBytes,
    long totalBytes,
    String message,
    UUID providerId,
    Instant createdAt,
    Instant updatedAt
) {}

final class LocalModelPullJob {
    final UUID id = UUID.randomUUID();
    final String model;
    final String providerCode;
    final Instant createdAt = Instant.now();
    volatile Instant updatedAt = createdAt;
    volatile String status = "QUEUED";
    volatile int progress;
    volatile long completedBytes;
    volatile long totalBytes;
    volatile String message = "Đã xếp hàng tải model";
    volatile UUID providerId;

    LocalModelPullJob(String model, String providerCode) {
        this.model = model;
        this.providerCode = providerCode;
    }

    LocalModelPullJobResponse view() {
        return new LocalModelPullJobResponse(
            id, model, status, progress, completedBytes, totalBytes,
            message, providerId, createdAt, updatedAt
        );
    }
}

@Service
class LocalAiRuntimeService {
    private final ObjectMapper mapper;
    private final AiProviderConfigurationService providers;
    private final AiProviderConfigRepository providerRepository;
    private final HttpClient http;
    private final String managementUrl;
    private final String openAiBaseUrl;
    private final ConcurrentHashMap<UUID, LocalModelPullJob> pulls = new ConcurrentHashMap<>();
    private final ExecutorService pullExecutor = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("ollama-pull-", 0).factory());

    LocalAiRuntimeService(
        ObjectMapper mapper,
        AiProviderConfigurationService providers,
        AiProviderConfigRepository providerRepository,
        @Value("${ai.ollama-management-url:http://ollama:11434}") String managementUrl,
        @Value("${ai.ollama-openai-base-url:http://ollama:11434/v1}") String openAiBaseUrl
    ) {
        this.mapper = mapper;
        this.providers = providers;
        this.providerRepository = providerRepository;
        this.managementUrl = trimSlash(managementUrl);
        this.openAiBaseUrl = trimSlash(openAiBaseUrl);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    }

    LocalModelRuntimeResponse status() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(managementUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(6)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return unavailable("Ollama phản hồi HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            List<String> models = new ArrayList<>();
            root.path("models").forEach(item -> {
                String name = item.path("name").asText("").trim();
                if (!name.isEmpty()) models.add(name);
            });
            return new LocalModelRuntimeResponse(
                true, managementUrl, openAiBaseUrl, List.copyOf(models),
                models.isEmpty() ? "Ollama đã sẵn sàng, chưa có model nào." : "Ollama đang hoạt động."
            );
        } catch (Exception error) {
            return unavailable("Chưa kết nối được Ollama. Hãy kiểm tra container ollama.");
        }
    }

    LocalModelPullJobResponse startInstall(LocalModelInstallRequest input) {
        String model = input.model().trim();
        if (!model.matches("[A-Za-z0-9._:/-]{2,240}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI_MODEL_NAME_INVALID", "Tên model Ollama không hợp lệ");
        }
        LocalModelPullJob existing = pulls.values().stream()
            .filter(job -> job.model.equalsIgnoreCase(model) && (job.status.equals("QUEUED") || job.status.equals("DOWNLOADING")))
            .findFirst().orElse(null);
        if (existing != null) return existing.view();

        String code = input.providerCode() == null || input.providerCode().isBlank()
            ? "LOCAL_" + model.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT)
            : input.providerCode().trim().toUpperCase(Locale.ROOT);
        LocalModelPullJob job = new LocalModelPullJob(model, code);
        pulls.put(job.id, job);
        pullExecutor.submit(() -> pullAndConfigure(job));
        return job.view();
    }

    LocalModelPullJobResponse pullStatus(UUID id) {
        LocalModelPullJob job = pulls.get(id);
        if (job == null) throw new ApiException(HttpStatus.NOT_FOUND, "OLLAMA_PULL_JOB_NOT_FOUND", "Không tìm thấy tác vụ tải model");
        return job.view();
    }

    private void pullAndConfigure(LocalModelPullJob job) {
        job.status = "DOWNLOADING";
        job.message = "Đang tải model từ Ollama Library";
        job.updatedAt = Instant.now();
        try {
            String body = mapper.writeValueAsString(Map.of("name", job.model, "stream", true));
            HttpRequest request = HttpRequest.newBuilder(URI.create(managementUrl + "/api/pull"))
                .timeout(Duration.ofHours(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<java.io.InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama phản hồi HTTP " + response.statusCode());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode update = mapper.readTree(line);
                    if (update.hasNonNull("error")) throw new IllegalStateException(update.path("error").asText());
                    long total = update.path("total").asLong(job.totalBytes);
                    long completed = update.path("completed").asLong(job.completedBytes);
                    String status = update.path("status").asText(job.message);
                    job.totalBytes = Math.max(job.totalBytes, total);
                    job.completedBytes = Math.max(job.completedBytes, completed);
                    if (job.totalBytes > 0) job.progress = Math.min(99, (int) Math.round(job.completedBytes * 100.0 / job.totalBytes));
                    if (!status.isBlank()) job.message = status;
                    job.updatedAt = Instant.now();
                }
            }
            job.status = "CONFIGURING";
            job.progress = 99;
            job.message = "Đang tự thiết lập kết nối AI";
            job.updatedAt = Instant.now();
            AiProviderResponse provider = providers.save(null, new AiProviderRequest(
                job.providerCode, AiProviderType.LOCAL_OPENAI_COMPATIBLE,
                openAiBaseUrl, job.model, true, "", 180, 4096,
                Map.of("runtime", "OLLAMA", "managed", true, "managementUrl", managementUrl)
            ));
            job.providerId = provider.id();
            job.status = "COMPLETED";
            job.progress = 100;
            job.message = "Đã tải model và bật kết nối AI";
            job.updatedAt = Instant.now();
        } catch (Exception error) {
            job.status = "FAILED";
            job.message = error.getMessage() == null ? "Không thể tải model" : error.getMessage();
            job.updatedAt = Instant.now();
        }
    }

    Map<String, Object> testProvider(UUID id) {
        AiProviderConfigEntity provider = providerRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AI_PROVIDER_NOT_FOUND", "Không tìm thấy cấu hình AI"));
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(trimSlash(provider.baseUrl) + "/models"))
                .timeout(Duration.ofSeconds(Math.min(20, Math.max(5, provider.requestTimeoutSeconds)))).GET();
            String key = providers.apiKey(provider);
            if (key != null && !key.isBlank()) builder.header("Authorization", "Bearer " + key);
            long started = System.nanoTime();
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long latencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_TEST_FAILED", "Endpoint AI phản hồi HTTP " + response.statusCode());
            }
            return Map.of("ok", true, "providerId", id, "model", provider.model, "latencyMs", latencyMs, "message", "Kết nối AI hoạt động tốt");
        } catch (ApiException error) {
            throw error;
        } catch (Exception error) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_UNREACHABLE", "Không thể kết nối endpoint AI");
        }
    }

    private LocalModelRuntimeResponse unavailable(String message) {
        return new LocalModelRuntimeResponse(false, managementUrl, openAiBaseUrl, List.of(), message);
    }

    private static String trimSlash(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }

    @PreDestroy
    void shutdown() {
        pullExecutor.shutdownNow();
    }
}

@RestController
@RequestMapping("/api/v1/ai")
public class LocalAiRuntimeApi {
    private final LocalAiRuntimeService runtime;

    public LocalAiRuntimeApi(LocalAiRuntimeService runtime) {
        this.runtime = runtime;
    }

    @GetMapping("/local-runtime")
    @PreAuthorize("hasAnyAuthority('configuration:manage','integrations:manage')")
    public LocalModelRuntimeResponse status() {
        return runtime.status();
    }

    @PostMapping("/local-runtime/pull")
    @PreAuthorize("hasAnyAuthority('configuration:manage','integrations:manage')")
    public ResponseEntity<LocalModelPullJobResponse> pull(@Valid @RequestBody LocalModelInstallRequest input) {
        return ResponseEntity.accepted().body(runtime.startInstall(input));
    }

    @GetMapping("/local-runtime/pull/{jobId}")
    @PreAuthorize("hasAnyAuthority('configuration:manage','integrations:manage')")
    public LocalModelPullJobResponse pullStatus(@PathVariable UUID jobId) {
        return runtime.pullStatus(jobId);
    }

    @PostMapping("/providers/{id}/test")
    @PreAuthorize("hasAnyAuthority('configuration:manage','integrations:manage')")
    public Map<String, Object> test(@PathVariable UUID id) {
        return runtime.testProvider(id);
    }
}
