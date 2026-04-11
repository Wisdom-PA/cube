package wisdom.cube.intent;

import wisdom.cube.core.AutomationEngine;

import java.util.Locale;

/**
 * Keyword heuristic classifier; ambiguous "light" commands ask for clarification (F4.T2, F4.T3 one-shot).
 */
public final class RuleBasedIntentClassifier implements IntentClassifier {

    @Override
    public IntentClassification classify(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return new IntentClassification.Unknown();
        }
        String t = transcript.toLowerCase(Locale.ROOT).trim();
        if (t.contains("living") && t.contains("light")) {
            if (t.contains("off")) {
                return new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "living_room", "off"));
            }
            if (t.contains("on")) {
                return new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "living_room", "on"));
            }
        }
        if (t.contains("kitchen") && t.contains("light")) {
            if (t.contains("off")) {
                return new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "kitchen", "off"));
            }
            if (t.contains("on")) {
                return new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "kitchen", "on"));
            }
        }
        if (t.contains("brightness") || t.contains("dim")) {
            return new IntentClassification.Resolved(new AutomationEngine.Intent("set_brightness", "living_room", "0.5"));
        }
        if (t.contains("light") && (t.contains("turn") || t.contains("switch"))) {
            return new IntentClassification.NeedsClarification("Which room?");
        }
        return new IntentClassification.Unknown();
    }
}
