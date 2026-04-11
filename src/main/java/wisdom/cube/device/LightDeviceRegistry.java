package wisdom.cube.device;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * In-process registry of lights (mock / dev). Intent targets use slugs such as {@code living_room};
 * stored {@link LightDevice#room()} uses display names (e.g. {@code Living room}).
 */
public interface LightDeviceRegistry {

    /**
     * Stable iteration order (e.g. insertion order) for list responses.
     */
    List<LightDevice> allInOrder();

    Optional<LightDevice> get(String id);

    boolean contains(String id);

    void setPower(String id, boolean on);

    /** Brightness is clamped to {@code [0, 1]}. */
    void setBrightness(String id, double brightness);

    /** Integration / health layer sets whether commands may reach the device (F6.T3). */
    void setReachable(String id, boolean reachable);

    /**
     * First light in the room matching {@code roomSlug} ({@code kitchen} ↔ {@code Kitchen}, etc.).
     */
    Optional<String> firstLightIdInRoom(String roomSlug);

    static boolean roomSlugMatchesDisplay(String roomSlug, String displayRoom) {
        if (roomSlug == null || displayRoom == null) {
            return false;
        }
        String s = roomSlug.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
        String d = displayRoom.toLowerCase(Locale.ROOT).trim();
        return s.equals(d);
    }
}
