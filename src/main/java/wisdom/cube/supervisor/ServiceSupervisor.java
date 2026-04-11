package wisdom.cube.supervisor;

/**
 * Starts managed services and applies health-based restarts (F2.T2.S2).
 */
public interface ServiceSupervisor {

    void register(ManagedService service, ServiceRestartPolicy policy);

    void startAll() throws Exception;

    void stopAll();

    /**
     * One health pass: restart services that are unhealthy and still allowed by policy.
     */
    void tickHealth();
}
