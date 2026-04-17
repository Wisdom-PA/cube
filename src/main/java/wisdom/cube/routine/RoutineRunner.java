package wisdom.cube.routine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wisdom.cube.device.LightDevice;
import wisdom.cube.device.LightDeviceRegistry;

/**
 * Runs routine actions in order; failures do not stop later steps (F6.T4.S3).
 */
public final class RoutineRunner {

    private static final Pattern DEVICE_ID = Pattern.compile("\"device_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern POWER = Pattern.compile("\"power\"\\s*:\\s*(true|false)");
    private static final Pattern BRIGHTNESS = Pattern.compile(
        "\"brightness\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");

    public List<RoutineStepResult> run(RoutineDefinition def, LightDeviceRegistry registry) {
        List<RoutineStepResult> out = new ArrayList<>();
        int i = 0;
        for (RoutineAction action : def.actions()) {
            out.add(execute(i++, action, registry));
        }
        return List.copyOf(out);
    }

    private static RoutineStepResult execute(int index, RoutineAction action, LightDeviceRegistry registry) {
        return switch (action.kind()) {
            case DEVICE_STATE -> deviceState(index, action.payload(), registry);
            case DELAY -> new RoutineStepResult(
                index,
                action.kind(),
                true,
                "delay_deferred_stub",
                null,
                null
            );
            case NOTIFICATION -> new RoutineStepResult(
                index,
                action.kind(),
                true,
                "notification_stub",
                null,
                null
            );
        };
    }

    private static RoutineStepResult deviceState(int index, String payload, LightDeviceRegistry registry) {
        Matcher idm = DEVICE_ID.matcher(payload == null ? "" : payload);
        if (!idm.find()) {
            return new RoutineStepResult(
                index,
                RoutineActionKind.DEVICE_STATE,
                false,
                "missing device_id",
                "BAD_PAYLOAD",
                "device_id"
            );
        }
        String id = idm.group(1);
        if (!registry.contains(id)) {
            return new RoutineStepResult(
                index,
                RoutineActionKind.DEVICE_STATE,
                false,
                "device " + id,
                "NOT_FOUND",
                "Unknown device"
            );
        }
        Optional<LightDevice> cur = registry.get(id);
        if (cur.isPresent() && !cur.get().reachable()) {
            return new RoutineStepResult(
                index,
                RoutineActionKind.DEVICE_STATE,
                false,
                "device " + id,
                "UNREACHABLE",
                "Device unreachable"
            );
        }
        Matcher pm = POWER.matcher(payload);
        if (pm.find()) {
            registry.setPower(id, Boolean.parseBoolean(pm.group(1)));
        }
        Matcher bm = BRIGHTNESS.matcher(payload);
        if (bm.find()) {
            registry.setBrightness(id, Double.parseDouble(bm.group(1)));
        }
        String summary = "patched " + id;
        return new RoutineStepResult(index, RoutineActionKind.DEVICE_STATE, true, summary, null, null);
    }
}
