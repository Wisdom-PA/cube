package wisdom.cube.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import wisdom.cube.core.AutomationEngine;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class DefaultAutomationEngineTest {

    @Test
    void setLight_onAndOff() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        DefaultAutomationEngine engine = new DefaultAutomationEngine(r);
        Optional<AutomationEngine.ActionResult> off = engine.execute(
            new AutomationEngine.Intent("set_light", "living_room", "off"));
        assertTrue(off.isPresent());
        assertTrue(off.get().success());
        assertFalse(r.get("light-1").orElseThrow().power());

        Optional<AutomationEngine.ActionResult> on = engine.execute(
            new AutomationEngine.Intent("set_light", "living_room", "on"));
        assertTrue(on.get().success());
        assertTrue(r.get("light-1").orElseThrow().power());
    }

    @Test
    void setLight_unknownRoom() {
        DefaultAutomationEngine engine = new DefaultAutomationEngine(new InMemoryLightDeviceRegistry());
        Optional<AutomationEngine.ActionResult> r = engine.execute(
            new AutomationEngine.Intent("set_light", "attic", "on"));
        assertTrue(r.isPresent());
        assertFalse(r.get().success());
        assertEquals("NOT_FOUND", r.get().errorCode());
    }

    @Test
    void setLight_badParam() {
        DefaultAutomationEngine engine = new DefaultAutomationEngine(new InMemoryLightDeviceRegistry());
        Optional<AutomationEngine.ActionResult> r = engine.execute(
            new AutomationEngine.Intent("set_light", "kitchen", "maybe"));
        assertFalse(r.get().success());
        assertEquals("BAD_PARAM", r.get().errorCode());
    }

    @Test
    void setBrightness() {
        InMemoryLightDeviceRegistry r = new InMemoryLightDeviceRegistry();
        DefaultAutomationEngine engine = new DefaultAutomationEngine(r);
        assertTrue(engine.execute(
            new AutomationEngine.Intent("set_brightness", "kitchen", "0.4")).get().success());
        assertEquals(0.4, r.get("light-2").orElseThrow().brightness(), 1e-9);
    }

    @Test
    void setBrightness_invalidNumber() {
        DefaultAutomationEngine engine = new DefaultAutomationEngine(new InMemoryLightDeviceRegistry());
        Optional<AutomationEngine.ActionResult> r = engine.execute(
            new AutomationEngine.Intent("set_brightness", "kitchen", "x"));
        assertFalse(r.get().success());
        assertEquals("BAD_PARAM", r.get().errorCode());
    }

    @Test
    void unsupportedIntentType() {
        DefaultAutomationEngine engine = new DefaultAutomationEngine(new InMemoryLightDeviceRegistry());
        Optional<AutomationEngine.ActionResult> r = engine.execute(
            new AutomationEngine.Intent("open_garage", "garage", ""));
        assertFalse(r.get().success());
        assertEquals("UNSUPPORTED", r.get().errorCode());
    }

    @Test
    void nullIntent() {
        DefaultAutomationEngine engine = new DefaultAutomationEngine(new InMemoryLightDeviceRegistry());
        Optional<AutomationEngine.ActionResult> r = engine.execute(null);
        assertFalse(r.get().success());
        assertEquals("BAD_REQUEST", r.get().errorCode());
    }
}
