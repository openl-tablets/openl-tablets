package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The patterns of a setting, matched whatever the letter case and the slashes around a path.
 *
 * @author Yury Molchan
 */
class PathPatternsTest {

    private static final PathPatterns PATTERNS = PathPatterns.of(" pom.xml , **/~$*,, **/target/**, /docs/ ");

    @ParameterizedTest
    @ValueSource(strings = {"pom.xml", "POM.XML", "/pom.xml", "rules/~$Rules.xlsx", "~$Rules.xlsx", "target",
            "target/", "p/target/Rules.xlsx", "docs", "/docs/"})
    void matchesThePathsThePatternsName(String path) {
        assertTrue(PATTERNS.matches(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "rules/pom.xml", "pom.xml.bak", "rules/Rules.xlsx", "targets/a", "docs/a.md"})
    void matchesNoOtherPath(String path) {
        assertFalse(PATTERNS.matches(path));
    }

    @Test
    void aBlankSettingListsNothing() {
        assertSame(PathPatterns.NONE, PathPatterns.of(null));
        assertSame(PathPatterns.NONE, PathPatterns.of(" "));
        assertFalse(PathPatterns.NONE.matches("pom.xml"));
    }
}
