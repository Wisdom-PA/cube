package wisdom.cube.routine;

import java.util.List;
import java.util.Optional;

/**
 * In-memory view of routines for the HTTP API and future engine (F6.T4).
 */
public interface RoutineCatalog {

    List<RoutineDefinition> definitions();

    /**
     * Updates display name when supported (F6.T5). Default: not supported.
     */
    default Optional<RoutineDefinition> patchRoutineDisplayName(String routineId, String newName) {
        return Optional.empty();
    }

    /**
     * {@code RoutineList} body per {@code openapi/cube-app.yaml} (id + name only per entry).
     */
    default String listSummariesJson() {
        List<RoutineDefinition> defs = definitions();
        StringBuilder b = new StringBuilder();
        b.append("{\"routines\":[");
        for (int i = 0; i < defs.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            RoutineDefinition d = defs.get(i);
            b.append("{\"id\":\"")
                .append(RoutineJson.escape(d.routineId()))
                .append("\",\"name\":\"")
                .append(RoutineJson.escape(d.name()))
                .append("\"}");
        }
        b.append("]}");
        return b.toString();
    }
}
