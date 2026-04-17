package wisdom.cube.routine;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MutableRoutineCatalogTest {

    @Test
    void patchNameUpdatesDefinitionAndListJson() {
        MutableRoutineCatalog cat = new MutableRoutineCatalog();
        Optional<RoutineDefinition> r = cat.patchRoutineDisplayName("r1", "Night scene");
        assertTrue(r.isPresent());
        assertEquals("Night scene", r.get().name());
        List<RoutineDefinition> defs = cat.definitions();
        assertEquals("Night scene", defs.stream().filter(d -> "r1".equals(d.routineId())).findFirst().orElseThrow().name());
        assertTrue(cat.listSummariesJson().contains("Night scene"));
    }

    @Test
    void patchUnknownReturnsEmpty() {
        MutableRoutineCatalog cat = new MutableRoutineCatalog();
        assertTrue(cat.patchRoutineDisplayName("missing", "X").isEmpty());
    }
}
