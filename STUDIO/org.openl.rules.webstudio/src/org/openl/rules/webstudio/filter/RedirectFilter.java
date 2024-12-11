package org.openl.rules.webstudio.filter;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.info.OpenLInfoLogger;

public class RedirectFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger(RedirectFilter.class);

    private String redirectPage;

    public void setRedirectPage(String redirectPage) {
        this.redirectPage = redirectPage;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        setRedirectPage(filterConfig.getInitParameter("redirectPage"));
    }

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
        OpenLInfoLogger.memStat();
    }
}
