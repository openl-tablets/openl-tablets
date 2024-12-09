package org.openl.itest.core;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.ClassMatcher;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

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

    private JettyServer() throws IOException {
        var webAppContext = new WebAppContext();
        webAppContext.setResourceBase(System.getProperty("webservice-webapp"));
        webAppContext.setExtraClasspath(getExtraClasspath());
        // Solve issue with different slf4j implementations comes from dependencies
        webAppContext.addSystemClassMatcher(new ClassMatcher("org.slf4j."));
        webAppContext.addSystemClassMatcher(new ClassMatcher("-javax.activation."));

        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*/classes/.*" +
                "|.*ruleservice.ws[^/]*\\.jar$" + // For RuleService (ALL) which does not contain classes folder
                "|.*javax\\.faces[^/]*\\.jar$"); // Mojarra Injection SPI for JSF in OpenL Studio

        webAppContext.setClassLoader(new WebAppClassLoader(webAppContext) {
            @Override
            public void addClassPath(String classPath) throws IOException {
                // exclude outdated Oracle JDBC Drive from classpath because it's challenging to use with testcontainers
                if (classPath.contains("ojdbc6.jar")) {
                    System.out.println("Excluding ojdbc6.jar from classpath: " + classPath);
                    return; // Skip adding this JAR
                }
                super.addClassPath(classPath);
            }
        });

        var server = new Server(0);
        server.setStopAtShutdown(true);
        server.setHandler(webAppContext);

        this.webAppContext = webAppContext;
        this.server = server;
    }

    private String getExtraClasspath() {
        var classPath = new ArrayList<String>();
        var classes = Paths.get("target/classes");
        if (Files.exists(classes)) {
            classPath.add(classes.toString());
        }
        try (Stream<Path> stream = Files.walk(Paths.get("libs"))) {

            classPath.addAll(stream.map(Path::toString).collect(Collectors.toList()));
        } catch (IOException ignored) {
            // ignore
        }

        return classPath.isEmpty() ? null : String.join(",", classPath);
    }

    public static JettyServer get() {
        try {
            return new JettyServer();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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

    public void test() throws Exception {
        var profile = this.webAppContext.getInitParams().get("spring.profiles.active");
        try (var client = start()) {
            client.test(profile == null ? "test-resources" : ("test-resources-" + profile));
        }
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
        Locale.setDefault(Locale.US);
        // set -10 as default
        TimeZone.setDefault(TimeZone.getTimeZone("America/Adak"));
        try {
            this.server.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        return new HttpClient(this, URI.create("http://localhost:" + port));
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
