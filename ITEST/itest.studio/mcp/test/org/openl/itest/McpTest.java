package org.openl.itest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

/**
 * OpenL Studio runs its MCP server, and Jetty serves it at {@code /mcp} through a core context that proxies the
 * requests to the server, the way the Docker image does.
 *
 * @author Yury Molchan
 */
class McpTest {

    /** In the single-user mode the server needs no credentials. */
    @Test
    void servesTheMcpServerAtMcp() throws Exception {
        try (var client = studio(false).start()) {
            client.test("test-resources");
        }
    }

    /**
     * Every tool of the server is called against a real OpenL Studio. The multi-user mode lets the requests carry a
     * personal access token, so the state Studio keeps for one client — the opened project, test and run results,
     * the debug session, merge conflicts — outlives each stateless MCP request.
     */
    @Test
    void coversEveryTool() throws Exception {
        try (var client = studio(true).start()) {
            client.test("test-resources-tools");
        }
    }

    private static JettyServer studio(boolean multiUser) throws IOException {
        var port = freePort();
        var mcpPort = freePort();
        var proxy = new ProxyHandler.Reverse("^https?://[^/]+/(.*)$", "http://127.0.0.1:" + mcpPort + "/$1") {
            // Jetty shares one HTTP/1.1 connection factory between its clients and destroys it with the first server
            // that stops, so the proxy of every server started in this class gets a factory of its own.
            @Override
            protected HttpClient newHttpClient() {
                var transport = new HttpClientTransportDynamic(new ClientConnector(),
                        new HttpClientConnectionFactory.HTTP11());
                return new HttpClient(transport);
            }
        };
        var mcp = new ContextHandler(proxy, "/mcp");
        mcp.setAllowNullPathInContext(true);
        var studio = JettyServer.get()
                .withPort(port)
                .withContext(mcp)
                .withInitParam("mcp.enabled", "true")
                .withInitParam("mcp.node", node())
                .withInitParam("mcp.port", String.valueOf(mcpPort))
                .withInitParam("mcp.studio-url", "http://127.0.0.1:" + port);
        if (multiUser) {
            studio.withInitParam("user.mode", "multi")
                    .withInitParam("security.administrators", "admin")
                    .withInitParam("db.url", "jdbc:h2:mem:mcp-users;DB_CLOSE_DELAY=-1");
        }
        return studio;
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    /** The Node.js runtime the build of this suite installs. */
    private static String node() {
        var windows = System.getProperty("os.name").startsWith("Windows");
        return Path.of("target", "node", windows ? "node.exe" : "node").toAbsolutePath().toString();
    }
}
