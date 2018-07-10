package org.openl.itest.core.jetty;

/**
 * @author Vladyslav Pikus
 */
public class JettyManager {

    private static final ThreadLocal<JettyServer> CONTAINER = new ThreadLocal<>();

    public static JettyServer createServer(ClassLoader classLoader, String explodedWarLocation) {
        if (CONTAINER.get() != null) {
            throw new IllegalStateException("Cannot create a new instance of Jetty. It's already exist");
        }
        JettyServer server = new JettyServer(classLoader, explodedWarLocation);
        CONTAINER.set(server);
        return server;
    }

    public static JettyServer getServer() {
        JettyServer server = CONTAINER.get();
        if (server == null) {
            throw new IllegalStateException("Cannot get an instance of Jetty for current Thread");
        }
        return server;
    }

    public static void removeAndStopServer() throws Exception {
        JettyServer server = CONTAINER.get();
        if (server != null) {
            try {
                server.stop();
            } finally {
                CONTAINER.remove();
            }
        }
    }
}
