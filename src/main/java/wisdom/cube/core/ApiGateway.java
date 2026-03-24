package wisdom.cube.core;

/**
 * App-facing API gateway: handles requests from the mobile app (config, status, backup, etc.).
 * Mocked in unit tests; skeleton implementation optional in Phase 1.4.
 */
public interface ApiGateway {

    /**
     * Start the gateway (bind port, register routes). No-op if already running.
     */
    void start();

    /**
     * Stop the gateway and release resources.
     */
    void stop();
}
