package wisdom.cube.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigBodyParserTest {

    @Test
    void jsonEscapeEscapesQuotes() {
        assertEquals("x\\\"y", ConfigBodyParser.jsonEscape("x\"y"));
    }

    @Test
    void jsonEscapeDoublesBackslash() {
        assertEquals("a\\\\b", ConfigBodyParser.jsonEscape("a\\b"));
    }

    @Test
    void jsonEscapeReplacesNewlineTabCarriageReturnWithSpace() {
        assertEquals("a b c", ConfigBodyParser.jsonEscape("a\nb\rc"));
    }

    @Test
    void jsonUnescapeHandlesEscapedQuoteAndBackslash() {
        assertEquals("a\"b\\c", ConfigBodyParser.jsonUnescape("a\\\"b\\\\c"));
    }

    @Test
    void jsonUnescapePassesThroughPlainText() {
        assertEquals("plain", ConfigBodyParser.jsonUnescape("plain"));
    }

    @Test
    void jsonUnescapeBackslashBeforeNonSpecialCharKeepsBackslash() {
        assertEquals("a\\z", ConfigBodyParser.jsonUnescape("a\\z"));
    }

    @Test
    void jsonUnescapeTrailingBackslash() {
        assertEquals("z\\", ConfigBodyParser.jsonUnescape("z\\"));
    }

    @Test
    void jsonEscapeEmptyString() {
        assertEquals("", ConfigBodyParser.jsonEscape(""));
    }

    @Test
    void applyPatchNullOrBlankIsNoOp() {
        ConfigBodyParser.MutableConfig c = new ConfigBodyParser.MutableConfig("A", "paranoid");
        ConfigBodyParser.applyPatch(null, c);
        assertEquals("A", c.deviceName);
        ConfigBodyParser.applyPatch("   ", c);
        assertEquals("A", c.deviceName);
    }

    @Test
    void applyPatchUpdatesDeviceNameOnly() {
        ConfigBodyParser.MutableConfig c = new ConfigBodyParser.MutableConfig("A", "paranoid");
        ConfigBodyParser.applyPatch("{\"device_name\":\"B\"}", c);
        assertEquals("B", c.deviceName);
        assertEquals("paranoid", c.defaultPrivacyMode);
    }

    @Test
    void applyPatchUpdatesPrivacyOnly() {
        ConfigBodyParser.MutableConfig c = new ConfigBodyParser.MutableConfig("A", "paranoid");
        ConfigBodyParser.applyPatch("{\"default_privacy_mode\":\"normal\"}", c);
        assertEquals("A", c.deviceName);
        assertEquals("normal", c.defaultPrivacyMode);
    }

    @Test
    void applyPatchUnescapesDeviceNameFromJson() {
        ConfigBodyParser.MutableConfig c = new ConfigBodyParser.MutableConfig("A", "paranoid");
        String body = "{\"device_name\":\"Say \\\"Hi\\\"\"}";
        ConfigBodyParser.applyPatch(body, c);
        assertEquals("Say \"Hi\"", c.deviceName);
    }

    @Test
    void deviceNamePatternMatchesComplexEscapeSequence() {
        assertTrue(ConfigBodyParser.DEVICE_NAME_FIELD.matcher(
            "{\"device_name\":\"x\\\\y\"}").find());
    }

    @Test
    void gettersReflectMutableState() {
        ConfigBodyParser.MutableConfig c = new ConfigBodyParser.MutableConfig("n", "normal");
        assertEquals("n", c.getDeviceName());
        assertEquals("normal", c.getDefaultPrivacyMode());
        c.deviceName = "z";
        assertEquals("z", c.getDeviceName());
    }

    @Test
    void applyPatchUpdatesGlobalOffline() {
        ConfigBodyParser.MutableConfig c = new ConfigBodyParser.MutableConfig("A", "paranoid");
        ConfigBodyParser.applyPatch("{\"global_offline\":true}", c);
        assertTrue(c.isGlobalOffline());
        ConfigBodyParser.applyPatch("{\"global_offline\":false}", c);
        assertFalse(c.isGlobalOffline());
    }
}
