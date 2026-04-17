package wisdom.cube.gateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProfileApiErrorsTest {

    @Test
    void errorJsonBodiesContainCodes() {
        assertTrue(ProfileApiErrors.profileNotFoundJson().contains("PROFILE_NOT_FOUND"));
        assertTrue(ProfileApiErrors.profilePatchUnsupportedJson().contains("PROFILE_PATCH_UNSUPPORTED"));
        assertTrue(ProfileApiErrors.profilePatchInvalidJson().contains("PROFILE_PATCH_INVALID"));
    }
}
