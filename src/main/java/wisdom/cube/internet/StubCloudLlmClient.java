package wisdom.cube.internet;

import java.util.Optional;

/**
 * Deterministic cloud stub for CI (F5.T1.S1 placeholder).
 */
public final class StubCloudLlmClient implements CloudLlmClient {

    private final String cannedResponse;

    public StubCloudLlmClient(String cannedResponse) {
        this.cannedResponse = cannedResponse == null ? "" : cannedResponse;
    }

    @Override
    public Optional<String> complete(String sanitizedUserMessage) {
        if (cannedResponse.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(cannedResponse);
    }
}
