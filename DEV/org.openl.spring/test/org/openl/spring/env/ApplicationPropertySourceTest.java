package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Checks which files of a configuration folder an application reads its settings from.
 *
 * @author Yury Molchan
 */
class ApplicationPropertySourceTest {

    @TempDir
    private Path folder;

    /** An application without a name, deployed at the root context, reads no file named after an application. */
    @Test
    void readsNoFileOfAnotherNameWhenTheApplicationHasNoName() throws IOException {
        Files.writeString(folder.resolve("application.properties"), "shared = yes");
        Files.writeString(folder.resolve("notes.properties"), "project.history.count = 5");

        var source = source("");

        assertEquals("yes", source.getProperty("shared"));
        assertNull(source.getProperty("project.history.count"));
    }

    @Test
    void readsTheFileNamedAfterTheApplication() throws IOException {
        Files.writeString(folder.resolve("webstudio.properties"), "project.history.count = 5");
        Files.writeString(folder.resolve("notes.properties"), "stray = yes");

        var source = source("webstudio");

        assertEquals("5", source.getProperty("project.history.count"));
        assertNull(source.getProperty("stray"));
    }

    private ApplicationPropertySource source(String appName) {
        var environment = new StandardEnvironment();
        environment.getPropertySources()
                .addFirst(new MapPropertySource("test",
                        Map.of("openl.config.location", folder.toUri().toString(),
                                "openl.config.name", "application.properties, {appName}.properties")));
        return new ApplicationPropertySource(environment, appName);
    }
}
