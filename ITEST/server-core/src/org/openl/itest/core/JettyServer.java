package org.openl.itest.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Simple wrapper for Jetty Server
 *
 * @author Vladyslav Pikus, Yury Molchan
 */
public class JettyServer {

    private final Server server;

    private JettyServer(String explodedWar, boolean sharedClassloader, boolean useWebXml, String[] profiles) throws IOException {
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
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), false, false, profiles);
        jetty.server.start();
        return jetty;
    }

    /**
     * Start an application with configuration defined using {@code @WebListener} and sharing JUnit classloader with the
     * application.
     */
    public static JettyServer startSharingClassLoader() throws Exception {
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), true, false, null);
        jetty.server.start();
        return jetty;
    }

    /**
     * Temporary for WebStudio only! Start an application with configuration defined in web.xml.
     *
     * @param profiles Spring profiles which are activated
     */
    public static JettyServer startWithWebXml(String... profiles) throws Exception {
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), false, true, profiles);
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
    public static void test(String profile) throws Exception {
        String[] profiles = profile == null ? new String[0] : new String[] { profile };
        String testFolder = profile == null ? "test-resources" : ("test-resources-" + profile);
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), false, false, profiles);

        final Locale DEFAULT_LOCALE = Locale.getDefault();
        final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);

            // set +2 as default
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Helsinki"));

            jetty.server.start();
            try {
                jetty.client().test(testFolder);
            } finally {
                jetty.stop();
            }
        } finally {
            Locale.setDefault(DEFAULT_LOCALE);
            TimeZone.setDefault(DEFAULT_TIMEZONE);
        }
    }

    public static void test() throws Exception {
        test(null);
    }

}
