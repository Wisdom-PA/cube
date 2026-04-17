package wisdom.cube.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LightDeviceModelTest {

    @Test
    void inferCapabilitiesForLightIncludesDimmable() {
        LightDevice d = new LightDevice("id", "n", "light", "Kitchen", true, 0.5, true);
        LightCapabilities c = LightCapabilities.inferFrom(d);
        assertTrue(c.supports(LightCapability.ON_OFF));
        assertTrue(c.supports(LightCapability.DIMMABLE));
        assertFalse(c.supports(LightCapability.COLOR_RGB));
    }

    @Test
    void inferCapabilitiesForUnknownTypeIsOnOffOnly() {
        LightDevice d = new LightDevice("id", "n", "plug", "Kitchen", true, 0.5, true);
        LightCapabilities c = LightCapabilities.inferFrom(d);
        assertTrue(c.supports(LightCapability.ON_OFF));
        assertFalse(c.supports(LightCapability.DIMMABLE));
    }

    @Test
    void stateClampsBrightness() {
        LightDevice d = new LightDevice("id", "n", "light", "Kitchen", true, 999, true);
        LightDeviceModel m = LightDeviceModel.from(d);
        assertEquals(1.0, m.state().brightness01(), 1e-9);
    }
}

