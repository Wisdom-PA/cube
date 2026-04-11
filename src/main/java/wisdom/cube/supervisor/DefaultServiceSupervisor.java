package wisdom.cube.supervisor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reference supervisor: start/stop all registrations; {@link #tickHealth()} restarts failed services.
 */
public final class DefaultServiceSupervisor implements ServiceSupervisor {

    private final List<Registration> registrations = new CopyOnWriteArrayList<>();
    private final java.util.function.LongSupplier clockMs;

    public DefaultServiceSupervisor() {
        this(System::currentTimeMillis);
    }

    DefaultServiceSupervisor(java.util.function.LongSupplier clockMs) {
        this.clockMs = clockMs;
    }

    @Override
    public void register(ManagedService service, ServiceRestartPolicy policy) {
        registrations.add(new Registration(service, policy));
    }

    @Override
    public void startAll() throws Exception {
        List<Exception> failures = new ArrayList<>();
        for (Registration r : registrations) {
            try {
                r.service.start();
            } catch (Exception e) {
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            Exception first = failures.get(0);
            for (int i = 1; i < failures.size(); i++) {
                first.addSuppressed(failures.get(i));
            }
            throw first;
        }
    }

    @Override
    public void stopAll() {
        for (int i = registrations.size() - 1; i >= 0; i--) {
            registrations.get(i).service.stop();
        }
    }

    @Override
    public void tickHealth() {
        long now = clockMs.getAsLong();
        for (Registration r : registrations) {
            if (r.service.healthy()) {
                continue;
            }
            ServiceRestartPolicy p = r.policy;
            if (r.restartCount >= p.maxRestarts()) {
                continue;
            }
            if (r.lastRestartAttemptMs >= 0
                && now - r.lastRestartAttemptMs < p.minDelayBetweenRestartsMs()) {
                continue;
            }
            r.lastRestartAttemptMs = now;
            try {
                r.service.stop();
            } catch (RuntimeException e) {
                throw e;
            }
            try {
                r.service.start();
                if (r.service.healthy()) {
                    r.restartCount = 0;
                } else {
                    r.restartCount++;
                }
            } catch (Exception e) {
                r.restartCount++;
            }
        }
    }

    static final class Registration {
        final ManagedService service;
        final ServiceRestartPolicy policy;
        int restartCount;
        /** -1 if no restart has been attempted yet (min-delay does not block first restart). */
        long lastRestartAttemptMs = -1L;

        Registration(ManagedService service, ServiceRestartPolicy policy) {
            this.service = service;
            this.policy = policy;
        }
    }
}
