package org.openl.studio.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class McpCallsTest {

    @Test
    void namesTheClientOfACallOfTheRunningServer() {
        var secret = McpCalls.newSecret();

        assertEquals("203.0.113.7", McpCalls.clientAddressOf(call(secret, " 203.0.113.7 ")));
    }

    @Test
    void takesARequestWithoutTheSecretOfTheRunningServerForNoCallOfIt() {
        var secret = McpCalls.newSecret();

        assertNull(McpCalls.clientAddressOf(call(null, "203.0.113.7")), "No secret");
        assertNull(McpCalls.clientAddressOf(call(secret + "x", "203.0.113.7")), "Another secret");
        assertNull(McpCalls.clientAddressOf(call("", "203.0.113.7")), "An empty secret");
    }

    @Test
    void namesNoClientForACallThatNamesNone() {
        var secret = McpCalls.newSecret();

        assertNull(McpCalls.clientAddressOf(call(secret, null)));
        assertNull(McpCalls.clientAddressOf(call(secret, " ")));
    }

    private static MockHttpServletRequest call(String secret, String clientAddress) {
        var request = new MockHttpServletRequest();
        if (secret != null) {
            request.addHeader("OpenL-Mcp-Secret", secret);
        }
        if (clientAddress != null) {
            request.addHeader("OpenL-Client-Address", clientAddress);
        }
        return request;
    }
}
