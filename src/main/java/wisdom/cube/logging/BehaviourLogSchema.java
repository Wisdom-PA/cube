package wisdom.cube.logging;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Types aligned with Plan §4 behaviour log schema.
 * All entries grouped by chain_id; one timestamp per intent and per action.
 */
public final class BehaviourLogSchema {

    private BehaviourLogSchema() {}

    /** Chain summary: one row per chain (F8.T1.S1). */
    public record ChainSummary(
        UUID chainId,
        String deviceId,
        Instant chainStartTs,
        Optional<Instant> chainEndTs,
        String initialProfileId,
        Instant identifiedAtTs,
        String identifiedProfileId,
        List<PrivacyModeChange> privacyModeChanges
    ) {}

    /** Privacy mode switch within a chain. */
    public record PrivacyModeChange(
        Instant atTs,
        String fromMode,
        String toMode,
        String trigger
    ) {}

    /** Intent entry: user utterance and parsed intent (no audio). */
    public record IntentEntry(
        UUID chainId,
        int intentIndex,
        Instant ts,
        String utteranceText,
        String type,
        String targets,
        String parameters,
        String profileId
    ) {}

    /** Action entry: what the system did, before/after state, result. */
    public record ActionEntry(
        UUID chainId,
        int actionIndex,
        int intentIndex,
        Instant ts,
        String beforeAfterSummary,
        String result,
        String errorCode,
        String errorMessage
    ) {}

    /** Internet call: external request metadata (no PII). */
    public record InternetCallEntry(
        UUID chainId,
        int callIndex,
        Instant ts,
        String deviceId,
        String profileId,
        String metadataSummary,
        String serviceCategory,
        String endpoint,
        String result,
        String errorCode,
        String errorMessage
    ) {}
}
