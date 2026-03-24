package wisdom.cube;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CubeTest {

    @Test
    void cubeClassExists() {
        assertNotNull(Cube.class);
    }

    @Test
    void mainRunsWithoutError() {
        Cube.main(new String[0]);
    }
}
