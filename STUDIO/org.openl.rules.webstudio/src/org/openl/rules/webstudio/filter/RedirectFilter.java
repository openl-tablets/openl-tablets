package org.openl.rules.webstudio.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedirectFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger(RedirectFilter.class);

    private String redirectPage;

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String redirectUrl = request.getContextPath() + redirectPage;
        log.debug("Redirect to {}", redirectPage);
        response.sendRedirect(redirectUrl);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
        redirectPage = filterConfig.getInitParameter("redirectPage");
    }
}
