package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Yury Molchan
 */
class UserGuidesTest {

    @Test
    void takesTheTitleOfTheFrontMatterFirst() {
        assertEquals("Tutorials", UserGuides.titleOf("---\ntitle: Tutorials\ndescription: x\n---\n# Heading\n"));
        assertEquals("Quoted: Title", UserGuides.titleOf("---\ntitle: \"Quoted: Title\"\n---\n"));
        assertEquals("Single", UserGuides.titleOf("---\ntitle: 'Single'\n---\n"));
    }

    @Test
    void takesTheHeadingThePageStartsWith() {
        assertEquals("Heading", UserGuides.titleOf("---\ndescription: x\n---\n\n## Heading\n\nText\n"));
        assertEquals("Level 1", UserGuides.titleOf("\n  # Level 1\n"));
        assertEquals("Level 3", UserGuides.titleOf("### Level 3\n"));
    }

    @Test
    void takesNoTitleFromAnythingElse() {
        assertNull(UserGuides.titleOf("#### Level 4\n"));
        assertNull(UserGuides.titleOf("Text first.\n\n# Heading\n"));
        assertNull(UserGuides.titleOf("---\ntitle: Unclosed\n"));
        assertNull(UserGuides.titleOf(""));
    }

    @ParameterizedTest
    @CsvSource({
            "05-sso-saml, Sso Saml",
            "openl-studio, OpenL Studio",
            "editing-testing, Editing Testing",
            "1234-steps, 1234 Steps",
            "01, 01",
            "a1-x, A1 X",
            "-x, ' X'"
    })
    void makesATitleOfAName(String name, String title) {
        assertEquals(title, UserGuides.titleOfName(name));
    }
}
