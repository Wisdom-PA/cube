package wisdom.cube.internet;

import wisdom.cube.core.LlmService;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Tries cloud when {@link InternetAccessGate} allows; otherwise or on failure uses on-device LLM (F5.T1.S4).
 */
public final class CloudFallbackLlmService implements LlmService {

    private final LlmService onDevice;
    private final Optional<CloudLlmClient> cloudClient;
    private final InternetAccessGate gate;
    private final String profileId;
    private final Optional<InternetCallLogger> callLogger;
    private final Supplier<UUID> chainIdSupplier;

    public CloudFallbackLlmService(
        LlmService onDevice,
        Optional<CloudLlmClient> cloudClient,
        InternetAccessGate gate,
        String profileId,
        Optional<InternetCallLogger> callLogger,
        Supplier<UUID> chainIdSupplier
    ) {
        this.onDevice = onDevice;
        this.cloudClient = cloudClient == null ? Optional.empty() : cloudClient;
        this.gate = gate;
        this.profileId = profileId;
        this.callLogger = callLogger == null ? Optional.empty() : callLogger;
        this.chainIdSupplier = chainIdSupplier == null ? UUID::randomUUID : chainIdSupplier;
    }

    @Override
    public Optional<String> complete(String prompt) {
        if (cloudClient.isEmpty()) {
            return onDevice.complete(prompt);
        }
        if (!gate.allowOnlineLlm(profileId)) {
            return onDevice.complete(prompt);
        }
        String sanitized = CloudLlmPrompts.cloudUserMessage(prompt);
        UUID chainId = chainIdSupplier.get();
        Optional<String> cloud = cloudClient.get().complete(sanitized);
        if (cloud.isPresent() && !cloud.get().isBlank()) {
            callLogger.ifPresent(log -> log.record(
                chainId,
                profileId,
                "cloud_llm completion",
                "cloud_llm",
                "api.claude.example",
                "ok",
                Optional.empty(),
                Optional.empty()
            ));
            return cloud;
        }
        callLogger.ifPresent(log -> log.record(
            chainId,
            profileId,
            "cloud_llm completion failed",
            "cloud_llm",
            "api.claude.example",
            "error",
            Optional.of("CLOUD_EMPTY"),
            Optional.of("empty or failed response")
        ));
        return onDevice.complete(prompt);
    }
}
