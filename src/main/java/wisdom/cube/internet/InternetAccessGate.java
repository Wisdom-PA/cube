package wisdom.cube.internet;

/**
 * Whether a cloud LLM call may run for the current request (F5.T2, F5.T2.S4).
 */
public interface InternetAccessGate {

    boolean allowOnlineLlm(String profileId);
}
