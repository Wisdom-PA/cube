package wisdom.cube.internet;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultInternetAccessGateTest {

    @Test
    void globalOfflineBlocks() {
        AtomicReference<VoiceCloudConsent> v = new AtomicReference<>(VoiceCloudConsent.ALLOW);
        DefaultInternetAccessGate g = new DefaultInternetAccessGate(
            () -> true,
            () -> "normal",
            new SessionInternetConsent(),
            new DefaultProfileInternetPolicy(),
            v
        );
        assertFalse(g.allowOnlineLlm("adult-1"));
    }

    @Test
    void paranoidRequiresAllow() {
        AtomicReference<VoiceCloudConsent> v = new AtomicReference<>(VoiceCloudConsent.UNSET);
        DefaultInternetAccessGate g = new DefaultInternetAccessGate(
            () -> false,
            () -> "paranoid",
            new SessionInternetConsent(),
            new DefaultProfileInternetPolicy(),
            v
        );
        assertFalse(g.allowOnlineLlm("adult-1"));
        v.set(VoiceCloudConsent.ALLOW);
        assertTrue(g.allowOnlineLlm("adult-1"));
    }

    @Test
    void sessionConsentOverridesParanoid() {
        AtomicReference<VoiceCloudConsent> v = new AtomicReference<>(VoiceCloudConsent.UNSET);
        SessionInternetConsent s = new SessionInternetConsent(() -> 1_000L);
        s.grantForMillis(60_000L);
        DefaultInternetAccessGate g = new DefaultInternetAccessGate(
            () -> false,
            () -> "paranoid",
            s,
            new DefaultProfileInternetPolicy(),
            v
        );
        assertTrue(g.allowOnlineLlm("adult-1"));
    }

    @Test
    void denyBlocksEvenInNormalMode() {
        AtomicReference<VoiceCloudConsent> v = new AtomicReference<>(VoiceCloudConsent.DENY);
        DefaultInternetAccessGate g = new DefaultInternetAccessGate(
            () -> false,
            () -> "normal",
            new SessionInternetConsent(),
            new DefaultProfileInternetPolicy(),
            v
        );
        assertFalse(g.allowOnlineLlm("adult-1"));
    }
}
