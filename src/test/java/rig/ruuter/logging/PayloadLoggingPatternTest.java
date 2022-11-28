package rig.ruuter.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static rig.ruuter.logging.PayloadLoggingPattern.fromString;

class PayloadLoggingPatternTest {

    @Test
    void exactMatch() {
        assertPatternMatches("a.b.request", "a", "b", PayloadType.REQUEST);
        assertPatternNotMatches("a.b.request", "a", "a", PayloadType.REQUEST);
        assertPatternNotMatches("a.b.request", "b", "b", PayloadType.REQUEST);
        assertPatternNotMatches("a.b.request", "a", "b", PayloadType.RESPONSE);
    }

    @Test
    void wildcardConf() {
        assertPatternMatches("*.b.request", "a", "b", PayloadType.REQUEST);
        assertPatternMatches("*.b.request", "b", "b", PayloadType.REQUEST);
        assertPatternNotMatches("*.b.request", "a", "a", PayloadType.REQUEST);

        assertPatternMatches("*.*.REQUEST", "a", "a", PayloadType.REQUEST);
        assertPatternNotMatches("*.*.request", "a", "a", PayloadType.RESPONSE);
    }

    @Test
    void fromStringWildcards() {
        assertEquals(fromString("a"), fromString("a.*.*"));
        assertEquals(fromString("a.b"), fromString("a.b.*"));
        assertEquals(fromString("*"), fromString("*.*.*"));
        assertEquals(fromString("*.*"), fromString("*.*.*"));
        assertEquals(fromString("*.*.request"), fromString("*.*.request"));
    }

    private void assertPatternMatches(String pattern, String configurationCode, String destination, PayloadType request) {
        assertTrue(fromString(pattern)
            .matches(configurationCode, destination, request));
    }

    private void assertPatternNotMatches(String pattern, String configurationCode, String destination, PayloadType request) {
        assertFalse(fromString(pattern)
            .matches(configurationCode, destination, request));
    }
}
