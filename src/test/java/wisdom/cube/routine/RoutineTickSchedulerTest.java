package wisdom.cube.routine;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.logging.InMemoryBehaviourLogStore;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineTickSchedulerTest {

    @Test
    void rejectsNonPositivePeriod() {
        assertThrows(IllegalArgumentException.class, () ->
            new RoutineTickScheduler(
                Clock.systemUTC(),
                new FixtureRoutineCatalog(),
                new InMemoryLightDeviceRegistry(),
                new InMemoryBehaviourLogStore(),
                0L
            ));
    }

    @Test
    void periodicTickInvokesProcessor() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        RoutineCatalog cat = () -> {
            calls.incrementAndGet();
            return List.of();
        };
        RoutineTickScheduler s = new RoutineTickScheduler(
            Clock.systemUTC(),
            cat,
            new InMemoryLightDeviceRegistry(),
            new InMemoryBehaviourLogStore(),
            1L,
            TimeUnit.MILLISECONDS
        );
        s.start();
        Thread.sleep(250);
        s.stop();
        assertTrue(calls.get() >= 2, "expected multiple ticks, got " + calls.get());
    }

    @Test
    void processorExceptionDoesNotStopFutureTicks() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        RoutineCatalog cat = () -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                throw new IllegalStateException("simulated failure");
            }
            return List.of();
        };
        RoutineTickScheduler s = new RoutineTickScheduler(
            Clock.systemUTC(),
            cat,
            new InMemoryLightDeviceRegistry(),
            new InMemoryBehaviourLogStore(),
            30L,
            TimeUnit.MILLISECONDS
        );
        s.start();
        Thread.sleep(200);
        s.stop();
        assertTrue(calls.get() >= 2, "expected second tick after failure, got " + calls.get());
    }

    @Test
    void doubleStartIsIdempotent() {
        RoutineTickScheduler s = new RoutineTickScheduler(
            Clock.systemUTC(),
            new FixtureRoutineCatalog(),
            new InMemoryLightDeviceRegistry(),
            new InMemoryBehaviourLogStore(),
            1,
            TimeUnit.HOURS
        );
        s.start();
        s.start();
        s.stop();
    }
}
