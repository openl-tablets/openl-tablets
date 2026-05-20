package org.openl.rules.maven.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OpenLCoordinatesTest {

    @Test
    void artifactIdEqualsFolderNameWhenAtRoot() {
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/auto");
        var coords = OpenLCoordinates.of(anchor, project, "com.example", false);
        assertEquals("com.example", coords.groupId());
        assertEquals("auto", coords.artifactId());
    }

    @Test
    void deepPathProducesDottedGroupId() {
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/my/project/openl-project-name");
        var coords = OpenLCoordinates.of(anchor, project, "com.example", false);
        assertEquals("com.example.my.project", coords.groupId());
        assertEquals("openl-project-name", coords.artifactId());
    }

    @Test
    void singleLevelNesting() {
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/pricing/auto");
        var coords = OpenLCoordinates.of(anchor, project, "com.example", false);
        assertEquals("com.example.pricing", coords.groupId());
        assertEquals("auto", coords.artifactId());
    }

    @Test
    void invalidFolderNameIsRejected() {
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/bad+name");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> OpenLCoordinates.of(anchor, project, "com.example", false));
        assertEquals(true, ex.getMessage().contains("bad+name"));
    }

    @Test
    void invalidIntermediateSegmentIsRejected() {
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/with space/auto");
        assertThrows(IllegalArgumentException.class,
                () -> OpenLCoordinates.of(anchor, project, "com.example", false));
    }

    @Test
    void projectOutsideAnchorIsRejected() {
        var anchor = Path.of("/repo");
        var project = Path.of("/elsewhere/auto");
        assertThrows(IllegalArgumentException.class,
                () -> OpenLCoordinates.of(anchor, project, "com.example", false));
    }

    @Test
    void flattenGroupIdSkipsPathSegments() {
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/deep/nested/folder/auto");
        var coords = OpenLCoordinates.of(anchor, project, "com.example", true);
        assertEquals("com.example", coords.groupId(), "flatten mode uses the anchor groupId as-is");
        assertEquals("auto", coords.artifactId(), "artifactId still comes from the project folder");
    }

    @Test
    void flattenGroupIdSkipsIntermediateValidation() {
        // Intermediate folders carry characters that aren't valid in a Maven groupId, but flatten
        // mode never inspects them — so this should not throw.
        var anchor = Path.of("/repo");
        var project = Path.of("/repo/with space/auto");
        var coords = OpenLCoordinates.of(anchor, project, "com.example", true);
        assertEquals("com.example", coords.groupId());
        assertEquals("auto", coords.artifactId());
    }
}
