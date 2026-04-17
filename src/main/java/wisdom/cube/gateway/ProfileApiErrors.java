package wisdom.cube.gateway;

/**
 * Structured error bodies for profile routes; matches contract {@code ApiError}.
 */
public final class ProfileApiErrors {

    private ProfileApiErrors() {
    }

    public static String profileNotFoundJson() {
        return "{\"error\":{\"code\":\"PROFILE_NOT_FOUND\",\"message\":\"Unknown profile id\"}}";
    }

    public static String profilePatchUnsupportedJson() {
        return "{\"error\":{\"code\":\"PROFILE_PATCH_UNSUPPORTED\","
            + "\"message\":\"This profile store does not support edits\"}}";
    }

    public static String profilePatchInvalidJson() {
        return "{\"error\":{\"code\":\"PROFILE_PATCH_INVALID\",\"message\":\"Missing or empty display_name\"}}";
    }
}
