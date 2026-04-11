package wisdom.cube.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HttpServerGatewayExtractJsonTest {

    @Test
    void extractsDevicesArray() {
        String list = "{\"devices\":[{\"id\":\"a\"},{\"id\":\"b\"}]}";
        String array = HttpServerGateway.extractJsonArrayAfterKey(list, "devices");
        assertTrue(array.startsWith("["));
        assertTrue(array.contains("\"id\":\"a\""));
    }

    @Test
    void missingKeyReturnsEmptyArray() {
        assertEquals("[]", HttpServerGateway.extractJsonArrayAfterKey("{}", "devices"));
    }
}
