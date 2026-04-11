package wisdom.cube.intent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wisdom.cube.core.AutomationEngine;
import wisdom.cube.device.LightDevice;
import wisdom.cube.device.LightDeviceRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SingleDeviceFallbackIntentClassifierTest {

    @Mock
    LightDeviceRegistry reg;

    @Test
    void singleLightResolvesGenericOnCommand() {
        when(reg.allInOrder()).thenReturn(List.of(
            new LightDevice("l1", "Sofa", "light", "Living room", false, 0.4, true)
        ));
        IntentClassifier inner = transcript -> new IntentClassification.Unknown();
        SingleDeviceFallbackIntentClassifier c = new SingleDeviceFallbackIntentClassifier(reg, inner);
        IntentClassification r = c.classify("turn on the light");
        assertInstanceOf(IntentClassification.Resolved.class, r);
        AutomationEngine.Intent i = ((IntentClassification.Resolved) r).intent();
        assertEquals("set_light", i.type());
        assertEquals("living_room", i.targets());
        assertEquals("on", i.parameters());
    }

    @Test
    void multipleLightsFallsThroughToUnknown() {
        when(reg.allInOrder()).thenReturn(List.of(
            new LightDevice("a", "A", "light", "R1", false, 0.5, true),
            new LightDevice("b", "B", "light", "R2", false, 0.5, true)
        ));
        IntentClassifier inner = transcript -> new IntentClassification.Unknown();
        SingleDeviceFallbackIntentClassifier c = new SingleDeviceFallbackIntentClassifier(reg, inner);
        assertInstanceOf(IntentClassification.Unknown.class, c.classify("turn on the light"));
    }

    @Test
    void roomDisplayToSlugNormalizesSpaces() {
        assertEquals("living_room", SingleDeviceFallbackIntentClassifier.roomDisplayToSlug("Living room"));
        assertEquals("unknown", SingleDeviceFallbackIntentClassifier.roomDisplayToSlug(null));
        assertEquals("unknown", SingleDeviceFallbackIntentClassifier.roomDisplayToSlug("   "));
    }

    @Test
    void singleLightTurnOff() {
        when(reg.allInOrder()).thenReturn(List.of(
            new LightDevice("l1", "Sofa", "light", "Living room", true, 1.0, true)
        ));
        IntentClassifier inner = transcript -> new IntentClassification.Unknown();
        SingleDeviceFallbackIntentClassifier c = new SingleDeviceFallbackIntentClassifier(reg, inner);
        IntentClassification r = c.classify("turn off the light");
        assertEquals("off", ((IntentClassification.Resolved) r).intent().parameters());
    }

    @Test
    void delegatesResolvedFromInnerClassifier() {
        IntentClassifier inner = transcript -> new IntentClassification.Resolved(
            new AutomationEngine.Intent("set_light", "living_room", "on"));
        SingleDeviceFallbackIntentClassifier c = new SingleDeviceFallbackIntentClassifier(reg, inner);
        IntentClassification r = c.classify("anything");
        assertEquals("living_room", ((IntentClassification.Resolved) r).intent().targets());
    }

    @Test
    void lightMentionWithoutOnOrOffStaysUnknown() {
        when(reg.allInOrder()).thenReturn(List.of(
            new LightDevice("l1", "Sofa", "light", "Living room", false, 0.4, true)
        ));
        IntentClassifier inner = transcript -> new IntentClassification.Unknown();
        SingleDeviceFallbackIntentClassifier c = new SingleDeviceFallbackIntentClassifier(reg, inner);
        assertInstanceOf(IntentClassification.Unknown.class, c.classify("the light is nice"));
    }
}
