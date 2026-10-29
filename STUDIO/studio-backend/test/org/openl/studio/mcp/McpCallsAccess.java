package org.openl.studio.mcp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Lets a test of another package make the calls of the built-in MCP server, as the server makes them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpCallsAccess {

    /** A new secret of the running server, the one its calls carry. */
    public static String newSecret() {
        return McpCalls.newSecret();
    }
}
