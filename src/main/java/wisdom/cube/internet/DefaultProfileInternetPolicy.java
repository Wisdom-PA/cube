package wisdom.cube.internet;

/**
 * Stub: every profile may use internet when other gates pass (F5.T2.S3 placeholder).
 */
public final class DefaultProfileInternetPolicy implements ProfileInternetPolicy {

    @Override
    public boolean profileAllowsInternet(String profileId) {
        return profileId != null && !profileId.isBlank();
    }
}
