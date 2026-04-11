package wisdom.cube.intent;

import wisdom.cube.core.AutomationEngine;

/**
 * Result of NLU (F4.T2): resolved intent, clarification question, or unknown.
 */
public sealed interface IntentClassification permits
    IntentClassification.Resolved,
    IntentClassification.NeedsClarification,
    IntentClassification.Unknown {

    record Resolved(AutomationEngine.Intent intent) implements IntentClassification { }

    record NeedsClarification(String questionForUser) implements IntentClassification { }

    record Unknown() implements IntentClassification { }
}
