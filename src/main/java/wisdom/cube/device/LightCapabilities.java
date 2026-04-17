package wisdom.cube.device;

import java.util.EnumSet;
import java.util.Set;

/**
 * Declared feature set for a light (F6.T1.S4). This is internal-only for now; the HTTP contract
 * currently exposes only a normalised state summary.
 */
public record LightCapabilities(Set<LightCapability> capabilities) {

    public LightCapabilities {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    public static LightCapabilities inferFrom(LightDevice device) {
        if (device == null) {
            return new LightCapabilities(Set.of());
        }
        EnumSet<LightCapability> out = EnumSet.of(LightCapability.ON_OFF);
        String type = device.type() == null ? "" : device.type().toLowerCase();
        if ("light".equals(type)) {
            out.add(LightCapability.DIMMABLE);
        }
        return new LightCapabilities(out);
    }

    public boolean supports(LightCapability capability) {
        return capabilities.contains(capability);
    }
}

