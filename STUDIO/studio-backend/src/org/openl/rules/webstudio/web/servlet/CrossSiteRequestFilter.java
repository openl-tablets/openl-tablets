package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;

import org.openl.web.AllowedOrigins;

/**
 * Rejects a state-changing request that a cross-site page made on behalf of the signed-in user.
 *
 * <p>The session cookie is sent by the browser on every request to the application, including one a
 * page on another site triggered. Without this filter such a request runs with the victim's session,
 * which is a cross-site request forgery. The filter closes that gap by admitting a state-changing
 * request only when it originates from the application itself or from an origin the administrator has
 * explicitly listed.
 *
 * <p>The decision reads two request headers a browser sets and a page cannot forge:
 * <ul>
 *     <li>{@code Sec-Fetch-Site} is the primary signal. {@code same-origin} and {@code none} (a request
 *     the user started, such as typing the address) are admitted; {@code same-site} and
 *     {@code cross-site} are admitted only for a listed origin. Every browser the frontend supports
 *     sends this header, but only to an HTTPS address or to {@code localhost}.</li>
 *     <li>{@code Origin} is the fallback for a client that sends no {@code Sec-Fetch-Site}, such as a
 *     browser that opened OpenL Studio over plain HTTP. Its host and port must match the address the
 *     request was made to, or be listed. A request that carries no {@code Origin} is admitted as a
 *     non-browser client, which has no ambient session cookie to abuse.</li>
 * </ul>
 *
 * <p>Over plain HTTP the decision therefore depends on the address the request was made to. A reverse
 * proxy in front of OpenL Studio must pass on the public host in {@code Host} or
 * {@code X-Forwarded-Host}, or the application's own state-changing requests are rejected.
 *
 * <p>The list of trusted origins is {@code cors.allowed.origins} — the same setting that opens the API
 * to a browser application on another origin, so an origin allowed to call the API cross-site is also
 * allowed to change state. An asterisk trusts every origin and therefore turns this protection off.
 *
 * <p>Only a state-changing method is guarded; a safe method (GET, HEAD, OPTIONS, TRACE) passes
 * untouched. The filter is mapped to {@code /rest/*}, where the REST API lives, to {@code /mcp/*}, where the MCP
 * server answers with the same session, and to the form sign-in at {@code /login}, so another site cannot sign a
 * browser into an account of its choosing. The single sign-on
 * endpoints — {@code /login/saml2/**}, {@code /login/oauth2/**}, {@code /saml2/**}, {@code /oauth2/**} and
 * {@code /logout}, where the identity provider posts its logout request — are not mapped, so a SAML or OAuth2
 * login redirected back from the identity provider is never mistaken for an attack. A rejected request is
 * answered with {@code 403} and a one-line plain-text reason.
 *
 * @author Yury Molchan
 */
public class CrossSiteRequestFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private static final String REJECTION = "Cross-site request rejected.";

    private AllowedOrigins allowedOrigins = AllowedOrigins.parse(null);

    @Override
    public void init(FilterConfig config) {
        var env = SpringInitializer.getApplicationContext(config.getServletContext()).getEnvironment();
        configure(env.getProperty("cors.allowed.origins"));
    }

    /**
     * Reads the trusted origins from the {@code cors.allowed.origins} value. Package-private so the
     * decision can be exercised without a servlet container.
     */
    void configure(@Nullable String allowed) {
        allowedOrigins = AllowedOrigins.parse(allowed);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        var httpRequest = (HttpServletRequest) request;
        if (isStateChanging(httpRequest.getMethod()) && !isAllowed(httpRequest)) {
            reject((HttpServletResponse) response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(REJECTION);
    }

    private static boolean isStateChanging(String method) {
        return !SAFE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }

    private boolean isAllowed(HttpServletRequest request) {
        var site = request.getHeader("Sec-Fetch-Site");
        if (site != null) {
            return "same-origin".equals(site) || "none".equals(site)
                    || allowedOrigins.contains(request.getHeader("Origin"));
        }
        var origin = request.getHeader("Origin");
        return origin == null || isSameOrigin(origin, request) || allowedOrigins.contains(origin);
    }

    /**
     * Whether the origin names the very address the request was made to. The host and port are read
     * from the request as the application sees it after the forwarded-header filter, so a public host
     * behind a reverse proxy is matched.
     *
     * <p>The scheme itself is not compared: each side drops the default port of its own scheme before
     * the comparison, so a proxy that terminates TLS without announcing it does not turn the
     * application's own request into a rejected one. An opaque ({@code null}) or malformed origin never
     * matches.
     */
    private static boolean isSameOrigin(String origin, HttpServletRequest request) {
        var claimed = authority(origin);
        return claimed != null && claimed.equalsIgnoreCase(
                authority(request.getScheme(), request.getServerName(), request.getServerPort()));
    }

    private static @Nullable String authority(String origin) {
        try {
            var uri = new URI(origin);
            return uri.getHost() == null ? null : authority(uri.getScheme(), uri.getHost(), uri.getPort());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** The {@code host[:port]} of an address, without a port that is the default of its scheme. */
    private static String authority(@Nullable String scheme, String host, int port) {
        var defaultPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        return port < 0 || port == defaultPort ? host : host + ':' + port;
    }
}
