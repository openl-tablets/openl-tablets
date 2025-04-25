package org.openl.example.auth;

import java.util.Base64;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Order(1)
public class OpenApiAuthChecker implements AuthorizationChecker {

    @Override
    public boolean authorize(HttpServletRequest request) {
        if (OpenAPIUrlMatcher.isOpenApiUrl(request.getRequestURI())) {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return false;
            }

            String base64Credentials = authHeader.substring("Basic ".length());
            byte[] credBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credBytes);

            String[] values = credentials.split(":", 2);
            if (values.length != 2) {
                return false;
            }

            String username = values[0];
            String password = values[1];

            String expectedUsername = System.getenv("AUTH_OPENAPI_USERNAME");
            String expectedPassword = System.getenv("AUTH_OPENAPI_PASSWORD");

            return username != null && password != null &&
                    username.equals(expectedUsername) &&
                    password.equals(expectedPassword);
        } else {
            return false;
        }
    }
}
