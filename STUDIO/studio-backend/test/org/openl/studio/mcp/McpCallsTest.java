package org.openl.studio.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import org.openl.studio.mcp.McpCalls.McpCall;

class McpCallsTest {

    @Test
    void namesTheClientOfACallOfTheRunningServer() {
        var secret = McpCalls.newSecret();

        assertEquals(new McpCall("203.0.113.7", "claude-code 2.1.0"),
                McpCalls.callOf(call(secret, " 203.0.113.7 ", " claude-code 2.1.0 ")));
    }

    @Test
    void takesARequestWithoutTheSecretOfTheRunningServerForNoCallOfIt() {
        var secret = McpCalls.newSecret();

        assertNull(McpCalls.callOf(call(null, "203.0.113.7", "claude-code")), "No secret");
        assertNull(McpCalls.callOf(call(secret + "x", "203.0.113.7", "claude-code")), "Another secret");
        assertNull(McpCalls.callOf(call("", "203.0.113.7", "claude-code")), "An empty secret");
    }

    @Test
    void takesACallThatNamesNothingAsACallAllTheSame() {
        var secret = McpCalls.newSecret();

        assertEquals(new McpCall(null, null), McpCalls.callOf(call(secret, null, null)));
        assertEquals(new McpCall(null, null), McpCalls.callOf(call(secret, " ", " ")));
    }

    private static MockHttpServletRequest call(String secret, String clientAddress, String client) {
        var request = new MockHttpServletRequest();
        if (secret != null) {
            request.addHeader("OpenL-Mcp-Secret", secret);
        }
        if (clientAddress != null) {
            request.addHeader("OpenL-Client-Address", clientAddress);
        }
        if (client != null) {
            request.addHeader("OpenL-Mcp-Client", client);
        }
        return request;
    }
}
