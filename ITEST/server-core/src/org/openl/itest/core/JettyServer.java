package org.openl.itest.core;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ClassMatcher;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Simple wrapper for Jetty Server
 *
 * @author Vladyslav Pikus, Yury Molchan
 */
public class JettyServer {

    private final Server server;
    private final WebAppContext webAppContext;
    private final Locale DEFAULT_LOCALE = Locale.getDefault();
    private final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    private JettyServer() {
        var webAppContext = new WebAppContext();
        webAppContext.setWar(System.getProperty("webservice-webapp"));
        // Fail the suite with the real deploy exception instead of serving HTTP 503 to every request
        webAppContext.setThrowUnavailableOnStartupException(true);
        webAppContext.setExtraClasspath(getExtraClasspath(webAppContext));
        // Solve issue with different slf4j implementations comes from dependencies
        webAppContext.addProtectedClassMatcher(new ClassMatcher("org.slf4j."));
        webAppContext.addProtectedClassMatcher(new ClassMatcher("-jakarta.activation."));

        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*/classes/.*" +
                "|.*ruleservice.ws[^/]*\\.jar$" + // For RuleService (ALL) which does not contain classes folder
                "|.*studio-ui[^/]*\\.jar$" + // For loading UI from the META-INF/resources in OpenL Studio
                "|.*jakarta\\.faces[^/]*\\.jar$"); // Mojarra Injection SPI for JSF in OpenL Studio

        var server = new Server(0);
        server.setStopAtShutdown(true);
        server.setHandler(webAppContext);

        this.webAppContext = webAppContext;
        this.server = server;
    }

    private ArrayList<Resource> getExtraClasspath(WebAppContext context) {
        var classPath = new ArrayList<Resource>();
        var classes = Path.of("target/classes");
        if (Files.exists(classes)) {
            classPath.add(context.newResource(classes.toUri()));
        }
        try (Stream<Path> stream = Files.walk(Path.of("libs"))) {

            classPath.addAll(stream.map(Path::toUri).map(context::newResource).collect(Collectors.toList()));
        } catch (IOException ignored) {
            // ignore
        }

        return classPath.isEmpty() ? null : classPath;
    }

    public static JettyServer get() {
        return new JettyServer();
    }

    public JettyServer withInitParam(Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            webAppContext.getInitParams().putAll(params);
        }
        return this;
    }

    public JettyServer withInitParam(String key, String value) {
        webAppContext.getInitParams().put(key, value);
        return this;
    }

    public JettyServer withProfile(String profile) {
        return withInitParam("spring.profiles.active", profile);
    }

    /**
     * Deploys the webapp under a context path instead of the server root.
     *
     * <p>A request for the context path itself, without the trailing slash, is handed to the webapp as it is. That
     * is what a container does when the webapp maps a servlet to {@code /*}: the servlet is called with no path
     * info instead of the container answering a redirect of its own.
     */
    public JettyServer withContextPath(String contextPath) {
        webAppContext.setContextPath(contextPath);
        webAppContext.setAllowNullPathInContext(true);
        return this;
    }

    public void test() throws Exception {
        var profile = this.webAppContext.getInitParams().get("spring.profiles.active");
        try (var client = start()) {
            client.test(profile == null ? "test-resources" : ("test-resources-" + profile));
        }
    }

    /**
     * Requires log4j-core in the webapp's {@code WEB-INF/lib}.
     *
     * <p>An incremental or {@code -Dquick} build can drop it, leaving a webapp that deploys with no logging
     * and, in OpenL Studio, fails with a cryptic RichFaces NPE. Checking here turns that into one clear
     * error before the server starts.
     */
    private void requireLog4jCore() {
        var lib = Path.of(webAppContext.getWar(), "WEB-INF", "lib");
        try (var jars = Files.list(lib)) {
            if (jars.anyMatch(jar -> jar.getFileName().toString().startsWith("log4j-core-"))) {
                return;
            }
        } catch (IOException ignored) {
            // A missing or unreadable WEB-INF/lib means log4j-core is absent too.
        }
        throw new IllegalStateException(
                "The webapp under test has no log4j-core in " + lib + ". Rebuild the webapp module without -Dquick.");
    }

    void stop() throws Exception {
        try {
            server.stop();
            server.destroy();
        } finally {
            Locale.setDefault(DEFAULT_LOCALE);
            TimeZone.setDefault(DEFAULT_TIMEZONE);
        }
    }

    public HttpClient start() {
        requireLog4jCore();
        Locale.setDefault(Locale.US);
        // set -10 as default
        TimeZone.setDefault(TimeZone.getTimeZone("America/Adak"));
        try {
            this.server.start();
        } catch (Exception e) {
            try {
                stop();
            } catch (Exception suppressed) {
                e.addSuppressed(suppressed);
            }
            throw new IllegalStateException("The webapp failed to deploy.", e);
        }
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        var httpClient = new HttpClient(this, URI.create("http://localhost:" + port));
        var readiness = System.getProperty("readiness-url", "");
        if (!readiness.isBlank()) {
            httpClient.tryWaitOK(readiness);
        }

        return httpClient;
    }

    /**
     * Starts Jetty Server and executes a set of http requests.
     */
    public static void test(String profile) throws Exception {
        JettyServer.get()
                .withProfile(profile)
                .test();
    }

}
