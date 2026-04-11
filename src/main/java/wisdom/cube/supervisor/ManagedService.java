package wisdom.cube.supervisor;

/**
 * Long-running process or subsystem managed by {@link ServiceSupervisor} (F2.T2.S2).
 */
public interface ManagedService {

    String name();

    void start() throws Exception;

    void stop();

    /**
     * When {@code false}, the supervisor may restart this service per {@link ServiceRestartPolicy}.
     */
    boolean healthy();
}
