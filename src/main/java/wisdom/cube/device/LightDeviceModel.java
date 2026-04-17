package wisdom.cube.device;

/**
 * Uniform light abstraction: immutable device info + capabilities + normalised state (F6.T1.S4).
 */
public record LightDeviceModel(LightDevice device, LightCapabilities capabilities, NormalisedLightState state) {

    public static LightDeviceModel from(LightDevice device) {
        LightCapabilities caps = LightCapabilities.inferFrom(device);
        NormalisedLightState st = device == null
            ? new NormalisedLightState(false, 0.0)
            : new NormalisedLightState(device.power(), device.brightness());
        return new LightDeviceModel(device, caps, st);
    }
}

