package org.openl.itest.core;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ClassMatcher;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * Simple wrapper for Jetty Server
 *
 * @author Vladyslav Pikus, Yury Molchan
 */
public class JettyServer {

    private final Server server;
    private final WebAppContext webAppContext;
    private final Locale defaultLocale = Locale.getDefault();
    private final TimeZone defaultTimeZone = TimeZone.getDefault();

    private JettyServer() {
        var webApp = new WebAppContext();
        webApp.setWar(System.getProperty("webservice-webapp"));
        // Fail the suite with the real deploy exception instead of serving HTTP 503 to every request
        webApp.setThrowUnavailableOnStartupException(true);
        webApp.setExtraClasspath(getExtraClasspath(webApp));
        // Solve issue with different slf4j implementations comes from dependencies
        webApp.addProtectedClassMatcher(new ClassMatcher("org.slf4j."));
        webApp.addProtectedClassMatcher(new ClassMatcher("-jakarta.activation."));

        webApp.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*/classes/.*" +
                "|.*ruleservice.ws[^/]*\\.jar$" + // For RuleService (ALL) which does not contain classes folder
                "|.*studio-ui[^/]*\\.jar$" + // For loading UI from the META-INF/resources in OpenL Studio
                "|.*studio-docs[^/]*\\.jar$"); // For the user guides OpenL Studio serves at /docs

        var httpServer = new Server(0);
        httpServer.setStopAtShutdown(true);
        httpServer.setHandler(webApp);

        this.webAppContext = webApp;
        this.server = httpServer;
    }

    private ArrayList<Resource> getExtraClasspath(WebAppContext context) {
        var resourceFactory = ResourceFactory.of(context);
        var classPath = new ArrayList<Resource>();
        var classes = Path.of("target/classes");
        if (Files.exists(classes)) {
            classPath.add(resourceFactory.newResource(classes.toUri()));
        }
        try (Stream<Path> stream = Files.walk(Path.of("libs"))) {

            classPath.addAll(stream.map(Path::toUri).map(resourceFactory::newResource).toList());
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

    // Closing the client stops Jetty, whose LifeCycle.stop() throws Exception.
    @SuppressWarnings("java:S112")
    public void test() throws Exception {
        var profile = this.webAppContext.getInitParams().get("spring.profiles.active");
        try (var client = start()) {
            client.test(profile == null ? "test-resources" : ("test-resources-" + profile));
        }
    }

    /**
     * Requires log4j-core in the webapp's {@code WEB-INF/lib}.
     *
     * <p>An incremental or {@code -Dquick} build can drop it, leaving a webapp that deploys with no logging at
     * all. Checking here turns that into one clear error before the server starts.
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
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(defaultTimeZone);
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
