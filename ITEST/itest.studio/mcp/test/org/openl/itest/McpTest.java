package org.openl.itest;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

/**
 * OpenL Studio runs its MCP server and serves it at {@code /mcp}, behind its own security.
 *
 * @author Yury Molchan
 */
class McpTest {

    /**
     * In the single-user mode the server needs no credentials. A web page of an origin the CORS settings allow gets the
     * CORS headers. A client of the 2025-11-25 protocol still works in a session of its own.
     */
    @Test
    void servesTheMcpServerAtMcp() throws Exception {
        var studio = studio(false).withInitParam("cors.allowed.origins", "https://inspector.example.com");
        try (var client = studio.start()) {
            client.test("test-resources");
        }
    }

    /**
     * Every tool of the server is called against a real OpenL Studio. The multi-user mode lets the requests carry a
     * personal access token, so the state Studio keeps for one client — the opened project, test and run results,
     * the debug session, merge conflicts — outlives each stateless MCP request. A request without a valid token is
     * refused by OpenL Studio before it reaches the server.
     */
    @Test
    void coversEveryTool() throws Exception {
        try (var client = studio(true).start()) {
            client.test("test-resources-tools");
        }
    }

    private static JettyServer studio(boolean multiUser) {
        var studio = JettyServer.get()
                .withInitParam("mcp.enabled", "true")
                .withInitParam("mcp.node", node());
        if (multiUser) {
            studio.withInitParam("user.mode", "multi")
                    .withInitParam("security.administrators", "admin")
                    .withInitParam("db.url", "jdbc:h2:mem:mcp-users;DB_CLOSE_DELAY=-1");
        }
        return studio;
    }

    /** The Node.js runtime the build of this suite installs. */
    private static String node() {
        var windows = System.getProperty("os.name").startsWith("Windows");
        return Path.of("target", "node", windows ? "node.exe" : "node").toAbsolutePath().toString();
    }
}
