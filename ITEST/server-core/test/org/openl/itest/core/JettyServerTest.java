package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies the fail-fast behavior of {@link JettyServer}.
 *
 * <p>A webapp missing log4j-core, or whose context cannot deploy, must abort the suite with a clear error
 * instead of leaving Jetty answering every request with HTTP 503.
 *
 * @author Yury Molchan
 */
class JettyServerTest {

    private static final String VALID_WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                     version="6.0"/>
            """;

    // A listener class that cannot be loaded makes the servlet context fail to start.
    private static final String BROKEN_WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                     version="6.0">
                <listener>
                    <listener-class>org.openl.itest.core.NoSuchListener</listener-class>
                </listener>
            </web-app>
            """;

    @Test
    void startFailsWhenLog4jCoreIsMissing(@TempDir Path webapp) throws IOException {
        Files.createDirectories(webapp.resolve("WEB-INF/lib"));
        writeWebXml(webapp, VALID_WEB_XML);
        var error = startExpectingFailure(webapp);
        assertTrue(error.getMessage().contains("log4j-core"), "the error must name the missing dependency");
    }

    @Test
    void startFailsFastWhenTheContextCannotDeploy(@TempDir Path webapp) throws IOException {
        var lib = Files.createDirectories(webapp.resolve("WEB-INF/lib"));
        // A valid (empty) jar named log4j-core-* satisfies the dependency check, so the deploy failure below
        // is what aborts the start.
        try (var jar = new JarOutputStream(Files.newOutputStream(lib.resolve("log4j-core-2.26.1.jar")), new Manifest())) {
            jar.flush();
        }
        writeWebXml(webapp, BROKEN_WEB_XML);
        var error = startExpectingFailure(webapp);
        assertNotNull(error.getCause(), "the deploy cause must be preserved, not swallowed as a 503");
    }

    private static void writeWebXml(Path webapp, String content) throws IOException {
        Files.writeString(webapp.resolve("WEB-INF/web.xml"), content);
    }

    private static IllegalStateException startExpectingFailure(Path webapp) {
        var previous = System.setProperty("webservice-webapp", webapp.toString());
        try {
            return assertThrows(IllegalStateException.class, () -> JettyServer.get().start());
        } finally {
            if (previous == null) {
                System.clearProperty("webservice-webapp");
            } else {
                System.setProperty("webservice-webapp", previous);
            }
        }
    }
}
