package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockServletContext;

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

    @Test
    void namesAPageThatCannotBeReadByItsFileAndGivesNoSize() {
        var context = new MockServletContext("classpath:org/openl/rules/webstudio/web/servlet/pages") {
            @Override
            public InputStream getResourceAsStream(String path) {
                return switch (path) {
                    case "/docs/openl-studio/index.md" -> null;
                    case "/docs/openl-studio/rules-editor.md" -> new InputStream() {
                        @Override
                        public int read() throws IOException {
                            throw new IOException("boom");
                        }
                    };
                    default -> super.getResourceAsStream(path);
                };
            }
        };

        var studio = UserGuides.read(context, "/docs/").contents().children().get(1);

        assertEquals("OpenL Studio", studio.title());
        assertEquals("openl-studio/index.md", studio.file());
        assertNull(studio.size());
        assertEquals(new UserGuides.Entry("Rules Editor", "openl-studio/rules-editor.md", null, List.of()),
                studio.children().getFirst());
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
