package wisdom.cube.profile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MutableProfileStoreTest {

    @Test
    void patchDisplayNameUpdatesListJson() {
        MutableProfileStore store = new MutableProfileStore();
        Optional<ProfileEntry> p = store.patchDisplayName("p1", "Primary adult");
        assertTrue(p.isPresent());
        assertEquals("Primary adult", p.get().displayName());
        assertTrue(store.listProfilesJson().contains("Primary adult"));
    }

    @Test
    void patchUnknownReturnsEmpty() {
        MutableProfileStore store = new MutableProfileStore();
        assertTrue(store.patchDisplayName("missing", "X").isEmpty());
    }
}
