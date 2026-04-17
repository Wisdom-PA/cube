package wisdom.cube.internet;

import java.util.function.LongSupplier;

/**
 * Session-level internet allow (F5.T2.S2). After {@link #grantForMillis(long)}, {@link #isActive()} is true until the deadline.
 */
public final class SessionInternetConsent {

    private final LongSupplier epochMillis;
    private volatile long deadlineEpochMs;

    public SessionInternetConsent(LongSupplier epochMillis) {
        this.epochMillis = epochMillis;
    }

    public SessionInternetConsent() {
        this(System::currentTimeMillis);
    }

    public void grantForMillis(long millis) {
        long now = epochMillis.getAsLong();
        deadlineEpochMs = now + Math.max(0L, millis);
    }

    public boolean isActive() {
        if (deadlineEpochMs <= 0L) {
            return false;
        }
        return epochMillis.getAsLong() <= deadlineEpochMs;
    }

    public void clear() {
        deadlineEpochMs = 0L;
    }
}
