package wisdom.cube.voice;

import java.util.Locale;
import java.util.Optional;
import java.util.function.LongSupplier;

import wisdom.cube.core.AutomationEngine;

/**
 * Short-term follow-up resolution (F4.T3.S5): after a successful light automation, pronoun-style
 * commands within {@link #DEFAULT_MAX_AGE_MS} reuse the last room / intent shape.
 */
public final class VoiceContextChain {

    public static final long DEFAULT_MAX_AGE_MS = 60_000L;

    private final LongSupplier epochMillis;
    private long lastSuccessEpochMs;
    private String lastRoomSlug;
    private String lastParameters;

    public VoiceContextChain() {
        this(System::currentTimeMillis);
    }

    public VoiceContextChain(LongSupplier epochMillis) {
        this.epochMillis = epochMillis;
    }

    public void recordAutomationSuccess(AutomationEngine.Intent intent) {
        lastSuccessEpochMs = epochMillis.getAsLong();
        lastRoomSlug = intent.targets();
        lastParameters = intent.parameters();
    }

    public void clear() {
        lastSuccessEpochMs = 0L;
        lastRoomSlug = null;
        lastParameters = null;
    }

    public boolean isFresh(long maxAgeMs) {
        if (lastRoomSlug == null) {
            return false;
        }
        return epochMillis.getAsLong() - lastSuccessEpochMs <= maxAgeMs;
    }

    /**
     * If the utterance is a follow-up (“turn it off”, “brighter”), map to an intent using context.
     */
    public Optional<AutomationEngine.Intent> resolveFollowUp(String transcript) {
        if (!isFresh(DEFAULT_MAX_AGE_MS)) {
            return Optional.empty();
        }
        String t = transcript.toLowerCase(Locale.ROOT).trim();
        if (t.isEmpty()) {
            return Optional.empty();
        }

        if (mentionsTurnItOnOff(t)) {
            String param = t.contains("off") ? "off" : "on";
            return Optional.of(new AutomationEngine.Intent("set_light", lastRoomSlug, param));
        }

        if (t.contains("brighter") || t.contains("dimmer") || t.contains("dim the") || t.contains("dim ")) {
            double base = parseBrightnessOrDefault(lastParameters);
            double next = (t.contains("dimmer") || t.contains("dim "))
                ? Math.max(0.0, base - 0.2)
                : Math.min(1.0, base + 0.2);
            return Optional.of(new AutomationEngine.Intent(
                "set_brightness",
                lastRoomSlug,
                Double.toString(next)
            ));
        }

        return Optional.empty();
    }

    private static boolean mentionsTurnItOnOff(String t) {
        if (t.contains("turn it off") || t.contains("switch it off")) {
            return true;
        }
        if (t.contains("turn it on") || t.contains("switch it on")) {
            return true;
        }
        return t.contains("turn ") && t.contains(" it ") && (t.contains(" on") || t.contains(" off"));
    }

    private static double parseBrightnessOrDefault(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return 0.5;
        }
        try {
            return Math.max(0.0, Math.min(1.0, Double.parseDouble(parameters.trim())));
        } catch (NumberFormatException ex) {
            return 0.5;
        }
    }
}
