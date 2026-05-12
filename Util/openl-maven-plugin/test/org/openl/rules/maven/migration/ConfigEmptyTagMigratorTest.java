package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigEmptyTagMigratorTest {

    @Test
    void rewritesRulesXmlOnDiskWhenEmptyTagsPresent(@TempDir Path projectFolder) throws IOException {
        Path file = projectFolder.resolve("rules.xml");
        Files.writeString(file, """
                <project>
                    <name>x</name>
                    <comment></comment>
                    <modules>
                        <module>
                            <name>main</name>
                            <rules-root path="rules/Hello.xlsx"/>
                        </module>
                    </modules>
                    <classpath/>
                </project>
                """, StandardCharsets.UTF_8);

        List<Path> changed = new ConfigEmptyTagMigrator().migrate(projectFolder, null);

        assertEquals(1, changed.size());
        assertEquals(file, changed.getFirst());
        assertEquals("""
                <project>
                    <name>x</name>
                    <modules>
                        <module>
                            <name>main</name>
                            <rules-root path="rules/Hello.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void rewritesRulesDeployXmlOnDiskWhenEmptyTagsPresent(@TempDir Path projectFolder) throws IOException {
        Path file = projectFolder.resolve("rules-deploy.xml");
        Files.writeString(file, """
                <rules-deploy>
                    <serviceName>svc</serviceName>
                    <groups></groups>
                </rules-deploy>
                """, StandardCharsets.UTF_8);

        List<Path> changed = new ConfigEmptyTagMigrator().migrate(projectFolder, null);

        assertEquals(1, changed.size());
        assertEquals(file, changed.getFirst());
        assertEquals("""
                <rules-deploy>
                    <serviceName>svc</serviceName>
                </rules-deploy>
                """, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void rewritesBothFilesInOneMigratorRun(@TempDir Path projectFolder) throws IOException {
        Path rulesXml = projectFolder.resolve("rules.xml");
        Path deployXml = projectFolder.resolve("rules-deploy.xml");
        Files.writeString(rulesXml, """
                <project>
                    <name>x</name>
                    <comment></comment>
                </project>
                """, StandardCharsets.UTF_8);
        Files.writeString(deployXml, """
                <rules-deploy>
                    <serviceName>svc</serviceName>
                    <groups></groups>
                </rules-deploy>
                """, StandardCharsets.UTF_8);

        List<Path> changed = new ConfigEmptyTagMigrator().migrate(projectFolder, null);

        // Order matters: project file first (rules.xml roundtrip runs before rules-deploy.xml).
        assertEquals(List.of(rulesXml, deployXml), changed);
    }

    @Test
    void skipsWhenBothConfigFilesMissing(@TempDir Path projectFolder) throws IOException {
        List<Path> changed = new ConfigEmptyTagMigrator().migrate(projectFolder, null);

        assertEquals(List.of(), changed);
        assertFalse(Files.exists(projectFolder.resolve("rules.xml")));
        assertFalse(Files.exists(projectFolder.resolve("rules-deploy.xml")));
    }
}
