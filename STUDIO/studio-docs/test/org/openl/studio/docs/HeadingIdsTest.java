package org.openl.studio.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeadingIdsTest {

    @Test
    void makesIdsTheWayGitHubDoes() {
        var ids = new HeadingIds();

        assertEquals("editing--testing", ids.next("Editing & Testing"));
        assertEquals("the-rulesxml-file", ids.next("The rules.xml File"));
        assertEquals("1-download-and-install", ids.next("1. Download and Install"));
        assertEquals("whats-inside-the-package", ids.next("What's Inside the Package?"));
        assertEquals("snake_case-and-kebab-case", ids.next("snake_case and kebab-case"));
        assertEquals("установка-openl", ids.next("Установка OpenL"));
    }

    @Test
    void numbersRepeatedHeadings() {
        var ids = new HeadingIds();

        assertEquals("example", ids.next("Example"));
        assertEquals("example-1", ids.next("Example"));
        assertEquals("example-2", ids.next("Example"));
        assertEquals("example-1-1", ids.next("Example 1"));
    }
}
