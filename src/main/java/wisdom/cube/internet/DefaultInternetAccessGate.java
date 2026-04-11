package wisdom.cube.internet;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Enforces {@code global_offline}, {@code default_privacy_mode}, session consent, profile policy, and voice turn consent.
 */
public final class DefaultInternetAccessGate implements InternetAccessGate {

    private final Supplier<Boolean> globalOffline;
    private final Supplier<String> defaultPrivacyMode;
    private final SessionInternetConsent sessionConsent;
    private final ProfileInternetPolicy profilePolicy;
    private final AtomicReference<VoiceCloudConsent> voiceConsent;

    public DefaultInternetAccessGate(
        Supplier<Boolean> globalOffline,
        Supplier<String> defaultPrivacyMode,
        SessionInternetConsent sessionConsent,
        ProfileInternetPolicy profilePolicy,
        AtomicReference<VoiceCloudConsent> voiceConsent
    ) {
        this.globalOffline = globalOffline;
        this.defaultPrivacyMode = defaultPrivacyMode;
        this.sessionConsent = sessionConsent;
        this.profilePolicy = profilePolicy;
        this.voiceConsent = voiceConsent;
    }

    @Override
    public boolean allowOnlineLlm(String profileId) {
        if (Boolean.TRUE.equals(globalOffline.get())) {
            return false;
        }
        if (!profilePolicy.profileAllowsInternet(profileId)) {
            return false;
        }
        if (sessionConsent.isActive()) {
            return true;
        }
        VoiceCloudConsent v = voiceConsent.get();
        if (v == VoiceCloudConsent.DENY) {
            return false;
        }
        String mode = defaultPrivacyMode.get();
        if (mode != null && "paranoid".equalsIgnoreCase(mode.trim())) {
            return v == VoiceCloudConsent.ALLOW;
        }
        return true;
    }
}
