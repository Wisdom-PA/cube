package wisdom.cube.wakeword;

/**
 * Fires once after {@code pollsBeforeFire} calls to {@link #poll()} (deterministic tests).
 */
public final class StubWakeWordDetector implements WakeWordDetector {

    private int pollsBeforeFire;
    private boolean fired;

    public StubWakeWordDetector(int pollsBeforeFire) {
        if (pollsBeforeFire < 0) {
            throw new IllegalArgumentException("pollsBeforeFire must be >= 0");
        }
        this.pollsBeforeFire = pollsBeforeFire;
    }

    @Override
    public boolean poll() {
        if (fired) {
            return false;
        }
        if (pollsBeforeFire > 0) {
            pollsBeforeFire--;
            return false;
        }
        fired = true;
        return true;
    }

    public void reset(int pollsBeforeFire) {
        this.pollsBeforeFire = pollsBeforeFire;
        this.fired = false;
    }
}
