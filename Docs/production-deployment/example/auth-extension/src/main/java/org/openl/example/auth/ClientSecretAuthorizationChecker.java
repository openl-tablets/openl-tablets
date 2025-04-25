package org.openl.example.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Component
@Order(2)
public class ClientSecretAuthorizationChecker implements AuthorizationChecker {
    
    @Override
    public boolean authorize(HttpServletRequest request) {
        if (! OpenAPIUrlMatcher.isOpenApiUrl(request.getRequestURI())) {
            String clientId = request.getHeader("client-id");
            String clientSecret = request.getHeader("client-secret");

            String expectedClientId = System.getenv("AUTH_CLIENT_ID");
            String expectedClientSecret = System.getenv("AUTH_CLIENT_SECRET");

            return clientId != null && clientSecret != null &&
                    clientId.equals(expectedClientId) &&
                    clientSecret.equals(expectedClientSecret);
        } else {
            return false;
        }
    }
}
