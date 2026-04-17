package wisdom.cube.profile;

import java.util.Objects;

/**
 * One household profile row for the cube HTTP API (F7 companion slice).
 */
public record ProfileEntry(String id, String role, String displayName) {

    public ProfileEntry {
        Objects.requireNonNull(id, "id");
        role = role == null ? "" : role;
        displayName = displayName == null ? "" : displayName;
    }
}
