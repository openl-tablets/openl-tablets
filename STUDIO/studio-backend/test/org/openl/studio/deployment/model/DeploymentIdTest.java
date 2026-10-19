package org.openl.studio.deployment.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeploymentIdTest {

    /** A Cyrillic name whose standard Base64 form carries a slash. */
    private static final String CYRILLIC_NAME = "Тарифный план";

    /** A Cyrillic name whose standard Base64 form carries a plus instead — the other half of the mapping. */
    private static final String CYRILLIC_NAME_WITH_PLUS = "Договор";

    @Test
    void encodeDecodeRoundTrips() {
        var id = new DeploymentId("production", "Proj");

        var decoded = DeploymentId.decode(id.encode());

        assertEquals("production", decoded.repository());
        assertEquals("Proj", decoded.name());
    }

    /**
     * A name outside US-ASCII is what makes the slash likely — see EPBDS-16403.
     */
    @ParameterizedTest
    @CsvSource({"r, ????",
                "production, " + CYRILLIC_NAME,
                "production, " + CYRILLIC_NAME_WITH_PLUS})
    void encodeStaysWithinOnePathSegment(String repository, String name) {
        var id = new DeploymentId(repository, name);

        var encoded = id.encode();

        assertFalse(encoded.contains("/"), "an id must not contain '/'");
        assertFalse(encoded.contains("+"), "an id must not contain '+'");
        assertEquals(id, DeploymentId.decode(encoded));
    }

    /**
     * An id issued before the URL-safe alphabet became the default still lives in bookmarks, so decoding must keep
     * accepting it.
     */
    @ParameterizedTest
    @CsvSource({"cjo/Pz8/, r, ????",
                // '/' → '_'
                "ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L/Qu9Cw0L0=, design, " + CYRILLIC_NAME,
                // '+' → '-', the other half of the mapping
                "ZGVzaWduOtCU0L7Qs9C+0LLQvtGA, design, " + CYRILLIC_NAME_WITH_PLUS})
    void decodeAcceptsAStandardId(String standardId, String repository, String name) {
        var decoded = DeploymentId.decode(standardId);

        assertEquals(repository, decoded.repository());
        assertEquals(name, decoded.name());
    }

    @Test
    void encodeReadsTheNameAsUtf8() {
        var id = new DeploymentId("design", CYRILLIC_NAME);

        assertEquals("ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L_Qu9Cw0L0=", id.encode());
    }

    @Test
    void decodeRefusesATextWithoutSeparator() {
        assertThrows(IllegalArgumentException.class, () -> DeploymentId.decode("cHJvZHVjdGlvbg"));
    }
}
