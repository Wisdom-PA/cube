package wisdom.cube.internet;

/**
 * Per-voice-turn explicit cloud preference (F5.T2.S1). {@link #UNSET} in paranoid mode means cloud is not allowed.
 */
public enum VoiceCloudConsent {
    UNSET,
    ALLOW,
    DENY
}
