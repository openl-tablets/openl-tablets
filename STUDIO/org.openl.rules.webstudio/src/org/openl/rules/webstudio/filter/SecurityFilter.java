package org.openl.rules.webstudio.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.filter.DelegatingFilterProxy;

public class SecurityFilter extends DelegatingFilterProxy {

    private boolean configured;

    @Override
    protected void initFilterBean() throws ServletException {
        if (configured) {
            super.initFilterBean();
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if (!configured) {
            configured = System.getProperty("webstudio.configured") != null
                    && System.getProperty("webstudio.configured").equals("true");

            if (configured) {
                super.initFilterBean();
            }
        }

        if (!configured) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            super.doFilter(servletRequest, servletResponse, filterChain);

            if (servletRequest instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpSession session = request.getSession(false);

                if(session != null) {
                    // Log authentication errors if a backend authentication repository is unavailable, for example
                    Throwable ex = (Throwable) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                    if (ex instanceof AuthenticationServiceException) {
                        if (logger.isErrorEnabled()) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

    }

}
