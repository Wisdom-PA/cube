package wisdom.cube.gateway;

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
}
