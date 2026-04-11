package wisdom.cube.internet;

import wisdom.cube.logging.BehaviourLogSchema;
import wisdom.cube.logging.BehaviourLogWriter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records {@link BehaviourLogSchema.InternetCallEntry} rows for transparency (F5.T3.S2).
 */
public final class InternetCallLogger {

    private static final AtomicInteger NEXT_CALL_INDEX = new AtomicInteger(0);

    private final BehaviourLogWriter writer;
    private final String deviceId;

    public InternetCallLogger(BehaviourLogWriter writer, String deviceId) {
        this.writer = writer;
        this.deviceId = deviceId == null ? "cube" : deviceId;
    }

    public void record(
        UUID chainId,
        String profileId,
        String metadataSummary,
        String serviceCategory,
        String endpoint,
        String result,
        Optional<String> errorCode,
        Optional<String> errorMessage
    ) {
        int idx = NEXT_CALL_INDEX.incrementAndGet();
        writer.writeInternetCall(new BehaviourLogSchema.InternetCallEntry(
            chainId,
            idx,
            Instant.now(),
            deviceId,
            profileId == null ? "" : profileId,
            metadataSummary,
            serviceCategory,
            endpoint,
            result,
            errorCode.orElse(null),
            errorMessage.orElse(null)
        ));
    }
}
