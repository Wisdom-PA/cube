package wisdom.cube.supervisor;

import wisdom.cube.core.ApiGateway;

/**
 * Adapts {@link ApiGateway} to {@link ManagedService} for use with {@link DefaultServiceSupervisor}.
 */
public final class ApiGatewayManagedService implements ManagedService {

    private final ApiGateway gateway;
    private final String name;
    private volatile boolean started;

    public ApiGatewayManagedService(String name, ApiGateway gateway) {
        this.name = name;
        this.gateway = gateway;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {
        gateway.start();
        started = true;
    }

    @Override
    public void stop() {
        gateway.stop();
        started = false;
    }

    @Override
    public boolean healthy() {
        return started;
    }
}
