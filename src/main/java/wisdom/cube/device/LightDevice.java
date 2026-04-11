package wisdom.cube.device;

/**
 * Immutable snapshot of a controllable light for API and automation layers (Phase 7).
 */
public record LightDevice(
        String id,
        String name,
        String type,
        String room,
        boolean power,
        double brightness) {
}
