package wisdom.cube.intent;

import org.junit.jupiter.api.Test;
import wisdom.cube.core.AutomationEngine;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedIntentClassifierTest {

    private final RuleBasedIntentClassifier c = new RuleBasedIntentClassifier();

    @Test
    void livingRoomOn() {
        IntentClassification r = c.classify("turn on living room light");
        assertInstanceOf(IntentClassification.Resolved.class, r);
        AutomationEngine.Intent i = ((IntentClassification.Resolved) r).intent();
        assertTrue(i.targets().contains("living"));
    }

    @Test
    void ambiguousLight() {
        IntentClassification r = c.classify("turn on the light");
        assertInstanceOf(IntentClassification.NeedsClarification.class, r);
    }

    @Test
    void unknown() {
        assertInstanceOf(IntentClassification.Unknown.class, c.classify("weather in Paris"));
    }

    @Test
    void brightness() {
        IntentClassification r = c.classify("set brightness dimmer");
        assertInstanceOf(IntentClassification.Resolved.class, r);
    }

    @Test
    void livingRoomLightWithoutOnOffIsUnknown() {
        assertInstanceOf(IntentClassification.Unknown.class, c.classify("living room light"));
    }

    @Test
    void kitchenLightWithoutOnOffIsUnknown() {
        assertInstanceOf(IntentClassification.Unknown.class, c.classify("kitchen light status"));
    }

    @Test
    void kitchenLightOn() {
        IntentClassification r = c.classify("kitchen light on please");
        assertInstanceOf(IntentClassification.Resolved.class, r);
    }

    @Test
    void livingRoomLightOff() {
        IntentClassification r = c.classify("living room light off");
        assertInstanceOf(IntentClassification.Resolved.class, r);
    }

    @Test
    void blankAndNullAreUnknown() {
        assertInstanceOf(IntentClassification.Unknown.class, c.classify(""));
        assertInstanceOf(IntentClassification.Unknown.class, c.classify("   "));
        assertInstanceOf(IntentClassification.Unknown.class, c.classify(null));
    }

    @Test
    void switchKeywordTriggersClarification() {
        assertInstanceOf(IntentClassification.NeedsClarification.class, c.classify("switch the light"));
    }
}
