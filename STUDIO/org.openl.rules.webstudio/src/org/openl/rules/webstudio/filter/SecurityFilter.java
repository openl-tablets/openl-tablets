package org.openl.rules.webstudio.filter;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openl.rules.webstudio.web.servlet.SpringInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.WebAttributes;

public class SecurityFilter implements Filter {

    private Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        ServletContext sc = servletRequest.getServletContext();
        Lock readLock = SpringInitializer.getLock(sc);
        readLock.lock();
        try {
            Filter filterChainProxy = SpringInitializer.getApplicationContext(sc)
                .getBean("filterChainProxy", Filter.class);
            filterChainProxy.doFilter(servletRequest, servletResponse, filterChain);
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            readLock.unlock();
        }
        SpringInitializer.refresh(sc);
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

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void destroy() {

    }
}
