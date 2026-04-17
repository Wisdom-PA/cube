package wisdom.cube.routine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureRoutineCatalogTest {

    private static final String LEGACY_LIST =
        "{\"routines\":["
            + "{\"id\":\"r1\",\"name\":\"Evening lights\"},"
            + "{\"id\":\"r2\",\"name\":\"Good morning\"}"
            + "]}";

    @Test
    void defaultCatalogMatchesLegacyListJson() {
        FixtureRoutineCatalog cat = new FixtureRoutineCatalog();
        assertEquals(LEGACY_LIST, cat.listSummariesJson());
    }

    @Test
    void defaultDefinitionsHaveTriggersAndActions() {
        FixtureRoutineCatalog cat = new FixtureRoutineCatalog();
        assertEquals(2, cat.definitions().size());
        RoutineDefinition first = cat.definitions().get(0);
        assertEquals("r1", first.routineId());
        assertTrue(first.triggers().stream().anyMatch(t -> t.kind() == RoutineTriggerKind.TIME));
        assertTrue(first.actions().stream().anyMatch(a -> a.kind() == RoutineActionKind.DEVICE_STATE));
        RoutineDefinition second = cat.definitions().get(1);
        assertTrue(second.conditions().stream().anyMatch(c -> c.kind() == RoutineConditionKind.TIME_WINDOW));
    }
}
