package wisdom.cube.logging;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.routine.RoutineActionKind;
import wisdom.cube.routine.RoutineDefinition;
import wisdom.cube.routine.RoutineStepResult;

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
    void recordRoutineRunProducesTimerIntentAndStepActions() {
        InMemoryBehaviourLogStore store = emptyStore();
        RoutineDefinition def = new RoutineDefinition(
            "r1",
            "Test routine",
            "p1",
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        List<RoutineStepResult> steps = List.of(
            new RoutineStepResult(0, RoutineActionKind.NOTIFICATION, true, "notification_stub", null, null),
            new RoutineStepResult(1, RoutineActionKind.DEVICE_STATE, false, "x", "NOT_FOUND", "Unknown device")
        );
        store.recordRoutineRun(def, steps);
        String json = store.toLogQueryJson();
        assertTrue(json.contains("routine.timer"));
        assertTrue(json.contains("Test routine"));
        assertTrue(json.contains("NOTIFICATION"));
        assertTrue(json.contains("NOT_FOUND"));

        String hist = store.toRoutineRunHistoryJson(10);
        assertTrue(hist.contains("\"runs\":["));
        assertTrue(hist.contains("\"routine_id\":\"r1\""));
        assertTrue(hist.contains("\"ok\":false"));
        assertTrue(hist.contains("\"kind\":\"NOTIFICATION\""));
        assertTrue(hist.contains("\"kind\":\"DEVICE_STATE\""));
        assertTrue(hist.contains("\"error_code\":\"NOT_FOUND\""));
    }

    @Test
    void routineRunHistoryEmptyWhenNoRuns() {
        InMemoryBehaviourLogStore store = emptyStore();
        assertEquals("{\"runs\":[]}", store.toRoutineRunHistoryJson(50));
    }

    @Test
    void internetActivityJsonEmptyWhenNoCalls() {
        InMemoryBehaviourLogStore store = emptyStore();
        assertEquals("{\"events\":[]}", store.toInternetActivityJson(50));
    }

    @Test
    void internetActivityJsonNewestFirstAndRespectsLimit() {
        InMemoryBehaviourLogStore store = emptyStore();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        Instant older = Instant.parse("2026-04-10T10:00:00Z");
        Instant newer = Instant.parse("2026-04-10T11:00:00Z");
        store.writeInternetCall(new BehaviourLogSchema.InternetCallEntry(
            c1, 0, older, "cube", "p1", "older-summary", "dns", "x.example", "ok", null, null));
        store.writeInternetCall(new BehaviourLogSchema.InternetCallEntry(
            c2, 0, newer, "cube", "p1", "newer-summary", "weather", "y.example", "ok", null, null));
        String json = store.toInternetActivityJson(1);
        assertTrue(json.contains("newer-summary"));
        assertFalse(json.contains("older-summary"));
        String full = store.toInternetActivityJson(50);
        assertTrue(full.indexOf("newer-summary") < full.indexOf("older-summary"));
    }

    @Test
    void internetCallsEvictOldestWhenOverCap() {
        InMemoryBehaviourLogStore store = emptyStore();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i <= 500; i++) {
            UUID cid = UUID.randomUUID();
            store.writeInternetCall(new BehaviourLogSchema.InternetCallEntry(
                cid,
                0,
                base.plusSeconds(i),
                "cube",
                "p1",
                "call-" + i,
                "svc",
                "ep",
                "ok",
                null,
                null));
        }
        String json = store.toInternetActivityJson(10_000);
        assertFalse(json.contains("call-0"));
        assertTrue(json.contains("call-500"));
        assertTrue(json.contains("call-1"));
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

    @Test
    void recordVoiceDeviceAutomationIncludesTargetsParametersAndActionError() {
        InMemoryBehaviourLogStore store = emptyStore();
        AutomationEngine.Intent intent = new AutomationEngine.Intent("set_light", "attic", "on");
        AutomationEngine.ActionResult fail = AutomationEngine.ActionResult.failure("NOT_FOUND", "No light");
        store.recordVoiceDeviceAutomation("adult-1", "turn on attic", intent, fail, "I could not find a light.");
        String json = store.toLogQueryJson();
        assertTrue(json.contains("set_light"));
        assertTrue(json.contains("attic"));
        assertTrue(json.contains("\"targets\":\"attic\""));
        assertTrue(json.contains("\"parameters\":\"on\""));
        assertTrue(json.contains("NOT_FOUND"));
        assertTrue(json.contains("\"error_code\":\"NOT_FOUND\""));
    }
}
