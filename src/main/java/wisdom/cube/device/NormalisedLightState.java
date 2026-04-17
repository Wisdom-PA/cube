package wisdom.cube.device;

/**
 * Normalised light state (F6.T1.S4).
 *
 * @param brightness01 Normalised brightness in [0, 1] when {@link LightCapability#DIMMABLE} is supported.
 */
public record NormalisedLightState(boolean power, double brightness01) {

    public NormalisedLightState {
        brightness01 = clamp01(brightness01);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}

