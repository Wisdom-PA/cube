package wisdom.cube.internet;

/**
 * Per-profile internet rules (F5.T2.S3). Replace with profile store when F7 lands.
 */
public interface ProfileInternetPolicy {

    boolean profileAllowsInternet(String profileId);
}
