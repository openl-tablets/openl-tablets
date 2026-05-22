package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

class ConfigProjectClasspathMigratorTest {

    @Test
    void dropsClasspathWhenOnlyGroovyTrailingSlash() {
        assertMigration(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="groovy/"/>
                            </classpath>
                        </project>
                        """,
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    @Test
    void dropsClasspathWhenOnlyGroovyNoSlash() {
        assertMigration(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="groovy"/>
                            </classpath>
                        </project>
                        """,
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    @Test
    void dropsClasspathWhenOnlyLibJar() {
        assertMigration(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="lib/*.jar"/>
                            </classpath>
                        </project>
                        """,
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    @Test
    void dropsClasspathWhenBackslashLibJar() {
        // ProjectDescriptor.processClasspathPathPatterns() normalises '\' to '/' before matching,
        // so "lib\*.jar" is runtime-equivalent to "lib/*.jar" and must be dropped on the same branch.
        assertMigration(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="lib\\*.jar"/>
                            </classpath>
                        </project>
                        """,
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    @Test
    void dropsClasspathWhenEveryEntryIsADefault() {
        // Any combination of the default paths still drops the whole block.
        assertMigration(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="groovy/"/>
                                <entry path="groovy"/>
                                <entry path="lib/*.jar"/>
                            </classpath>
                        </project>
                        """,
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    @Test
    void keepsClasspathWhenAnyEntryIsNonDefault() {
        // A single non-default entry keeps the whole block — the migrator never drops user-curated paths.
        assertUnchanged(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="groovy/"/>
                                <entry path="custom/"/>
                            </classpath>
                        </project>
                        """);
    }

    @Test
    void keepsClasspathWhenLibJarMixedWithNonDefault() {
        // The migrator never selectively removes default entries — a single non-default entry keeps
        // the whole block, including the now-default lib/*.jar alongside it.
        assertUnchanged(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="lib/*.jar"/>
                                <entry path="custom/"/>
                            </classpath>
                        </project>
                        """);
    }

    @Test
    void keepsClasspathWhenOnlyNonDefaultEntry() {
        assertUnchanged(
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="custom/"/>
                            </classpath>
                        </project>
                        """);
    }

    @Test
    void leavesRulesXmlWithoutClasspathUnchanged() {
        assertUnchanged(
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    /**
     * Parses {@code before} into a {@link ProjectDescriptor}, runs the migrator's transform, marshals
     * the result back to XML, and asserts it equals {@code after}.
     */
    private static void assertMigration(String before, String after) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(before.getBytes(StandardCharsets.UTF_8)));
        ConfigProjectClasspathMigrator.transform(descriptor);
        var actual = new String(descriptor.toBytes(), StandardCharsets.UTF_8);
        assertEquals(after, actual);
    }

    private static void assertUnchanged(String content) {
        assertMigration(content, content);
    }
}
