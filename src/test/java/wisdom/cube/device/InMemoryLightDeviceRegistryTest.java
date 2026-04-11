package wisdom.cube.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class InMemoryLightDeviceRegistryTest {

    @Test
    void defaultFixturesAndOrder() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        assertEquals(2, r.allInOrder().size());
        assertEquals("light-1", r.allInOrder().get(0).id());
        assertEquals("Living room", r.allInOrder().get(0).room());
    }

    @Test
    void roomSlugMatchesDisplay_normalizesUnderscoreAndCase() {
        assertTrue(LightDeviceRegistry.roomSlugMatchesDisplay("living_room", "Living room"));
        assertTrue(LightDeviceRegistry.roomSlugMatchesDisplay("KITCHEN", "kitchen"));
        assertFalse(LightDeviceRegistry.roomSlugMatchesDisplay("bedroom", "Living room"));
    }

    @Test
    void firstLightIdInRoom_findsBySlug() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        assertEquals(Optional.of("light-1"), r.firstLightIdInRoom("living_room"));
        assertEquals(Optional.of("light-2"), r.firstLightIdInRoom("kitchen"));
    }

    @Test
    void firstLightIdInRoom_emptyForUnknownOrBlank() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        assertEquals(Optional.empty(), r.firstLightIdInRoom("attic"));
        assertEquals(Optional.empty(), r.firstLightIdInRoom(""));
        assertEquals(Optional.empty(), r.firstLightIdInRoom(null));
    }

    @Test
    void setPowerAndBrightness() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        r.setPower("light-2", true);
        r.setBrightness("light-2", 0.25);
        LightDevice d = r.get("light-2").orElseThrow();
        assertTrue(d.power());
        assertEquals(0.25, d.brightness(), 1e-9);
    }

    @Test
    void setBrightnessClamps() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        r.setBrightness("light-1", 99);
        assertEquals(1.0, r.get("light-1").orElseThrow().brightness(), 1e-9);
        r.setBrightness("light-1", -3);
        assertEquals(0.0, r.get("light-1").orElseThrow().brightness(), 1e-9);
    }

    @Test
    void setReachableAndRefreshAll() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        assertTrue(r.get("light-1").orElseThrow().reachable());
        r.setReachable("light-1", false);
        assertFalse(r.get("light-1").orElseThrow().reachable());
        r.refreshReachabilityAll(true);
        assertTrue(r.get("light-1").orElseThrow().reachable());
    }
}
