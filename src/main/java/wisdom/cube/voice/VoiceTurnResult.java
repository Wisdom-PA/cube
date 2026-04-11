package wisdom.cube.voice;

import java.util.Optional;

/**
 * Outcome of one voice interpretation cycle (Phase 6–7).
 */
public record VoiceTurnResult(boolean ok, String code, Optional<String> spokenText) {

    public static VoiceTurnResult ok(String spoken) {
        return new VoiceTurnResult(true, "ok", Optional.ofNullable(spoken));
    }

    public static VoiceTurnResult error(String code) {
        return new VoiceTurnResult(false, code, Optional.empty());
    }

    /** Error path where TTS already spoke {@code spoken} (e.g. device unreachable). */
    public static VoiceTurnResult error(String code, String spoken) {
        String s = spoken == null ? "" : spoken.trim();
        return new VoiceTurnResult(false, code, s.isEmpty() ? Optional.empty() : Optional.of(s));
    }
}
