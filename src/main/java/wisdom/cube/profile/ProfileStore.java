package wisdom.cube.profile;

import java.util.Optional;

/**
 * In-memory profiles for {@code GET /profiles} and {@code PATCH /profiles/{id}} (Phase 8 starter).
 */
public interface ProfileStore {

    String listProfilesJson();

    boolean profileExists(String profileId);

    default Optional<ProfileEntry> patchDisplayName(String profileId, String newDisplayName) {
        return Optional.empty();
    }
}
