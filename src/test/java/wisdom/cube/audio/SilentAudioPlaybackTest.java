package wisdom.cube.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SilentAudioPlaybackTest {

    @Test
    void volumeClamped() {
        SilentAudioPlayback p = new SilentAudioPlayback();
        p.setVolume(2.0f);
        assertEquals(1.0f, p.getVolume(), 0.001f);
        p.setVolume(-1.0f);
        assertEquals(0.0f, p.getVolume(), 0.001f);
    }
}
