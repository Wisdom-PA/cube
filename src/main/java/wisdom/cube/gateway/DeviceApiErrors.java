package wisdom.cube.gateway;

/**
 * Structured error bodies for device routes (F6.T3); matches contract {@code ApiError}.
 */
public final class DeviceApiErrors {

    private DeviceApiErrors() { }

    public static String deviceNotFoundJson() {
        return "{\"error\":{\"code\":\"DEVICE_NOT_FOUND\",\"message\":\"Unknown device id\"}}";
    }
}
