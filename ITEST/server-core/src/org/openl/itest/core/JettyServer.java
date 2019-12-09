package org.openl.itest.core;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;

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
        if (sharedClassloader) {
            webAppContext.setClassLoader(JettyServer.class.getClassLoader());
        }
        this.server.setHandler(webAppContext);
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
        return HttpClient.create("http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort());
    }
}
