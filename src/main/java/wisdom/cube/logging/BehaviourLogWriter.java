package wisdom.cube.logging;

import wisdom.cube.core.AutomationEngine;

/**
 * Write-path for behaviour log (Plan §4). Implementations persist chain summaries,
 * intents, actions, and internet calls. No raw audio; utterance text only.
 */
public interface BehaviourLogWriter {

    void writeChainSummary(BehaviourLogSchema.ChainSummary summary);

    void writeIntent(BehaviourLogSchema.IntentEntry intent);

    void writeAction(BehaviourLogSchema.ActionEntry action);

    void writeInternetCall(BehaviourLogSchema.InternetCallEntry call);

    /**
     * Optional convenience for voice → device chains; default no-op for writers that only support raw entries.
     */
    default void recordVoiceDeviceAutomation(
        String profileId,
        String utterance,
        AutomationEngine.Intent intent,
        AutomationEngine.ActionResult result,
        String spokenToUser
    ) {
        // default no-op
    }
}
