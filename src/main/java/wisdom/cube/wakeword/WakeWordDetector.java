package wisdom.cube.wakeword;

/**
 * Wake-word path (F3.T2). Real engine replaces stubs on hardware.
 */
public interface WakeWordDetector {

    /**
     * Non-blocking poll: {@code true} when wake phrase detected since last poll.
     */
    boolean poll();
}
