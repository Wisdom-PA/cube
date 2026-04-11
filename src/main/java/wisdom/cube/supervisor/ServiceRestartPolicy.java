package wisdom.cube.supervisor;

/**
 * Restart limits for a {@link ManagedService}.
 */
public record ServiceRestartPolicy(int maxRestarts, long minDelayBetweenRestartsMs) {

    public ServiceRestartPolicy {
        if (maxRestarts < 0) {
            throw new IllegalArgumentException("maxRestarts must be >= 0");
        }
        if (minDelayBetweenRestartsMs < 0) {
            throw new IllegalArgumentException("minDelayBetweenRestartsMs must be >= 0");
        }
    }

    /** No automatic restarts. */
    public static ServiceRestartPolicy none() {
        return new ServiceRestartPolicy(0, 0);
    }

    /** Default dev policy: a few quick retries. */
    public static ServiceRestartPolicy defaultDev() {
        return new ServiceRestartPolicy(3, 100L);
    }
}
