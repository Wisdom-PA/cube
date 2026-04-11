package wisdom.cube.voice;

import java.util.Optional;

/**
 * Outcome of one voice interpretation cycle (Phase 6; no automation execution).
 */
public record VoiceTurnResult(boolean ok, String code, Optional<String> spokenText) {

    public static VoiceTurnResult ok(String spoken) {
        return new VoiceTurnResult(true, "ok", Optional.ofNullable(spoken));
    }

    public static VoiceTurnResult error(String code) {
        return new VoiceTurnResult(false, code, Optional.empty());
    }
}
