package wisdom.cube.supervisor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultServiceSupervisorTest {

    @Test
    void startAllWithNoRegistrations() throws Exception {
        new DefaultServiceSupervisor().startAll();
    }

    @Test
    void stopAllStopsInReverseRegistrationOrder() throws Exception {
        DefaultServiceSupervisor sup = new DefaultServiceSupervisor();
        List<String> stops = new ArrayList<>();
        sup.register(new StopRecordingService("a", stops), ServiceRestartPolicy.none());
        sup.register(new StopRecordingService("b", stops), ServiceRestartPolicy.none());
        sup.startAll();
        sup.stopAll();
        assertEquals(List.of("b", "a"), stops);
    }

    @Test
    void noFurtherRestartsWhenPolicyExhausted() throws Exception {
        DefaultServiceSupervisor sup = new DefaultServiceSupervisor();
        CountingService svc = new CountingService("gw");
        sup.register(svc, new ServiceRestartPolicy(1, 0L));
        sup.startAll();
        assertEquals(1, svc.starts.get());
        svc.healthy.set(false);
        sup.tickHealth();
        assertEquals(2, svc.starts.get());
        sup.tickHealth();
        assertEquals(2, svc.starts.get());
    }

    @Test
    void restartStartFailureIncrementsRestartCount() throws Exception {
        DefaultServiceSupervisor sup = new DefaultServiceSupervisor();
        FailingSecondStartService svc = new FailingSecondStartService();
        sup.register(svc, new ServiceRestartPolicy(3, 0L));
        sup.startAll();
        svc.healthyFlag = false;
        sup.tickHealth();
        assertEquals(2, svc.startCount());
    }

    @Test
    void startAllPropagatesFailures() {
        DefaultServiceSupervisor sup = new DefaultServiceSupervisor();
        sup.register(new BoomService("a"), ServiceRestartPolicy.none());
        sup.register(new OkService("b"), ServiceRestartPolicy.none());
        assertThrows(Exception.class, sup::startAll);
    }

    @Test
    void tickHealthRestartsUnhealthyServiceWithinPolicy() throws Exception {
        AtomicInteger clock = new AtomicInteger(0);
        DefaultServiceSupervisor sup = new DefaultServiceSupervisor(clock::get);
        CountingService svc = new CountingService("gw");
        sup.register(svc, new ServiceRestartPolicy(2, 10L));
        sup.startAll();
        svc.healthy.set(false);
        clock.set(100);
        sup.tickHealth();
        assertEquals(2, svc.starts.get());
        svc.healthy.set(true);
        sup.tickHealth();
        assertEquals(2, svc.starts.get());
    }

    @Test
    void tickHealthRespectsMinDelay() throws Exception {
        AtomicInteger clock = new AtomicInteger(0);
        DefaultServiceSupervisor sup = new DefaultServiceSupervisor(clock::get);
        CountingService svc = new CountingService("gw");
        sup.register(svc, new ServiceRestartPolicy(5, 50L));
        sup.startAll();
        svc.healthy.set(false);
        clock.set(0);
        sup.tickHealth();
        assertEquals(2, svc.starts.get());
        clock.set(10);
        sup.tickHealth();
        assertEquals(2, svc.starts.get());
        clock.set(60);
        sup.tickHealth();
        assertEquals(3, svc.starts.get());
    }

    private static final class BoomService implements ManagedService {
        private final String name;

        BoomService(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void start() throws Exception {
            throw new Exception("boom");
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean healthy() {
            return false;
        }
    }

    private static final class OkService implements ManagedService {
        private final String name;

        OkService(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean healthy() {
            return true;
        }
    }

    private static final class StopRecordingService implements ManagedService {
        private final String name;
        private final List<String> stops;

        StopRecordingService(String name, List<String> stops) {
            this.name = name;
            this.stops = stops;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            stops.add(name);
        }

        @Override
        public boolean healthy() {
            return true;
        }
    }

    private static final class FailingSecondStartService implements ManagedService {
        private int starts;
        private volatile boolean healthyFlag = true;

        @Override
        public String name() {
            return "fail2";
        }

        @Override
        public void start() throws Exception {
            starts++;
            if (starts >= 2) {
                throw new Exception("second start fails");
            }
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean healthy() {
            return healthyFlag;
        }

        int startCount() {
            return starts;
        }
    }

    private static final class CountingService implements ManagedService {
        private final String name;
        private final AtomicInteger starts = new AtomicInteger();
        private final AtomicBoolean healthy = new AtomicBoolean(true);

        CountingService(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void start() {
            starts.incrementAndGet();
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean healthy() {
            return healthy.get();
        }
    }
}
