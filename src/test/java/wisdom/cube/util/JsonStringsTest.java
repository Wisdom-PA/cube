package wisdom.cube.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonStringsTest {

    @Test
    void escapeQuotesAndBackslashes() {
        assertEquals("a\\\\b\\\"c", JsonStrings.escape("a\\b\"c"));
    }

    @Test
    void escapeNullReturnsEmpty() {
        assertEquals("", JsonStrings.escape(null));
    }

    @Test
    void newlinesBecomeSpaces() {
        assertEquals("a b", JsonStrings.escape("a\nb"));
    }
}
