package org.openl.itest.core.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.net.Socket;

/**
 * Simple wrapper for Jetty Server
 *
 * @author Vladyslav Pikus
 */
public class JettyServer {

    private final Server server;
    private final String baseURI;

    JettyServer(ClassLoader classLoader, String explodedWar) {
        int port = chooseFreePort();
        this.baseURI = "http://localhost:" + port + "/ws";

        this.server = new Server(port);
        this.server.setStopAtShutdown(true);
        this.server.setHandler(createWebAppContext(classLoader, explodedWar));
    }

    private WebAppContext createWebAppContext(ClassLoader classLoader, String explodedWar) {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setDescriptor(explodedWar + "/WEB-INF/web.xml");
        webAppContext.setContextPath("/ws");
        webAppContext.setResourceBase(explodedWar);
        webAppContext.setClassLoader(classLoader);
        return webAppContext;
    }

    private int chooseFreePort() {
        for (int port = 49152; port < 49160; port++) {
            try {
                new Socket("localhost", port);
            } catch (IOException e) {
                return port;
            }
        }
        throw new IllegalStateException("Cannot choose a free port");
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
