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
        boolean reachable;

        Mutable(String id, String name, String type, String room, boolean power, double brightness, boolean reachable) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.room = room;
            this.power = power;
            this.brightness = brightness;
            this.reachable = reachable;
        }

        LightDevice snapshot() {
            return new LightDevice(id, name, type, room, power, brightness, reachable);
        }
    }

    private final Map<String, Mutable> byId = new LinkedHashMap<>();

    public InMemoryLightDeviceRegistry() {
        putDefault("light-1", "Living room light", "light", "Living room", true, 1.0);
        putDefault("light-2", "Kitchen light", "light", "Kitchen", false, 1.0);
    }

    private void putDefault(String id, String name, String type, String room, boolean power, double brightness) {
        byId.put(id, new Mutable(id, name, type, room, power, brightness, true));
    }

    /** Marks every device reachable or not (e.g. after a discovery / health sweep). */
    public synchronized void refreshReachabilityAll(boolean reachable) {
        for (Mutable m : byId.values()) {
            m.reachable = reachable;
        }
    }

    @Override
    public synchronized List<LightDevice> allInOrder() {
        List<LightDevice> out = new ArrayList<>(byId.size());
        for (Mutable m : byId.values()) {
            out.add(m.snapshot());
        }
        return List.copyOf(out);
    }

    @Override
    public synchronized Optional<LightDevice> get(String id) {
        Mutable m = byId.get(id);
        return m == null ? Optional.empty() : Optional.of(m.snapshot());
    }

    @Override
    public synchronized boolean contains(String id) {
        return id != null && byId.containsKey(id);
    }

    @Override
    public synchronized void setPower(String id, boolean on) {
        Mutable m = byId.get(id);
        if (m != null) {
            m.power = on;
        }
    }

    @Override
    public synchronized void setBrightness(String id, double brightness) {
        Mutable m = byId.get(id);
        if (m != null) {
            m.brightness = clampUnit(brightness);
        }
    }

    @Override
    public synchronized void setReachable(String id, boolean reachable) {
        Mutable m = byId.get(id);
        if (m != null) {
            m.reachable = reachable;
        }
    }

    @Override
    public synchronized Optional<String> firstLightIdInRoom(String roomSlug) {
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
