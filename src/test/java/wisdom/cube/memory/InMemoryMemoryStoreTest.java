package wisdom.cube.memory;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

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

    @Test
    void forgetKeyPrefixNullIsNoOp() {
        InMemoryMemoryStore m = new InMemoryMemoryStore();
        m.remember("p1", "k", "v");
        m.forgetKeyPrefix("p1", null);
        assertEquals("v", m.recall("p1", "k").orElseThrow());
    }

    @Test
    void forgetKeyPrefixRemovesMatchingKeysOnly() {
        InMemoryMemoryStore m = new InMemoryMemoryStore();
        m.remember("p1", "ctx:room1", "a");
        m.remember("p1", "ctx:room2", "b");
        m.remember("p1", "other", "c");
        m.forgetKeyPrefix("p1", "ctx:");
        assertTrue(m.recall("p1", "ctx:room1").isEmpty());
        assertTrue(m.recall("p1", "ctx:room2").isEmpty());
        assertEquals("c", m.recall("p1", "other").orElseThrow());
    }

    @Test
    void forgetDayRemovesOnlyThatPrefix() {
        InMemoryMemoryStore m = new InMemoryMemoryStore();
        LocalDate d = LocalDate.of(2026, 4, 11);
        m.rememberForDay("p1", d, "a", "1");
        m.rememberForDay("p1", d, "b", "2");
        m.remember("p1", "other", "x");
        m.forgetDay("p1", d);
        assertTrue(m.recall("p1", "mem/2026-04-11/a").isEmpty());
        assertEquals("x", m.recall("p1", "other").orElseThrow());
    }

    @Test
    void exportProfileReturnsLogicalKeys() {
        InMemoryMemoryStore m = new InMemoryMemoryStore();
        m.remember("p1", "k1", "v1");
        m.remember("p1", "k2", "v2");
        m.remember("p2", "k1", "other");
        Map<String, String> out = m.exportProfile("p1");
        assertEquals(2, out.size());
        assertEquals("v1", out.get("k1"));
        assertEquals("v2", out.get("k2"));
    }
}
