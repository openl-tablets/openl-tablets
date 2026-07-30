package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * The {@code config.project.default-modules} migrator adds the project-name drop on top of the shared
 * module migration. This covers that extra step; the shared module transforms are covered by
 * {@code RulesXmlMigrationsDefaultModulesTest} in the core module, which OpenL Studio applies without the
 * name drop.
 */
class ConfigProjectDefaultModulesMigratorTest {

    @Test
    void dropsProjectNameWhenEqualsFolderName() {
        assertMigrationInFolder("my-project",
                """
                        <project>
                            <name>my-project</name>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void keepsProjectNameWhenDifferentFromFolder() {
        assertUnchangedInFolder("other-folder",
                """
                        <project>
                            <name>explicit-name</name>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void rewritesRulesXmlWhenAllDefaultsCanBeDropped() {
        assertMigrationInFolder("my-project",
                """
                        <project>
                            <name>my-project</name>
                            <modules>
                                <module>
                                    <name>A</name>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <name>B</name>
                                    <rules-root path="rules/B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                "<project/>\n");
    }

    private static void assertMigrationInFolder(String folderName, String before, String after) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(before.getBytes(StandardCharsets.UTF_8)));
        descriptor.setProjectFolder(Path.of(folderName));
        ConfigProjectDefaultModulesMigrator.transform(descriptor);
        var actual = new String(descriptor.toBytes(), StandardCharsets.UTF_8);
        assertEquals(after, actual);
    }

    private static void assertUnchangedInFolder(String folderName, String content) {
        assertMigrationInFolder(folderName, content, content);
    }
}
