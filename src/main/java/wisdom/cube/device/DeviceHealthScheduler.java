package wisdom.cube.device;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs {@link DeviceDiscoveryService#refreshDiscoveries(LightDeviceRegistry)} on a fixed delay so
 * integrations can refresh reachability without an explicit app scan (F6.T3.S1).
 */
public final class DeviceHealthScheduler {

    private static final Logger LOG = Logger.getLogger(DeviceHealthScheduler.class.getName());

    private final DeviceDiscoveryService discovery;
    private final LightDeviceRegistry registry;
    private final long period;
    private final TimeUnit unit;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    /**
     * @param period length of each interval; must be positive
     */
    public DeviceHealthScheduler(
        DeviceDiscoveryService discovery,
        LightDeviceRegistry registry,
        long period,
        TimeUnit unit
    ) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.unit = Objects.requireNonNull(unit, "unit");
        if (period <= 0) {
            throw new IllegalArgumentException("period must be positive");
        }
        this.period = period;
    }

    /** Convenience for production wiring with second-based intervals. */
    public static DeviceHealthScheduler everySeconds(
        DeviceDiscoveryService discovery,
        LightDeviceRegistry registry,
        long periodSeconds
    ) {
        return new DeviceHealthScheduler(discovery, registry, periodSeconds, TimeUnit.SECONDS);
    }

    public void start() {
        if (future != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cube-device-health");
            t.setDaemon(true);
            return t;
        });
        future = executor.scheduleAtFixedRate(this::runOnceSafe, period, period, unit);
    }

    private void runOnceSafe() {
        try {
            discovery.refreshDiscoveries(registry);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Device health refresh failed", e);
        }
    }

    public void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
