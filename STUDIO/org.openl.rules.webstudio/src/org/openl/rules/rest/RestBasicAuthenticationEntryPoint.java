package org.openl.rules.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * Authentication entry point for REST services
 */
public class RestBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().println("Unauthorized: " + authException.getMessage());
    }
}
