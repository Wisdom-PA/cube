package wisdom.cube.logging;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.routine.RoutineDefinition;
import wisdom.cube.routine.RoutineStepResult;
import wisdom.cube.util.JsonStrings;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * In-memory {@link BehaviourLogWriter} that can render {@code LogQueryResult}-shaped JSON for
 * {@code GET /logs} (F8.T1.S2 starter — no rotation or disk yet).
 */
public final class InMemoryBehaviourLogStore implements BehaviourLogWriter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final Object lock = new Object();
    private static final int MAX_ROUTINE_RUN_HISTORY = 500;

    private final List<BehaviourLogSchema.ChainSummary> summaries = new ArrayList<>();
    private final List<BehaviourLogSchema.IntentEntry> intents = new ArrayList<>();
    private final List<BehaviourLogSchema.ActionEntry> actions = new ArrayList<>();
    private final List<BehaviourLogSchema.InternetCallEntry> internetCalls = new ArrayList<>();
    /** Newest first; capped for inspection API (F6.T5). */
    private final List<RoutineRunSnapshot> routineRuns = new ArrayList<>();

    @Override
    public void writeChainSummary(BehaviourLogSchema.ChainSummary summary) {
        synchronized (lock) {
            summaries.add(summary);
        }
    }

    @Override
    public void writeIntent(BehaviourLogSchema.IntentEntry intent) {
        synchronized (lock) {
            intents.add(intent);
        }
    }

    @Override
    public void writeAction(BehaviourLogSchema.ActionEntry action) {
        synchronized (lock) {
            actions.add(action);
        }
    }

    @Override
    public void writeInternetCall(BehaviourLogSchema.InternetCallEntry call) {
        synchronized (lock) {
            internetCalls.add(call);
        }
    }

    /**
     * Records one closed chain for a successful companion-app device PATCH.
     */
    public void recordDevicePatchFromApp(String deviceId, String patchBody, String deviceJsonResponse) {
        UUID chainId = UUID.randomUUID();
        Instant now = Instant.now();
        String body = patchBody == null ? "" : patchBody;
        String response = deviceJsonResponse == null ? "" : deviceJsonResponse;
        writeChainSummary(new BehaviourLogSchema.ChainSummary(
            chainId,
            "cube",
            now,
            Optional.of(now),
            "app",
            now,
            "app",
            List.of()
        ));
        writeIntent(new BehaviourLogSchema.IntentEntry(
            chainId,
            0,
            now,
            "Device control (companion app)",
            "device.patch",
            deviceId,
            body,
            "app"
        ));
        writeAction(new BehaviourLogSchema.ActionEntry(
            chainId,
            0,
            0,
            now,
            truncate(response, 500),
            "ok",
            null,
            null
        ));
    }

    /**
     * Records one closed chain for voice → device automation (no audio; utterance text only).
     */
    @Override
    public void recordVoiceDeviceAutomation(
        String profileId,
        String utterance,
        AutomationEngine.Intent intent,
        AutomationEngine.ActionResult result,
        String spokenToUser
    ) {
        UUID chainId = UUID.randomUUID();
        Instant now = Instant.now();
        String pid = profileId == null ? "unknown" : profileId;
        String utt = utterance == null ? "" : utterance;
        String spoken = spokenToUser == null ? "" : spokenToUser;
        writeChainSummary(new BehaviourLogSchema.ChainSummary(
            chainId,
            "cube",
            now,
            Optional.of(now),
            pid,
            now,
            pid,
            List.of()
        ));
        writeIntent(new BehaviourLogSchema.IntentEntry(
            chainId,
            0,
            now,
            utt,
            intent.type(),
            intent.targets(),
            intent.parameters(),
            pid
        ));
        String summary = intent.type() + " " + intent.targets() + " " + intent.parameters()
            + " → " + (result.success() ? spoken : result.errorMessage());
        writeAction(new BehaviourLogSchema.ActionEntry(
            chainId,
            0,
            0,
            now,
            truncate(summary, 500),
            result.success() ? "ok" : "error",
            result.success() ? null : result.errorCode(),
            result.success() ? null : result.errorMessage()
        ));
    }

    /**
     * Records one closed chain for a scheduled routine run (F6.T4.S4).
     */
    public void recordRoutineRun(RoutineDefinition routine, List<RoutineStepResult> steps) {
        UUID chainId = UUID.randomUUID();
        Instant now = Instant.now();
        String owner = routine.ownerProfileId() == null ? "unknown" : routine.ownerProfileId();
        String rid = routine.routineId() == null ? "" : routine.routineId();
        String rname = routine.name() == null ? "" : routine.name();
        writeChainSummary(new BehaviourLogSchema.ChainSummary(
            chainId,
            "cube",
            now,
            Optional.of(now),
            owner,
            now,
            owner,
            List.of()
        ));
        writeIntent(new BehaviourLogSchema.IntentEntry(
            chainId,
            0,
            now,
            "Routine timer: " + rname,
            "routine.timer",
            rid,
            "triggers=" + routine.triggers().size() + ",actions=" + routine.actions().size(),
            owner
        ));
        int ai = 0;
        for (RoutineStepResult s : steps) {
            writeAction(new BehaviourLogSchema.ActionEntry(
                chainId,
                ai,
                0,
                now,
                truncate(s.kind() + " " + s.summary(), 500),
                s.success() ? "ok" : "error",
                s.success() ? null : s.errorCode(),
                s.success() ? null : s.errorMessage()
            ));
            ai++;
        }
        boolean allOk = steps.stream().allMatch(RoutineStepResult::success);
        List<RoutineRunStepSnapshot> snapSteps = new ArrayList<>(steps.size());
        for (RoutineStepResult s : steps) {
            snapSteps.add(new RoutineRunStepSnapshot(
                s.stepIndex(),
                String.valueOf(s.kind()),
                s.success(),
                s.summary() == null ? "" : s.summary(),
                s.errorCode(),
                s.errorMessage()
            ));
        }
        RoutineRunSnapshot snap = new RoutineRunSnapshot(
            chainId,
            now,
            rid,
            rname,
            allOk,
            List.copyOf(snapSteps)
        );
        synchronized (lock) {
            routineRuns.add(0, snap);
            while (routineRuns.size() > MAX_ROUTINE_RUN_HISTORY) {
                routineRuns.remove(routineRuns.size() - 1);
            }
        }
    }

    /**
     * JSON for {@code GET /routines/history}: newest runs first, up to {@code limit} entries.
     */
    public String toRoutineRunHistoryJson(int limit) {
        int cap = Math.max(1, Math.min(limit, MAX_ROUTINE_RUN_HISTORY));
        synchronized (lock) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"runs\":[");
            int n = Math.min(cap, routineRuns.size());
            boolean first = true;
            for (int i = 0; i < n; i++) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                appendRoutineRunJson(sb, routineRuns.get(i));
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    private static void appendRoutineRunJson(StringBuilder sb, RoutineRunSnapshot r) {
        sb.append("{\"run_id\":\"").append(r.runId).append("\",");
        sb.append("\"at\":\"").append(ISO.format(r.at)).append("\",");
        sb.append("\"routine_id\":\"").append(JsonStrings.escape(r.routineId)).append("\",");
        sb.append("\"routine_name\":\"").append(JsonStrings.escape(r.routineName)).append("\",");
        sb.append("\"ok\":").append(r.allStepsOk).append(',');
        sb.append("\"steps\":[");
        boolean fs = true;
        for (RoutineRunStepSnapshot s : r.steps) {
            if (!fs) {
                sb.append(',');
            }
            fs = false;
            sb.append("{\"index\":").append(s.index).append(',');
            sb.append("\"kind\":\"").append(JsonStrings.escape(s.kind)).append("\",");
            sb.append("\"summary\":\"").append(JsonStrings.escape(s.summary)).append("\",");
            sb.append("\"ok\":").append(s.ok).append(',');
            sb.append("\"error_code\":");
            appendJsonNullableString(sb, s.errorCode);
            sb.append(",\"error_message\":");
            appendJsonNullableString(sb, s.errorMessage);
            sb.append('}');
        }
        sb.append("]}");
    }

    private static final class RoutineRunSnapshot {
        private final UUID runId;
        private final Instant at;
        private final String routineId;
        private final String routineName;
        private final boolean allStepsOk;
        private final List<RoutineRunStepSnapshot> steps;

        private RoutineRunSnapshot(
            UUID runId,
            Instant at,
            String routineId,
            String routineName,
            boolean allStepsOk,
            List<RoutineRunStepSnapshot> steps
        ) {
            this.runId = runId;
            this.at = at;
            this.routineId = routineId;
            this.routineName = routineName;
            this.allStepsOk = allStepsOk;
            this.steps = steps;
        }
    }

    private static final class RoutineRunStepSnapshot {
        private final int index;
        private final String kind;
        private final boolean ok;
        private final String summary;
        private final String errorCode;
        private final String errorMessage;

        private RoutineRunStepSnapshot(
            int index,
            String kind,
            boolean ok,
            String summary,
            String errorCode,
            String errorMessage
        ) {
            this.index = index;
            this.kind = kind;
            this.ok = ok;
            this.summary = summary;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Records one closed chain for a stub chat reply from {@code POST /chat}.
     */
    public void recordChatFromApp(String userMessage, String replyText) {
        UUID chainId = UUID.randomUUID();
        Instant now = Instant.now();
        String msg = userMessage == null ? "" : userMessage;
        String reply = replyText == null ? "" : replyText;
        writeChainSummary(new BehaviourLogSchema.ChainSummary(
            chainId,
            "cube",
            now,
            Optional.of(now),
            "app",
            now,
            "app",
            List.of()
        ));
        writeIntent(new BehaviourLogSchema.IntentEntry(
            chainId,
            0,
            now,
            msg,
            "chat.message",
            "",
            "",
            "app"
        ));
        writeAction(new BehaviourLogSchema.ActionEntry(
            chainId,
            0,
            0,
            now,
            truncate(reply, 500),
            "ok",
            null,
            null
        ));
    }

    /**
     * JSON object matching {@code LogQueryResult}: {@code { "chains": [ ... ] }}.
     */
    public String toLogQueryJson() {
        synchronized (lock) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"chains\":[");
            boolean firstChain = true;
            for (BehaviourLogSchema.ChainSummary s : summaries) {
                if (!firstChain) {
                    sb.append(',');
                }
                firstChain = false;
                appendChainJson(sb, s);
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    private void appendChainJson(StringBuilder sb, BehaviourLogSchema.ChainSummary s) {
        UUID cid = s.chainId();
        sb.append("{\"chain_id\":\"").append(cid).append("\",");
        sb.append("\"chain_start_ts\":\"").append(ISO.format(s.chainStartTs())).append("\"");
        if (s.chainEndTs().isPresent()) {
            sb.append(",\"chain_end_ts\":\"").append(ISO.format(s.chainEndTs().get())).append("\"");
        }
        sb.append(",\"intents\":[");
        appendIntentsForChain(sb, cid);
        sb.append("],\"actions\":[");
        appendActionsForChain(sb, cid);
        sb.append("],\"internet_calls\":[");
        appendInternetCallsForChain(sb, cid);
        sb.append("],\"privacy_mode_changes\":[");
        appendPrivacyChanges(sb, s.privacyModeChanges());
        sb.append("]}");
    }

    private void appendIntentsForChain(StringBuilder sb, UUID chainId) {
        List<BehaviourLogSchema.IntentEntry> rows = intentsFor(chainId);
        boolean first = true;
        for (BehaviourLogSchema.IntentEntry i : rows) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"utterance\":\"").append(JsonStrings.escape(i.utteranceText())).append("\",");
            sb.append("\"intent_index\":").append(i.intentIndex()).append(',');
            sb.append("\"type\":\"").append(JsonStrings.escape(i.type())).append("\",");
            sb.append("\"targets\":\"").append(JsonStrings.escape(i.targets())).append("\",");
            sb.append("\"parameters\":\"").append(JsonStrings.escape(i.parameters())).append("\",");
            sb.append("\"profile_id\":\"").append(JsonStrings.escape(i.profileId())).append("\",");
            sb.append("\"ts\":\"").append(ISO.format(i.ts())).append("\"}");
        }
    }

    private List<BehaviourLogSchema.IntentEntry> intentsFor(UUID chainId) {
        return intents.stream()
            .filter(i -> i.chainId().equals(chainId))
            .sorted(Comparator.comparingInt(BehaviourLogSchema.IntentEntry::intentIndex))
            .collect(Collectors.toList());
    }

    private void appendActionsForChain(StringBuilder sb, UUID chainId) {
        List<BehaviourLogSchema.ActionEntry> rows = actions.stream()
            .filter(a -> a.chainId().equals(chainId))
            .sorted(Comparator.comparingInt(BehaviourLogSchema.ActionEntry::actionIndex))
            .collect(Collectors.toList());
        boolean first = true;
        for (BehaviourLogSchema.ActionEntry a : rows) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"action_index\":").append(a.actionIndex()).append(',');
            sb.append("\"intent_index\":").append(a.intentIndex()).append(',');
            sb.append("\"result\":\"").append(JsonStrings.escape(a.result())).append("\",");
            sb.append("\"before_after_summary\":\"").append(JsonStrings.escape(a.beforeAfterSummary()))
                .append("\",");
            sb.append("\"ts\":\"").append(ISO.format(a.ts())).append("\",");
            sb.append("\"error_code\":");
            appendJsonNullableString(sb, a.errorCode());
            sb.append(",\"error_message\":");
            appendJsonNullableString(sb, a.errorMessage());
            sb.append('}');
        }
    }

    private void appendInternetCallsForChain(StringBuilder sb, UUID chainId) {
        List<BehaviourLogSchema.InternetCallEntry> rows = internetCalls.stream()
            .filter(c -> c.chainId().equals(chainId))
            .sorted(Comparator.comparingInt(BehaviourLogSchema.InternetCallEntry::callIndex))
            .collect(Collectors.toList());
        boolean first = true;
        for (BehaviourLogSchema.InternetCallEntry c : rows) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"call_index\":").append(c.callIndex()).append(',');
            sb.append("\"ts\":\"").append(ISO.format(c.ts())).append("\",");
            sb.append("\"service_category\":\"").append(JsonStrings.escape(c.serviceCategory())).append("\",");
            sb.append("\"endpoint\":\"").append(JsonStrings.escape(c.endpoint())).append("\",");
            sb.append("\"result\":\"").append(JsonStrings.escape(c.result())).append("\"}");
        }
    }

    private static void appendPrivacyChanges(StringBuilder sb, List<BehaviourLogSchema.PrivacyModeChange> changes) {
        boolean first = true;
        for (BehaviourLogSchema.PrivacyModeChange p : changes) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"at_ts\":\"").append(ISO.format(p.atTs())).append("\",");
            sb.append("\"from_mode\":\"").append(JsonStrings.escape(p.fromMode())).append("\",");
            sb.append("\"to_mode\":\"").append(JsonStrings.escape(p.toMode())).append("\",");
            sb.append("\"trigger\":\"").append(JsonStrings.escape(p.trigger())).append("\"}");
        }
    }

    private static void appendJsonNullableString(StringBuilder sb, String value) {
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(JsonStrings.escape(value)).append('"');
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "…";
    }
}
