package org.openl.rules.webstudio.web.servlet.wellknown;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;

import org.openl.rules.spring.openapi.RequestPathUtils;
import org.openl.rules.webstudio.web.servlet.SpringInitializer;
import org.openl.util.StringUtils;

/**
 * Servlet that handles requests to the /.well-known/* endpoints.
 *
 * <p>The <code>/.well-known</code> path is defined by RFC standards for
 * well-known URIs and is used for service discovery by clients, including
 * OAuth 2.0 and MCP clients.</p>
 *
 * @author Aliaksei Tsymbalist
 */
@WebServlet("/.well-known/*")
public class WellKnownServlet extends HttpServlet {

    private static final String PROTECTED_RESOURCE_METADATA_PATH = "/oauth-protected-resource";
    private static final String PROTECTED_RESOURCE_METADATA_RESPONSE_TEMPLATE_JSON = """
            '{'
              "resource": "{0}",
              "authorization_servers": [{1}],
              "scopes_supported": [{2}]
            '}'
            """;

    private Environment environment;

    @Override
    public void init(ServletConfig config) throws ServletException {
        environment = SpringInitializer.getApplicationContext(config.getServletContext()).getEnvironment();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (oauth2ModeEnabled() && req.getPathInfo().equals(PROTECTED_RESOURCE_METADATA_PATH)) {
            handleProtectedResourceMetadata(req, resp);
        } else {
            handleNotFound(resp);
        }
    }

    private boolean oauth2ModeEnabled() {
        return "oauth2".equals(environment.getProperty("user.mode"));
    }

    private void handleProtectedResourceMetadata(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        resp.getWriter().write(getResourceMetadata(req));
    }

    private void handleNotFound(HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private String getResourceMetadata(HttpServletRequest request) {
        return MessageFormat.format(
                PROTECTED_RESOURCE_METADATA_RESPONSE_TEMPLATE_JSON,
                RequestPathUtils.getRequestBasePath(request),
                getAuthorizationServers().stream()
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")),
                getScopesSupported().stream()
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", "))

        );
    }

    private List<String> getAuthorizationServers() {
        String issuer = environment.getProperty("security.oauth2.issuer-uri");
        return issuer != null ? List.of(issuer) : List.of();
    }

    private List<String> getScopesSupported() {
        String scope = environment.getProperty("security.oauth2.scope");
        return scope != null ? List.of(StringUtils.split(scope, ',')) : List.of();
    }
}
