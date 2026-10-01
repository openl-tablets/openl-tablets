package org.openl.studio.docs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Holds the user guides the jar ships to what OpenL Studio and the documentation site both draw.
 *
 * @author Yury Molchan
 */
class UserGuidesTest {

    /** Where the jar keeps the guides, which OpenL Studio serves at {@code /docs}. */
    private static final String GUIDES = "/META-INF/resources/docs/index.md";

    /** Where the guides are written, so a problem names the file to fix. */
    private static final String SOURCES = "Docs/user-guides/";

    @Test
    void guidesHaveNoProblems() throws IOException, URISyntaxException {
        var root = Path.of(Objects.requireNonNull(getClass().getResource(GUIDES), GUIDES).toURI()).getParent();

        var problems = new GuideValidator(root, allowedLinks()).problems();

        assertTrue(problems.isEmpty(),
                () -> problems.stream().map(problem -> SOURCES + problem).collect(Collectors.joining("\n", "\n", "")));
    }

    /** The prefixes an external link may start with, one a line; a blank line and a {@code #} comment are skipped. */
    static List<String> allowedLinks() throws IOException {
        try (InputStream links = UserGuidesTest.class.getResourceAsStream("/allowed-links.txt")) {
            return new String(Objects.requireNonNull(links, "allowed-links.txt").readAllBytes(), UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        }
    }
}
