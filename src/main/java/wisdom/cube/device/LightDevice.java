package wisdom.cube.device;

/**
 * Immutable snapshot of a controllable light for API and automation layers (Phase 7).
 *
 * @param reachable When false, state-changing operations should fail (F6.T3).
 */
public record LightDevice(
        String id,
        String name,
        String type,
        String room,
        boolean power,
        double brightness,
        boolean reachable) {
}
