package org.openl.itest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

class WebStudioTest {

    @Test
    void demo() throws Exception {
        seedDemoSettings();
        try (var client = JettyServer.get().start()) {
            staysReadyAfterStartUp(client);
            client.test("test-resources");
        }
    }

    /**
     * Writes the settings file the DEMO start script leaves for a first start, and points the application at
     * the home holding it.
     *
     * <p>The build gives every start a home of its own, naming it after the moment the application starts. A
     * suite that has to put something into the home before the start names it itself instead.
     */
    private static void seedDemoSettings() throws IOException {
        var configured = System.getProperty("openl.home", "target/openl-test-${openl.start.milli}");
        var home = Path.of(configured.replace("${openl.start.milli}", String.valueOf(System.currentTimeMillis())));
        Files.createDirectories(home);
        // The settings file is named after the context path the application answers on, and this suite answers
        // on the server root. The DEMO package answers on /webstudio and names the very same file after it.
        Files.writeString(home.resolve(".properties"), ".version=LATEST\ndemo.init=true\n");
        System.setProperty("openl.home", home.toString());
    }

    /**
     * Requires OpenL Studio to stay ready once it has started.
     *
     * <p>The demo initialization stores settings of its own, and settings stored by anyone else are a
     * configuration change the application reloads a second later. Reloading its own write costs a restart of
     * the whole Spring context, and the restart used to race the start-up of the web application and fail it,
     * leaving every page answering 503 (EPBDS-16473).
     */
    private static void staysReadyAfterStartUp(HttpClient client) throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            client.getForObject("/healthcheck/readiness", String.class, 200);
            Thread.sleep(100);
        }
    }
}
