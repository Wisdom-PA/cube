package wisdom.cube;

import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.gateway.DeviceFixtureStore;
import wisdom.cube.gateway.HttpServerGateway;
import wisdom.cube.logging.InMemoryBehaviourLogStore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Entry point for the on-device assistant. With {@code --port} or {@code CUBE_PORT}, starts the
 * HTTP API gateway ({@link HttpServerGateway}) and blocks until the JVM shuts down.
 */
public final class Cube {

    private Cube() {
    }

    public static void main(String[] args) {
        Integer port = resolveListenPort(args, System.getenv("CUBE_PORT"));
        if (port == null) {
            return;
        }
        runGatewayBlocking(port);
    }

    /**
     * Parses {@code --port N} from args, otherwise {@code CUBE_PORT} env. Returns null if neither
     * is set (no gateway; normal for tests and scaffold runs).
     */
    static Integer resolveListenPort(String[] args, String envCubePort) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    return Integer.valueOf(args[i + 1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid --port value: " + args[i + 1], e);
                }
            }
        }
        if (envCubePort != null && !envCubePort.isBlank()) {
            try {
                return Integer.valueOf(envCubePort.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid CUBE_PORT: " + envCubePort, e);
            }
        }
        return null;
    }

    private static void runGatewayBlocking(int requestedPort) {
        ExecutorService pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "cube-http");
            t.setDaemon(true);
            return t;
        });
        InMemoryBehaviourLogStore behaviourLog = new InMemoryBehaviourLogStore();
        DeviceFixtureStore deviceStore = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        HttpServerGateway gateway = new HttpServerGateway(requestedPort, pool, behaviourLog, deviceStore);
        gateway.start();
        int bound = gateway.getPort();
        System.out.println("Cube API gateway: http://127.0.0.1:" + bound);
        Semaphore stopped = new Semaphore(0);
        Thread shutdownHook = new Thread(() -> {
            gateway.stop();
            pool.shutdown();
            stopped.release();
        }, "cube-gateway-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            stopped.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            gateway.stop();
            pool.shutdown();
        }
    }
}
