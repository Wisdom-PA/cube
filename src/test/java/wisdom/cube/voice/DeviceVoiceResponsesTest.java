package wisdom.cube.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import wisdom.cube.core.AutomationEngine;

class DeviceVoiceResponsesTest {

    @Test
    void setLightOnSaysOkay() {
        assertEquals(
            "Okay.",
            DeviceVoiceResponses.afterAutomationSuccess(
                new AutomationEngine.Intent("set_light", "living_room", "on")));
    }

    @Test
    void setBrightnessSaysUpdated() {
        assertEquals(
            "Brightness updated.",
            DeviceVoiceResponses.afterAutomationSuccess(
                new AutomationEngine.Intent("set_brightness", "living_room", "0.5")));
    }
}
