package wisdom.cube.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoOpDeviceDiscoveryServiceTest {

    @Test
    void refreshReturnsZero() {
        assertEquals(0, new NoOpDeviceDiscoveryService().refreshDiscoveries(new InMemoryLightDeviceRegistry()));
    }

    @Test
    void refreshMarksInMemoryDevicesReachable() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        r.setReachable("light-2", false);
        assertEquals(0, new NoOpDeviceDiscoveryService().refreshDiscoveries(r));
        assertTrue(r.get("light-2").orElseThrow().reachable());
    }
}
