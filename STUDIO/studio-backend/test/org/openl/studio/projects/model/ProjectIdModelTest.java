package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProjectIdModelTest {

    /** A Cyrillic name whose standard Base64 form carries a slash. */
    private static final String CYRILLIC_NAME = "Тарифный план";

    /** A Cyrillic name whose standard Base64 form carries a plus instead — the other half of the mapping. */
    private static final String CYRILLIC_NAME_WITH_PLUS = "Договор";

    @Test
    void encodeDecodeRoundTrips() {
        var id = ProjectIdModel.builder().repository("design-flat").projectName("Proj").build();

        var decoded = ProjectIdModel.decode(id.encode());

        assertEquals("design-flat", decoded.getRepository());
        assertEquals("Proj", decoded.getProjectName());
    }

    /**
     * A project and a deployment configuration are addressed by the same id, and a name outside US-ASCII is
     * what makes the slash likely — see EPBDS-16402 and EPBDS-16403.
     */
    @ParameterizedTest
    @CsvSource({"r, ????",
                "design, " + CYRILLIC_NAME,
                "deployment, " + CYRILLIC_NAME,
                "design, " + CYRILLIC_NAME_WITH_PLUS})
    void encodeStaysWithinOnePathSegment(String repository, String projectName) {
        var id = ProjectIdModel.builder().repository(repository).projectName(projectName).build();

        var encoded = id.encode();

        assertFalse(encoded.contains("/"), "an id must not contain '/'");
        assertFalse(encoded.contains("+"), "an id must not contain '+'");
        assertEquals(id, ProjectIdModel.decode(encoded));
    }

    /**
     * An id issued before the URL-safe alphabet became the default still lives in bookmarks and in legacy
     * pages that build one in the browser, so decoding must keep accepting it.
     */
    @ParameterizedTest
    @CsvSource({"cjo/Pz8/, r, ????",
                // '/' → '_'
                "ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L/Qu9Cw0L0=, design, " + CYRILLIC_NAME,
                // '+' → '-', the other half of the mapping
                "ZGVzaWduOtCU0L7Qs9C+0LLQvtGA, design, " + CYRILLIC_NAME_WITH_PLUS})
    void decodeAcceptsAStandardId(String standardId, String repository, String projectName) {
        var decoded = ProjectIdModel.decode(standardId);

        assertEquals(repository, decoded.getRepository());
        assertEquals(projectName, decoded.getProjectName());
    }

    @Test
    void encodeReadsTheNameAsUtf8() {
        var id = ProjectIdModel.builder().repository("design").projectName(CYRILLIC_NAME).build();

        assertEquals("ZGVzaWduOtCi0LDRgNC40YTQvdGL0Lkg0L_Qu9Cw0L0=", id.encode());
    }
}
