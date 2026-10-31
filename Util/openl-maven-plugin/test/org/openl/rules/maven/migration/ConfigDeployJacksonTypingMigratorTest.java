package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigDeployJacksonTypingMigratorTest {

    @Test
    void rewritesRemovedTypingSettings(@TempDir Path projectFolder) throws IOException {
        var file = projectFolder.resolve("rules-deploy.xml");
        Files.writeString(file, """
                <rules-deploy>
                    <serviceName>svc</serviceName>
                    <configuration>
                        <entry>
                            <string>jackson.simpleClassNameAsTypingPropertyValue</string>
                            <string>true</string>
                        </entry>
                    </configuration>
                </rules-deploy>
                """);

        var changed = new ConfigDeployJacksonTypingMigrator().migrate(projectFolder, null);

        assertEquals(List.of(file), changed);
        assertEquals("""
                <rules-deploy>
                    <serviceName>svc</serviceName>
                    <configuration>
                        <entry>
                            <string>jackson.jsonTypeInfoId</string>
                            <string>NAME</string>
                        </entry>
                    </configuration>
                </rules-deploy>
                """, Files.readString(file));
    }

    @Test
    void leavesCurrentSettingsUntouched(@TempDir Path projectFolder) throws IOException {
        var file = projectFolder.resolve("rules-deploy.xml");
        var content = """
                <rules-deploy>
                    <serviceName>svc</serviceName>
                    <configuration>
                        <entry>
                            <string>jackson.defaultTypingMode</string>
                            <string>NON_FINAL</string>
                        </entry>
                    </configuration>
                </rules-deploy>
                """;
        Files.writeString(file, content);

        var changed = new ConfigDeployJacksonTypingMigrator().migrate(projectFolder, null);

        assertTrue(changed.isEmpty());
        assertEquals(content, Files.readString(file));
    }
}
