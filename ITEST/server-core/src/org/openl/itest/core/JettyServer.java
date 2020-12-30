package org.openl.itest.core;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple wrapper for Jetty Server
 *
 * @author Vladyslav Pikus, Yury Molchan
 */
public class JettyServer {

    private final Server server;

    private JettyServer(String explodedWar, boolean sharedClassloader) {
        this.server = new Server(0);
        this.server.setStopAtShutdown(true);
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setResourceBase(explodedWar);
        webAppContext.setExtraClasspath(getExtraClasspath());
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern", ".*/classes/.*");
        webAppContext
            .setConfigurations(new Configuration[] { new AnnotationConfiguration(), new WebInfConfiguration() });

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

    public static JettyServer start() throws Exception {
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), false);
        jetty.server.start();
        return jetty;
    }

    public static JettyServer startSharingClassLoader() throws Exception {
        JettyServer jetty = new JettyServer(System.getProperty("webservice-webapp"), true);
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
}
