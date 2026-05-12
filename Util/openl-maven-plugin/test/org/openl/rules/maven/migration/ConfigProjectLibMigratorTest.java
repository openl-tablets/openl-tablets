package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

class ConfigProjectLibMigratorTest {

    @Test
    void dropsLibJarWhenPackagingWontPopulateLib() {
        assertMigration(false,
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
    void keepsLibJarWhenPackagingWillPopulateLib() {
        assertUnchanged(true,
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="lib/*.jar"/>
                            </classpath>
                        </project>
                        """);
    }

    @Test
    void dropsOnlyTheLibJarEntryAndKeepsOthers() {
        // The migrator targets exactly lib/*.jar — other classpath entries (defaults or user-curated)
        // are left in place.
        assertMigration(false,
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="groovy/"/>
                                <entry path="lib/*.jar"/>
                                <entry path="custom/"/>
                            </classpath>
                        </project>
                        """,
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
    void doesNotTouchOtherLibPaths() {
        // Only the exact pattern lib/*.jar is removed; anything else under lib/ is user-curated.
        assertUnchanged(false,
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="lib/specific.jar"/>
                                <entry path="lib/sub/*.jar"/>
                            </classpath>
                        </project>
                        """);
    }

    @Test
    void dropsBackslashLibJarBecauseRuntimeNormalizesSeparators() {
        // ProjectDescriptor.processClasspathPathPatterns() normalizes '\' to '/' before matching,
        // so a Windows-style entry "lib\*.jar" is runtime-equivalent to "lib/*.jar" and must be
        // dropped on the same branch.
        assertMigration(false,
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
    void leavesRulesXmlWithoutClasspathUnchanged() {
        assertUnchanged(false,
                """
                        <project>
                            <name>p</name>
                        </project>
                        """);
    }

    @Test
    void leavesRulesXmlWithoutLibJarUnchanged() {
        assertUnchanged(false,
                """
                        <project>
                            <name>p</name>
                            <classpath>
                                <entry path="groovy/"/>
                            </classpath>
                        </project>
                        """);
    }

    /**
     * Parses {@code before} into a {@link ProjectDescriptor}, runs the migrator's transform with the
     * given {@code packagesLibDependencies} decision, marshals the result back to XML, and asserts it
     * equals {@code after}.
     */
    private static void assertMigration(boolean packagesLibDependencies, String before, String after) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(before.getBytes(StandardCharsets.UTF_8)));
        ConfigProjectLibMigrator.transform(descriptor, packagesLibDependencies);
        var actual = new String(descriptor.toBytes(), StandardCharsets.UTF_8);
        assertEquals(after, actual);
    }

    private static void assertUnchanged(boolean packagesLibDependencies, String content) {
        assertMigration(packagesLibDependencies, content, content);
    }
}
