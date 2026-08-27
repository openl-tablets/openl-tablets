package org.openl.itest;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class WebStudioTest {

    @Test
    void simple() throws Exception {
        seedLocalProject();
        try (var client = JettyServer.get().start()) {
            client.test("test-resources-simple");
        }
    }

    private static void seedLocalProject() throws IOException {
        var configured = System.getProperty("openl.home", "target/openl-test-${openl.start.milli}");
        var home = Path.of(configured.replace("${openl.start.milli}", String.valueOf(System.currentTimeMillis())));
        System.setProperty("openl.home", home.toString());
        LocalProjectFixture.seed(home.resolve("user-workspace").resolve("jdoe"),
                "EPBDS-15952-Project",
                Path.of("test-resources-simple", "EPBDS-15952", "EPBDS-15952-Project.zip"));
    }
}
