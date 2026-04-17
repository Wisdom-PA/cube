package wisdom.cube.device;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceHealthSchedulerTest {

    @Test
    void rejectsNonPositivePeriod() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            new DeviceHealthScheduler(new NoOpDeviceDiscoveryService(), new InMemoryLightDeviceRegistry(), 0, TimeUnit.SECONDS));
        assertTrue(ex.getMessage().contains("period"), ex.getMessage());
    }

    @Test
    void periodicRunInvokesDiscovery() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DeviceDiscoveryService disc = registry -> {
            calls.incrementAndGet();
            return 0;
        };
        InMemoryLightDeviceRegistry registry = new InMemoryLightDeviceRegistry();
        DeviceHealthScheduler scheduler = new DeviceHealthScheduler(disc, registry, 100, TimeUnit.MILLISECONDS);
        scheduler.start();
        Thread.sleep(650);
        scheduler.stop();
        assertTrue(calls.get() >= 3, "expected at least 3 refresh ticks, got " + calls.get());
    }

    @Test
    void discoveryExceptionDoesNotStopFutureTicks() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DeviceDiscoveryService disc = registry -> {
            calls.incrementAndGet();
            if (calls.get() == 1) {
                throw new IllegalStateException("simulated integration failure");
            }
            return 0;
        };
        DeviceHealthScheduler scheduler =
            new DeviceHealthScheduler(disc, new InMemoryLightDeviceRegistry(), 30, TimeUnit.MILLISECONDS);
        scheduler.start();
        Thread.sleep(250);
        scheduler.stop();
        assertTrue(calls.get() >= 2, "expected a second tick after first failure, got " + calls.get());
    }
}
