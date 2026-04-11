package wisdom.cube.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DeviceFixtureStoreTest {

    @Test
    void patchUpdatesPowerAndBrightness() {
        DeviceFixtureStore store = new DeviceFixtureStore();
        assertTrue(store.listJson().contains("\"power\":true"));
        String updated = store.patch("light-2", "{\"power\":true,\"brightness\":0.5}");
        assertNotNull(updated);
        assertTrue(updated.contains("\"power\":true"));
        assertTrue(updated.contains("\"brightness\":0.5"));
        String list = store.listJson();
        assertTrue(list.contains("\"id\":\"light-2\""));
        assertTrue(list.contains("\"brightness\":0.5"));
    }

    @Test
    void patchUnknownReturnsNull() {
        DeviceFixtureStore store = new DeviceFixtureStore();
        assertNull(store.patch("nope", "{\"power\":true}"));
    }

    @Test
    void patchWithNullOrBlankBodyReturnsDeviceJsonWithoutChangingFields() {
        DeviceFixtureStore store = new DeviceFixtureStore();
        String before = store.listJson();
        assertNotNull(store.patch("light-2", null));
        assertEquals(before, store.listJson());
        assertNotNull(store.patch("light-2", "   "));
        assertEquals(before, store.listJson());
    }

    @Test
    void patchBrightnessOnlyLeavesPowerUnchanged() {
        DeviceFixtureStore store = new DeviceFixtureStore();
        String updated = store.patch("light-2", "{\"brightness\":0.33}");
        assertNotNull(updated);
        assertTrue(updated.contains("0.33"));
        assertTrue(updated.contains("\"power\":false"));
    }

    @Test
    void patchPowerOnlyLeavesBrightnessFromPreviousPatch() {
        DeviceFixtureStore store = new DeviceFixtureStore();
        store.patch("light-1", "{\"brightness\":0.4}");
        String updated = store.patch("light-1", "{\"power\":false}");
        assertNotNull(updated);
        assertTrue(updated.contains("\"power\":false"));
        assertTrue(updated.contains("0.4"));
    }

    @Test
    void patchBrightnessClampsToUnitInterval() {
        DeviceFixtureStore store = new DeviceFixtureStore();
        String high = store.patch("light-1", "{\"brightness\":99}");
        int hb = high.indexOf("\"brightness\":");
        assertTrue(hb >= 0, high);
        String highTail = high.substring(hb);
        assertTrue(
            highTail.startsWith("\"brightness\":1}") || highTail.startsWith("\"brightness\":1,")
                || highTail.startsWith("\"brightness\":1.0}") || highTail.startsWith("\"brightness\":1.0,"),
            high);
        String low = store.patch("light-2", "{\"brightness\":-1}");
        assertTrue(low.contains("\"brightness\":0}") || low.contains("\"brightness\":0,"));
    }
}
