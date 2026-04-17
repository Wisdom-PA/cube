package wisdom.cube.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import wisdom.cube.util.JsonStrings;

/**
 * Default editable profile list aligned with the previous static {@code /profiles} payload.
 */
public final class MutableProfileStore implements ProfileStore {

    private final Object lock = new Object();
    private final List<ProfileEntry> entries;

    public MutableProfileStore() {
        this.entries = new ArrayList<>(List.of(
            new ProfileEntry("p1", "adult", "Adult"),
            new ProfileEntry("p2", "guest", "Guest")
        ));
    }

    @Override
    public String listProfilesJson() {
        synchronized (lock) {
            StringBuilder b = new StringBuilder();
            b.append("{\"profiles\":[");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    b.append(',');
                }
                appendEntry(b, entries.get(i));
            }
            b.append("]}");
            return b.toString();
        }
    }

    @Override
    public boolean profileExists(String profileId) {
        synchronized (lock) {
            return findIndex(profileId) >= 0;
        }
    }

    @Override
    public Optional<ProfileEntry> patchDisplayName(String profileId, String newDisplayName) {
        synchronized (lock) {
            int i = findIndex(profileId);
            if (i < 0) {
                return Optional.empty();
            }
            ProfileEntry cur = entries.get(i);
            ProfileEntry next = new ProfileEntry(cur.id(), cur.role(), newDisplayName);
            entries.set(i, next);
            return Optional.of(next);
        }
    }

    private int findIndex(String profileId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(profileId)) {
                return i;
            }
        }
        return -1;
    }

    private static void appendEntry(StringBuilder b, ProfileEntry e) {
        b.append("{\"id\":\"").append(JsonStrings.escape(e.id())).append("\",");
        b.append("\"role\":\"").append(JsonStrings.escape(e.role())).append("\",");
        b.append("\"display_name\":\"").append(JsonStrings.escape(e.displayName())).append("\"}");
    }

    /** JSON object matching {@code ProfileSummary} for PATCH 200 responses. */
    public static String toSummaryJson(ProfileEntry e) {
        return "{\"id\":\"" + JsonStrings.escape(e.id()) + "\",\"role\":\""
            + JsonStrings.escape(e.role()) + "\",\"display_name\":\""
            + JsonStrings.escape(e.displayName()) + "\"}";
    }
}
