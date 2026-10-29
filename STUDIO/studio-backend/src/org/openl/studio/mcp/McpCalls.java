package org.openl.studio.mcp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * Tells the calls the built-in MCP server makes to the REST API for its clients from any other request.
 *
 * <p>The MCP server calls the REST API itself, from the address of OpenL Studio, for the client that called it at
 * {@code /mcp}. OpenL Studio names the address of that client in each request it passes to the server, and the server
 * names it back in each of its calls.
 *
 * <p>Each run of the server gets a secret of its own in its environment, and sends it with each of its calls. Only a
 * request that carries the secret of the server running now is taken for one of its calls, so a client cannot claim
 * another address by sending the header itself.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpCalls {

    /** The variable of the environment of the server that holds its secret. */
    static final String SECRET_VARIABLE = "OPENL_MCP_SECRET";
    /** The header a call of the server carries its secret in. */
    static final String SECRET_HEADER = "OpenL-Mcp-Secret";
    /** The header that names the address of the client: in a request to the server, and in a call of the server. */
    static final String CLIENT_ADDRESS_HEADER = "OpenL-Client-Address";

    private static final int SECRET_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** The secret of the server running now, or null before any runs. */
    private static final AtomicReference<@Nullable String> SECRET = new AtomicReference<>();

    /** A new secret for a run of the server. The secret of the previous run is no longer accepted. */
    static String newSecret() {
        var bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        var secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SECRET.set(secret);
        return secret;
    }

    /** The secret of the server running now, or null before any runs. */
    static @Nullable String secret() {
        return SECRET.get();
    }

    /**
     * The address of the client of the MCP server a request is a call for, or null when the request is no call of the
     * running MCP server or names no address.
     */
    public static @Nullable String clientAddressOf(HttpServletRequest request) {
        return isCallOfServer(request) ? StringUtils.trimToNull(request.getHeader(CLIENT_ADDRESS_HEADER)) : null;
    }

    /** Whether a request carries the secret of the server running now. */
    private static boolean isCallOfServer(HttpServletRequest request) {
        var expected = SECRET.get();
        var sent = request.getHeader(SECRET_HEADER);
        // Compared in a time that tells nothing of how much of the secret matched
        return expected != null && sent != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), sent.getBytes(StandardCharsets.UTF_8));
    }
}
