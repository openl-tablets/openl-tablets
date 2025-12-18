package org.openl.rules.webstudio.web.servlet.wellknown;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(resp.getOutputStream(), getResourceMetadata(req));
    }

    private void handleNotFound(HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private ResourceMetadataModel getResourceMetadata(HttpServletRequest request) {
        ResourceMetadataModel resourceMetadata = new ResourceMetadataModel();
        resourceMetadata.setAuthorizationServers(getAuthorizationServers());
        resourceMetadata.setScopesSupported(getScopesSupported());
        resourceMetadata.setResource(RequestPathUtils.getRequestBasePath(request));
        return resourceMetadata;
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