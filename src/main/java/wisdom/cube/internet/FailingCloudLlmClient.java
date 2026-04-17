package wisdom.cube.internet;

import java.util.Optional;

/** Always fails — for testing on-device fallback (F5.T1.S4). */
public final class FailingCloudLlmClient implements CloudLlmClient {

    @Override
    public Optional<String> complete(String sanitizedUserMessage) {
        return Optional.empty();
    }
}
