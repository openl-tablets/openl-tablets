package org.openl.itest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;

import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

/**
 * OpenL Studio runs its MCP server, and Jetty serves it at {@code /mcp} through a core context that proxies the
 * requests to the server, the way the Docker image does. The suite runs in the single-user mode, so the server needs
 * no credentials.
 *
 * @author Yury Molchan
 */
class McpTest {

    @Test
    void servesTheMcpServerAtMcp() throws Exception {
        var port = freePort();
        var mcpPort = freePort();
        var proxy = new ProxyHandler.Reverse("^https?://[^/]+/(.*)$", "http://127.0.0.1:" + mcpPort + "/$1");
        var mcp = new ContextHandler(proxy, "/mcp");
        mcp.setAllowNullPathInContext(true);
        try (var client = JettyServer.get()
                .withPort(port)
                .withContext(mcp)
                .withInitParam("mcp.enabled", "true")
                .withInitParam("mcp.node", node())
                .withInitParam("mcp.port", String.valueOf(mcpPort))
                .withInitParam("mcp.studio-url", "http://127.0.0.1:" + port)
                .start()) {
            client.test("test-resources");
        }
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
