package org.openl.rules.webstudio.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.filter.DelegatingFilterProxy;

public class SecurityFilter extends DelegatingFilterProxy {
    private Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        try {
            super.doFilter(servletRequest, servletResponse, filterChain);
        } catch (RuntimeException e) {
            // Log unhandled exceptions
            logger.error(e.getMessage(), e);
            throw e;
        }

        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpSession session = request.getSession(false);

            if (session != null) {
                // Log authentication errors if a backend authentication repository is unavailable, for example
                Throwable ex = (Throwable) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (ex instanceof AuthenticationServiceException) {
                    logger.error("Authentication error.", ex);
                }
            }
        }

    }
}
