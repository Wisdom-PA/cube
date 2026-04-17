package wisdom.cube.routine;

/**
 * Outcome of one routine action during a run (F6.T4.S3).
 */
public record RoutineStepResult(
    int stepIndex,
    RoutineActionKind kind,
    boolean success,
    String summary,
    String errorCode,
    String errorMessage
) { }
