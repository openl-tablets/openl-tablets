package org.openl.security.oauth2;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.openl.util.StringUtils;

/**
 * OAuth2 configuration.
 */
public class OAuth2Configuration {

    private static final String OIDC_METADATA_PATH = "/.well-known/openid-configuration";
    private static final String OAUTH_METADATA_PATH = "/.well-known/oauth-authorization-server";

    private static final String ERROR_MESSAGE = "Unable to resolve the Configuration with the provided Issuer of \"%s\"";

    private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<>() {
    };

    private final String issuer;
    private final Map<String, Object> configuration;

    public OAuth2Configuration(String issuer) {
        this.issuer = issuer;
        var uri = URI.create(issuer);
        configuration = getConfiguration(oidc(uri), oidcRfc8414(uri), oauth(uri));
    }

    /**
     * Returns the introspection endpoint URL.
     *
     * @return the introspection endpoint URL
     */
    public Optional<String> getIntrospectionEndpoint() {
        return Optional.ofNullable(configuration.get("introspection_endpoint"))
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank);
    }

    private Map<String, Object> getConfiguration(URI... uris) {
        var rest = new RestTemplate();
        for (URI uri : uris) {
            try {
                RequestEntity<Void> request = RequestEntity.get(uri).build();
                var response = rest.exchange(request, STRING_OBJECT_MAP);
                return response.getBody();
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                if (!(ex instanceof HttpClientErrorException && ((HttpClientErrorException) ex).getStatusCode()
                        .is4xxClientError())) {
                    throw new IllegalArgumentException(String.format(ERROR_MESSAGE, issuer), ex);
                }
                // else try another endpoint
            }
        }
        throw new IllegalArgumentException(String.format(ERROR_MESSAGE, issuer));
    }

    private static URI oidc(URI issuer) {
        return UriComponentsBuilder.fromUri(issuer)
                .replacePath(issuer.getPath() + OIDC_METADATA_PATH)
                .build(Collections.emptyMap());
    }

    private static URI oidcRfc8414(URI issuer) {
        return UriComponentsBuilder.fromUri(issuer)
                .replacePath(OIDC_METADATA_PATH + issuer.getPath())
                .build(Collections.emptyMap());
    }

    private static URI oauth(URI issuer) {
        return UriComponentsBuilder.fromUri(issuer)
                .replacePath(OAUTH_METADATA_PATH + issuer.getPath())
                .build(Collections.emptyMap());
    }

}
