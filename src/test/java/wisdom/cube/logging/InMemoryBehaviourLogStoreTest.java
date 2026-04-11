package wisdom.cube.logging;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class InMemoryBehaviourLogStoreTest {

    private static InMemoryBehaviourLogStore emptyStore() {
        return new InMemoryBehaviourLogStore();
    }

    @Test
    void emptyStoreSerializesEmptyChains() {
        InMemoryBehaviourLogStore store = emptyStore();
        assertEquals("{\"chains\":[]}", store.toLogQueryJson());
    }

    @Test
    void recordDevicePatchProducesChainWithIdAndIntent() {
        InMemoryBehaviourLogStore store = emptyStore();
        store.recordDevicePatchFromApp("light-1", "{\"power\":true}", "{\"id\":\"light-1\"}");
        String json = store.toLogQueryJson();
        assertTrue(json.contains("\"chains\":["));
        assertTrue(json.contains("\"chain_id\":"));
        assertTrue(json.contains("Device control (companion app)"));
        assertTrue(json.contains("device.patch"));
        assertTrue(json.contains("\"utterance\":"));
    }

    @Test
    void recordChatProducesUserUtteranceInIntent() {
        InMemoryBehaviourLogStore store = emptyStore();
        store.recordChatFromApp("hello cube", "On-device (stub): hello cube");
        String json = store.toLogQueryJson();
        assertTrue(json.contains("hello cube"));
        assertTrue(json.contains("chat.message"));
    }

    @Test
    void chainWithPrivacyChangeAndInternetCallSerializesBoth() {
        InMemoryBehaviourLogStore store = emptyStore();
        UUID cid = UUID.randomUUID();
        Instant now = Instant.now();
        store.writeChainSummary(new BehaviourLogSchema.ChainSummary(
            cid,
            "cube-1",
            now,
            Optional.empty(),
            "adult-1",
            now,
            "adult-1",
            List.of(new BehaviourLogSchema.PrivacyModeChange(now, "paranoid", "normal", "app"))
        ));
        store.writeIntent(new BehaviourLogSchema.IntentEntry(
            cid, 0, now, "utter", "intent.type", "t1", "p1", "adult-1"));
        store.writeAction(new BehaviourLogSchema.ActionEntry(
            cid, 0, 0, now, "before → after", "ok", "E_CODE", "E_MSG"));
        store.writeInternetCall(new BehaviourLogSchema.InternetCallEntry(
            cid, 0, now, "cube-1", "adult-1", "GET x", "weather", "api.example.com", "ok", null, null));
        String json = store.toLogQueryJson();
        assertTrue(json.contains("privacy_mode_changes"));
        assertTrue(json.contains("paranoid"));
        assertTrue(json.contains("internet_calls"));
        assertTrue(json.contains("weather"));
        assertTrue(json.contains("E_CODE"));
        assertTrue(json.contains("E_MSG"));
    }

    @Test
    void summaryOnlyChainHasEmptyIntentAndActionArrays() {
        InMemoryBehaviourLogStore store = emptyStore();
        UUID cid = UUID.randomUUID();
        Instant now = Instant.now();
        store.writeChainSummary(new BehaviourLogSchema.ChainSummary(
            cid, "cube", now, Optional.of(now), "app", now, "app", List.of()));
        String json = store.toLogQueryJson();
        assertTrue(json.contains("\"intents\":[]"));
        assertTrue(json.contains("\"actions\":[]"));
        assertTrue(json.contains("\"internet_calls\":[]"));
    }

    @Test
    void truncatesLongPatchResponseSummary() {
        InMemoryBehaviourLogStore store = emptyStore();
        String longDevice = "{\"k\":\"" + "z".repeat(600) + "\"}";
        store.recordDevicePatchFromApp("light-1", "{}", longDevice);
        assertTrue(store.toLogQueryJson().contains("…"));
    }

    @Test
    void multipleChainsKeepInsertionOrder() {
        InMemoryBehaviourLogStore store = emptyStore();
        store.recordChatFromApp("first", "r1");
        store.recordChatFromApp("second", "r2");
        String json = store.toLogQueryJson();
        assertTrue(json.indexOf("first") < json.indexOf("second"));
    }
}
