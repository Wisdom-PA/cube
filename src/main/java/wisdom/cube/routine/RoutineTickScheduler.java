package wisdom.cube.routine;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import wisdom.cube.device.LightDeviceRegistry;
import wisdom.cube.logging.InMemoryBehaviourLogStore;

/**
 * Periodic routine evaluation (F6.T4.S2): time triggers and in-process tick listener.
 */
public final class RoutineTickScheduler {

    private static final Logger LOG = Logger.getLogger(RoutineTickScheduler.class.getName());

    private final RoutineTickProcessor processor;
    private final long period;
    private final TimeUnit unit;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public RoutineTickScheduler(
        Clock clock,
        RoutineCatalog catalog,
        LightDeviceRegistry registry,
        InMemoryBehaviourLogStore behaviourLog,
        long periodSeconds
    ) {
        this(clock, catalog, registry, behaviourLog, periodSeconds, TimeUnit.SECONDS);
    }

    public RoutineTickScheduler(
        Clock clock,
        RoutineCatalog catalog,
        LightDeviceRegistry registry,
        InMemoryBehaviourLogStore behaviourLog,
        long period,
        TimeUnit unit
    ) {
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(behaviourLog, "behaviourLog");
        Objects.requireNonNull(unit, "unit");
        if (period <= 0L) {
            throw new IllegalArgumentException("period must be positive");
        }
        this.processor = new RoutineTickProcessor(clock, catalog, registry, behaviourLog);
        this.period = period;
        this.unit = unit;
    }

    public void start() {
        if (future != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cube-routine-tick");
            t.setDaemon(true);
            return t;
        });
        future = executor.scheduleAtFixedRate(this::runOnceSafe, period, period, unit);
    }

    private void runOnceSafe() {
        try {
            processor.onTick();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Routine tick failed", e);
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
