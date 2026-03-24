package wisdom.cube.core;

import java.util.Optional;

/**
 * Executes device actions and routines (lights, scenes, etc.).
 * Mocked in unit tests; real implementation in Phase 7.
 */
public interface AutomationEngine {

    /**
     * Execute an intent (e.g. set light brightness). Returns result summary or empty on failure.
     */
    Optional<ActionResult> execute(Intent intent);

    /**
     * Parsed intent: type, targets, parameters (strings per Plan §4).
     */
    record Intent(String type, String targets, String parameters) {}

    /**
     * Result of one action: success/failure and optional error.
     */
    record ActionResult(boolean success, String errorCode, String errorMessage) {

        public static ActionResult ok() {
            return new ActionResult(true, null, null);
        }

        public static ActionResult failure(String errorCode, String errorMessage) {
            return new ActionResult(false, errorCode, errorMessage);
        }
    }
}
