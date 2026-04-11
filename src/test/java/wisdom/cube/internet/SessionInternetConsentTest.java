package wisdom.cube.internet;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionInternetConsentTest {

    @Test
    void grantThenExpires() {
        AtomicLong clock = new AtomicLong(0L);
        SessionInternetConsent s = new SessionInternetConsent(clock::get);
        s.grantForMillis(5_000L);
        assertTrue(s.isActive());
        clock.set(6_000L);
        assertFalse(s.isActive());
    }

    @Test
    void clearStopsSession() {
        SessionInternetConsent s = new SessionInternetConsent();
        s.grantForMillis(999_999L);
        s.clear();
        assertFalse(s.isActive());
    }
}
