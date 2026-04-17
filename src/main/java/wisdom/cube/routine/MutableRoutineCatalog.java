package wisdom.cube.routine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Editable in-memory routine catalog for dev gateway (F6.T5): display names can be patched while
 * triggers/actions stay fixed until a fuller persistence model exists.
 */
public final class MutableRoutineCatalog implements RoutineCatalog {

    private final Object lock = new Object();
    private final List<RoutineDefinition> definitions;

    public MutableRoutineCatalog() {
        this.definitions = new ArrayList<>(FixtureRoutineCatalog.defaultDefinitions());
    }

    @Override
    public List<RoutineDefinition> definitions() {
        synchronized (lock) {
            return List.copyOf(definitions);
        }
    }

    @Override
    public Optional<RoutineDefinition> patchRoutineDisplayName(String routineId, String newName) {
        synchronized (lock) {
            for (int i = 0; i < definitions.size(); i++) {
                RoutineDefinition d = definitions.get(i);
                if (d.routineId().equals(routineId)) {
                    RoutineDefinition next = new RoutineDefinition(
                        d.routineId(),
                        newName,
                        d.ownerProfileId(),
                        d.editorProfileIds(),
                        d.triggers(),
                        d.conditions(),
                        d.actions()
                    );
                    definitions.set(i, next);
                    return Optional.of(next);
                }
            }
            return Optional.empty();
        }
    }
}
