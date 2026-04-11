package wisdom.cube.device;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dev/mock light store shared by the HTTP device API and {@link wisdom.cube.core.AutomationEngine}.
 */
public final class InMemoryLightDeviceRegistry implements LightDeviceRegistry {

    private static final class Mutable {
        final String id;
        final String name;
        final String type;
        final String room;
        boolean power;
        double brightness;

        Mutable(String id, String name, String type, String room, boolean power, double brightness) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.room = room;
            this.power = power;
            this.brightness = brightness;
        }

        LightDevice snapshot() {
            return new LightDevice(id, name, type, room, power, brightness);
        }
    }

    private final Map<String, Mutable> byId = new LinkedHashMap<>();

    public InMemoryLightDeviceRegistry() {
        putDefault("light-1", "Living room light", "light", "Living room", true, 1.0);
        putDefault("light-2", "Kitchen light", "light", "Kitchen", false, 1.0);
    }

    private void putDefault(String id, String name, String type, String room, boolean power, double brightness) {
        byId.put(id, new Mutable(id, name, type, room, power, brightness));
    }

    @Override
    public List<LightDevice> allInOrder() {
        List<LightDevice> out = new ArrayList<>(byId.size());
        for (Mutable m : byId.values()) {
            out.add(m.snapshot());
        }
        return List.copyOf(out);
    }

    @Override
    public Optional<LightDevice> get(String id) {
        Mutable m = byId.get(id);
        return m == null ? Optional.empty() : Optional.of(m.snapshot());
    }

    @Override
    public boolean contains(String id) {
        return id != null && byId.containsKey(id);
    }

    @Override
    public void setPower(String id, boolean on) {
        Mutable m = byId.get(id);
        if (m != null) {
            m.power = on;
        }
    }

    @Override
    public void setBrightness(String id, double brightness) {
        Mutable m = byId.get(id);
        if (m != null) {
            m.brightness = clampUnit(brightness);
        }
    }

    @Override
    public Optional<String> firstLightIdInRoom(String roomSlug) {
        if (roomSlug == null || roomSlug.isBlank()) {
            return Optional.empty();
        }
        for (Mutable m : byId.values()) {
            if (!"light".equalsIgnoreCase(m.type)) {
                continue;
            }
            if (LightDeviceRegistry.roomSlugMatchesDisplay(roomSlug, m.room)) {
                return Optional.of(m.id);
            }
        }
        return Optional.empty();
    }

    private static double clampUnit(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
