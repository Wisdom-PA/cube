package wisdom.cube.audio;

/**
 * Policy for whether raw audio may ever be persisted (F3.T5.S4). Production default is no retention.
 */
public record AudioPrivacySettings(boolean allowRawPersistence) {

    /**
     * On-device assistant default: raw PCM must not be written to disk or sent off-device.
     */
    public static AudioPrivacySettings productionDefault() {
        return new AudioPrivacySettings(false);
    }
}
