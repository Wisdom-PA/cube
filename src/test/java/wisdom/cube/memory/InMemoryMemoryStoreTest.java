package wisdom.cube.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMemoryStoreTest {

    @Test
    void rememberRecallForget() {
        InMemoryMemoryStore m = new InMemoryMemoryStore();
        m.remember("p1", "k", "v");
        assertEquals("v", m.recall("p1", "k").orElseThrow());
        m.forget("p1", "k");
        assertTrue(m.recall("p1", "k").isEmpty());
    }

    @Test
    void forgetProfile() {
        InMemoryMemoryStore m = new InMemoryMemoryStore();
        m.remember("p1", "a", "1");
        m.remember("p2", "a", "2");
        m.forgetProfile("p1");
        assertTrue(m.recall("p1", "a").isEmpty());
        assertEquals("2", m.recall("p2", "a").orElseThrow());
    }
}
