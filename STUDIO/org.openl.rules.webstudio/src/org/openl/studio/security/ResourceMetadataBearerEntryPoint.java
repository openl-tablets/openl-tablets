package org.openl.studio.security;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.shibboleth.shared.primitive.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

import org.openl.rules.spring.openapi.RequestPathUtils;

/**
 * An {@link AuthenticationEntryPoint} that adds a WWW-Authenticate header
 * containing the OAuth 2.0 Bearer challenge and resource metadata information.
 *
 * <p>The <code>WWW-Authenticate</code> header is defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750</a>
 * (OAuth 2.0 Bearer Token Usage) and is sent in HTTP 401 Unauthorized responses
 * to indicate that the client must authenticate using a bearer token.</p>
 *
 * <p>This entry point is intended for use with OAuth 2.0 protected resources
 * and MCP clients. It constructs the <code>WWW-Authenticate</code> header
 * with parameters including <code>resource_metadata</code>, <code>realm</code>,
 * <code>error</code>, <code>error_description</code>, <code>error_uri</code>, and <code>scope</code>
 * depending on the authentication failure.</p>
 *
 * <p>The <code>resource_metadata</code> parameter points to the
 * <code>/.well-known/oauth-protected-resource</code> endpoint for this application,
 * enabling clients to discover resource capabilities and supported scopes.</p>
 *
 * @author Aliaksei Tsymbalist
 */
public class ResourceMetadataBearerEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceMetadataBearerEntryPoint.class);

    private static final String RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";

    private String realmName;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        LOG.debug("Authentication failed: {}", authException.getMessage());

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("resource_metadata", RequestPathUtils.getRequestBasePath(request) + RESOURCE_METADATA_PATH);

        if (this.realmName != null) {
            parameters.put("realm", this.realmName);
        }
        if (authException instanceof OAuth2AuthenticationException oAuth2AuthenticationException) {
            OAuth2Error error = oAuth2AuthenticationException.getError();
            parameters.put("error", error.getErrorCode());
            if (StringUtils.hasText(error.getDescription())) {
                parameters.put("error_description", error.getDescription());
            }
            if (StringUtils.hasText(error.getUri())) {
                parameters.put("error_uri", error.getUri());
            }
            if (error instanceof BearerTokenError bearerTokenError) {
                if (StringUtils.hasText(bearerTokenError.getScope())) {
                    parameters.put("scope", bearerTokenError.getScope());
                }
                status = bearerTokenError.getHttpStatus();
            }
        }
        String wwwAuthenticate = computeWWWAuthenticateHeaderValue(parameters);
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
        response.setStatus(status.value());
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    private static String computeWWWAuthenticateHeaderValue(Map<String, String> parameters) {
        StringBuilder wwwAuthenticate = new StringBuilder();
        wwwAuthenticate.append("Bearer");
        if (!parameters.isEmpty()) {
            wwwAuthenticate.append(" ");
            var iterator = parameters.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                wwwAuthenticate.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
                if (iterator.hasNext()) {
                    wwwAuthenticate.append(", ");
                }
            }
        }
        return wwwAuthenticate.toString();
    }
}
