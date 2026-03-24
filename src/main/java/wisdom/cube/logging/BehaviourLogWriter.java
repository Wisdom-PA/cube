package wisdom.cube.logging;

/**
 * Write-path for behaviour log (Plan §4). Implementations persist chain summaries,
 * intents, actions, and internet calls. No raw audio; utterance text only.
 */
public interface BehaviourLogWriter {

    void writeChainSummary(BehaviourLogSchema.ChainSummary summary);

    void writeIntent(BehaviourLogSchema.IntentEntry intent);

    void writeAction(BehaviourLogSchema.ActionEntry action);

    void writeInternetCall(BehaviourLogSchema.InternetCallEntry call);
}
