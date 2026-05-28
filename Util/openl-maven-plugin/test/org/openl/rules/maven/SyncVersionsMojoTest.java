package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SyncVersionsMojoTest {

    @Test
    void rewritesManagedZipPlaceholderToManagedVersion() {
        var xml = """
                <project>
                    <dependencies>
                        <dependency>
                            <name>core</name>
                            <mavenArtifact>com.example.domain:core:99.99.99</mavenArtifact>
                        </dependency>
                    </dependencies>
                </project>
                """;

        var result = SyncVersionsMojo.sync(xml, Map.of("com.example.domain:core", "0.0.0"));

        assertEquals(1, result.changed());
        assertTrue(result.content().contains("<mavenArtifact>com.example.domain:core:0.0.0</mavenArtifact>"));
        assertFalse(result.content().contains("99.99.99"));
    }

    @Test
    void leavesUnmanagedCoordinateUntouched() {
        var xml = "<mavenArtifact>org.apache.commons:commons-text:jar:1.15.0</mavenArtifact>";

        var result = SyncVersionsMojo.sync(xml, Map.of("com.example.domain:core", "0.0.0"));

        assertEquals(0, result.changed());
        assertEquals(xml, result.content());
    }

    @Test
    void preservesJarTypeWhenSyncingFourSegmentCoordinate() {
        var xml = "<mavenArtifact>com.example:lib:jar:1.0.0</mavenArtifact>";

        var result = SyncVersionsMojo.sync(xml, Map.of("com.example:lib", "2.0.0"));

        assertEquals(1, result.changed());
        assertEquals("<mavenArtifact>com.example:lib:jar:2.0.0</mavenArtifact>", result.content());
    }

    @Test
    void preservesTypeAndClassifierWhenSyncingFiveSegmentCoordinate() {
        var xml = "<mavenArtifact>com.example:lib:jar:tests:1.0.0</mavenArtifact>";

        var result = SyncVersionsMojo.sync(xml, Map.of("com.example:lib", "2.0.0"));

        assertEquals(1, result.changed());
        assertEquals("<mavenArtifact>com.example:lib:jar:tests:2.0.0</mavenArtifact>", result.content());
    }

    @Test
    void doesNotRewriteWhenAlreadyInSync() {
        var xml = "<mavenArtifact>com.example:lib:jar:2.0.0</mavenArtifact>";

        var result = SyncVersionsMojo.sync(xml, Map.of("com.example:lib", "2.0.0"));

        assertEquals(0, result.changed());
        assertEquals(xml, result.content());
    }

    @Test
    void syncsOnlyManagedEntriesAmongMany() {
        var xml = """
                <dependencies>
                    <dependency><mavenArtifact>com.example.domain:core:99.99.99</mavenArtifact></dependency>
                    <dependency><mavenArtifact>org.apache.commons:commons-text:jar:1.15.0</mavenArtifact></dependency>
                    <dependency><mavenArtifact>com.example:lib:jar:1.0.0</mavenArtifact></dependency>
                </dependencies>
                """;
        var managed = Map.of(
                "com.example.domain:core", "0.0.0",
                "com.example:lib", "2.0.0");

        var result = SyncVersionsMojo.sync(xml, managed);

        assertEquals(2, result.changed());
        assertTrue(result.content().contains("com.example.domain:core:0.0.0"));
        assertTrue(result.content().contains("com.example:lib:jar:2.0.0"));
        assertTrue(result.content().contains("org.apache.commons:commons-text:jar:1.15.0"),
                "an unmanaged entry sandwiched between managed ones must survive verbatim");
    }

    @Test
    void ignoresMalformedCoordinate() {
        var xml = "<mavenArtifact>not-a-coordinate</mavenArtifact>";

        var result = SyncVersionsMojo.sync(xml, Map.of("whatever", "1.0.0"));

        assertEquals(0, result.changed());
        assertEquals(xml, result.content());
    }

    @Test
    void preservesSurroundingFormattingAndComments() {
        var xml = """
                <project>
                    <!-- keep me -->
                    <dependencies>
                        <dependency>
                            <name>core</name>
                            <mavenArtifact>com.example.domain:core:99.99.99</mavenArtifact>
                        </dependency>
                    </dependencies>
                </project>
                """;

        var result = SyncVersionsMojo.sync(xml, Map.of("com.example.domain:core", "0.0.0"));

        assertTrue(result.content().contains("<!-- keep me -->"));
        assertTrue(result.content().contains("<name>core</name>"));
        assertTrue(result.content().trim().startsWith("<project>"));
    }
}
