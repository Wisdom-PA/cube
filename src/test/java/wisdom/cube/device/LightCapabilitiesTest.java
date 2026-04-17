package wisdom.cube.device;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightCapabilitiesTest {

    @Test
    void nullConstructorYieldsEmptyCapabilities() {
        LightCapabilities c = new LightCapabilities(null);
        assertTrue(c.capabilities().isEmpty());
        assertFalse(c.supports(LightCapability.ON_OFF));
    }

    @Test
    void capabilitiesAccessorReturnsSameContents() {
        EnumSet<LightCapability> in = EnumSet.of(LightCapability.ON_OFF);
        LightCapabilities c = new LightCapabilities(in);
        assertEquals(Set.of(LightCapability.ON_OFF), c.capabilities());
    }

    @Test
    void equalsAndHashCodeDependOnSetContents() {
        LightCapabilities a = new LightCapabilities(EnumSet.of(LightCapability.ON_OFF));
        LightCapabilities b = new LightCapabilities(EnumSet.of(LightCapability.ON_OFF));
        LightCapabilities c = new LightCapabilities(EnumSet.of(LightCapability.COLOR_RGB));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void toStringMentionsCapabilities() {
        LightCapabilities caps = new LightCapabilities(EnumSet.of(LightCapability.ON_OFF));
        assertTrue(caps.toString().contains("ON_OFF"));
    }
}
