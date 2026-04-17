package wisdom.cube.profile;

/**
 * Read-only profile list for tests that need PATCH to return {@code 501}.
 * {@link ProfileStore#patchDisplayName} uses the interface default (not supported).
 */
public final class FixtureProfileStore implements ProfileStore {

    private final MutableProfileStore delegate = new MutableProfileStore();

    @Override
    public String listProfilesJson() {
        return delegate.listProfilesJson();
    }

    @Override
    public boolean profileExists(String profileId) {
        return delegate.profileExists(profileId);
    }
}
