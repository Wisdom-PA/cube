package wisdom.cube.voice;

import wisdom.cube.wakeword.WakeWordDetector;

/**
 * Polls wake-word until hit or {@code maxPolls} exhausted (F3.T2 scaffold).
 */
public final class VoiceWakeCoordinator {

    private VoiceWakeCoordinator() {
    }

    public static boolean pollUntilWake(WakeWordDetector wake, int maxPolls) {
        for (int i = 0; i < maxPolls; i++) {
            if (wake.poll()) {
                return true;
            }
        }
        return false;
    }
}
