package wisdom.cube.internet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultProfileInternetPolicyTest {

    private final DefaultProfileInternetPolicy p = new DefaultProfileInternetPolicy();

    @Test
    void nonBlankProfileAllowed() {
        assertTrue(p.profileAllowsInternet("adult-1"));
    }

    @Test
    void blankProfileDenied() {
        assertFalse(p.profileAllowsInternet(""));
        assertFalse(p.profileAllowsInternet("   "));
        assertFalse(p.profileAllowsInternet(null));
    }
}
