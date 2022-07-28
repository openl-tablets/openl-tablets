package org.openl.itest.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.fail;

/**
 * Simple wrapper for Jetty Server
 *
 * @author Vladyslav Pikus, Yury Molchan
 */
public class JettyServer {

    private static final long MAX_READINESS_WAIT_TIMEOUT_MS = 60 * 1000;

    private final Server server;

    private JettyServer(String explodedWar, boolean sharedClassloader, String[] profiles) throws IOException {
        this.server = new Server(0);
        this.server.setStopAtShutdown(true);
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setResourceBase(explodedWar);
        webAppContext.setExtraClasspath(getExtraClasspath());

        if (profiles != null && profiles.length > 0) {
            webAppContext.setInitParameter("spring.profiles.active", String.join(",", profiles));
        }

        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*/classes/.*" +
                "|.*ruleservice.ws[^/]*\\.jar$" + // For RuleService (ALL) which does not contain classes folder
                "|.*javax\\.faces[^/]*\\.jar$"); // Mojarra Injection SPI for JSF in WebStudio


        if (sharedClassloader) {
            webAppContext.setClassLoader(JettyServer.class.getClassLoader());
        }
        this.server.setHandler(webAppContext);
    }

    private String getExtraClasspath() {
        try (Stream<Path> stream = Files.walk(Paths.get("libs"))) {
            return stream.map(Path::toString).collect(Collectors.joining(","));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Start an application with configuration defined using {@code @WebListener}.
     *
     * @param profiles Spring profiles which are activated
     */
    public static JettyServer start(String... profiles) throws Exception {
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), false, profiles);
        jetty.server.start();
        return jetty;
    }

    /**
     * Start an application with configuration defined using {@code @WebListener} and sharing JUnit classloader with the
     * application.
     */
    public static JettyServer startSharingClassLoader() throws Exception {
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), true, null);
        jetty.server.start();
        return jetty;
    }

    public void stop() throws Exception {
        server.stop();
    }

    public HttpClient client() {
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        try {
            URL url = new URL("http", "localhost", port, "");
            return HttpClient.create(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Starts Jetty Server and executes a set of http requests.
     */
    public static void test(String profile, boolean waitUntilReady) throws Exception {
        String[] profiles = profile == null ? new String[0] : new String[] { profile };
        String testFolder = profile == null ? "test-resources" : ("test-resources-" + profile);
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), false, profiles);

        final Locale DEFAULT_LOCALE = Locale.getDefault();
        final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);

            // set +2 as default
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Helsinki"));

            jetty.server.start();
            try {
                var httpClient = jetty.client();
                if (waitUntilReady) {
                    boolean ready = false;
                    long started = System.currentTimeMillis();
                    while (!ready && (started - System.currentTimeMillis()) < MAX_READINESS_WAIT_TIMEOUT_MS) {
                        try {
                            httpClient.getForObject("/admin/healthcheck/readiness", String.class, HttpStatus.OK);
                            ready = true;
                        } catch (AssertionError ignored) {
                            System.out.println("Not ready yet. Wait 500 ms and retry");
                            TimeUnit.MILLISECONDS.sleep(500);
                        }
                    }
                    if (!ready) {
                        fail("Not Ready!");
                    }
                }
                httpClient.test(testFolder);
            } finally {
                jetty.stop();
            }
        } finally {
            Locale.setDefault(DEFAULT_LOCALE);
            TimeZone.setDefault(DEFAULT_TIMEZONE);
        }
    }

    public static void test(String profile) throws Exception {
        test(profile, false);
    }

    public static void test() throws Exception {
        test(null);
    }

}
