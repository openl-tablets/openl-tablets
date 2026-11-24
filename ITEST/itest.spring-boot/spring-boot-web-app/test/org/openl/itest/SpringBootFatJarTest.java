package org.openl.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.util.TestSocketUtils;

/**
 * Runs the executable jar of Spring Boot, where the rules are archives nested into the executable jar.
 *
 * <p>Runs after the {@code package} phase, which builds the executable jar named by the {@code fat.jar} system
 * property. Without the property, as in the unit tests of the {@code test} phase, the test is skipped.
 *
 * @author Yury Molchan
 */
@EnabledIfSystemProperty(named = "fat.jar", matches = ".+", disabledReason = "No executable jar is built yet")
class SpringBootFatJarTest {

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);
    // A stalled response fails the test instead of blocking it, so the application is stopped
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static Process application;
    private static String baseUrl;

    @BeforeAll
    static void start() throws Exception {
        var port = TestSocketUtils.findAvailableTcpPort();
        baseUrl = "http://localhost:" + port;
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var jar = System.getProperty("fat.jar");
        var log = new File(jar + ".log");
        application = new ProcessBuilder(java, "-jar", jar, "--server.port=" + port)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start();
        await("The start of the application, see " + log).atMost(STARTUP_TIMEOUT)
                .pollInterval(Duration.ofMillis(500))
                .failFast("The application has stopped, see " + log, () -> !application.isAlive())
                .until(SpringBootFatJarTest::isReady);
    }

    private static boolean isReady() throws InterruptedException {
        try {
            return get("/admin/healthcheck/readiness").statusCode() == 200;
        } catch (IOException e) {
            return false; // not listening yet
        }
    }

    @AfterAll
    static void stop() throws InterruptedException {
        if (application != null) {
            application.destroy();
            if (!application.waitFor(30, TimeUnit.SECONDS)) {
                application.destroyForcibly();
            }
        }
    }

    @Test
    void deploysRulesOfNestedArchives() throws Exception {
        assertThat(get("/admin/ui/info").body())
                .contains("\"deploymentName\":\"itest.spring-boot.openl-rules")
                .contains("\"deploymentName\":\"rules-to-deploy-datasource\"");
    }

    @Test
    void ruleOfNestedJar() throws Exception {
        assertThat(post("/openl-rules-rs/SayHello", "Mister")).isEqualTo("Hello, Mister!");
    }

    @Test
    void ruleOfZipInClasses() throws Exception {
        assertThat(post("/deployed-rules/hello", "John Smith")).contains("Hello, John Smith");
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(REQUEST_TIMEOUT).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String post(String path, String body) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
