package wisdom.cube.device;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Declared feature set for a light (F6.T1.S4). This is internal-only for now; the HTTP contract
 * currently exposes only a normalised state summary.
 */
public final class LightCapabilities {

    private final Set<LightCapability> capabilities;

    public LightCapabilities(Set<LightCapability> capabilities) {
        EnumSet<LightCapability> copy = EnumSet.noneOf(LightCapability.class);
        copy.addAll(Objects.requireNonNullElse(capabilities, Collections.emptySet()));
        this.capabilities = Collections.unmodifiableSet(copy);
    }

    public static LightCapabilities inferFrom(LightDevice device) {
        if (device == null) {
            return new LightCapabilities(null);
        }
        EnumSet<LightCapability> out = EnumSet.noneOf(LightCapability.class);
        out.add(LightCapability.ON_OFF);
        String type = device.type() == null ? "" : device.type().toLowerCase();
        if ("light".equals(type)) {
            out.add(LightCapability.DIMMABLE);
        }
        return new LightCapabilities(out);
    }

    public Set<LightCapability> capabilities() {
        return capabilities;
    }

    public boolean supports(LightCapability capability) {
        return capabilities.contains(capability);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LightCapabilities other && capabilities.equals(other.capabilities);
    }

    @Override
    public int hashCode() {
        return capabilities.hashCode();
    }

    @Override
    public String toString() {
        return "LightCapabilities[capabilities=" + capabilities + ']';
    }
}
