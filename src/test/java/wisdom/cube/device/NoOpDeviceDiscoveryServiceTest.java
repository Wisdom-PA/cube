package wisdom.cube.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NoOpDeviceDiscoveryServiceTest {

    @Test
    void refreshReturnsZero() {
        assertEquals(0, new NoOpDeviceDiscoveryService().refreshDiscoveries(new InMemoryLightDeviceRegistry()));
    }
}
