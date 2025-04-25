package org.openl.example.auth;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.api.AccessDeniedHandler;

@Order(0)
@Component
public class ClientAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (OpenAPIUrlMatcher.isOpenApiUrl(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Example-app\"");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or incorrect client-id or client-secret\"}");
        }
    }
}
