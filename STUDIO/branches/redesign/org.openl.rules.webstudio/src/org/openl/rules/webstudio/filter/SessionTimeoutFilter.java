package org.openl.rules.webstudio.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SessionTimeoutFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(SessionTimeoutFilter.class);

    private static final int REDIRECT_ERROR_CODE = 399;

    private FilterConfig config;
    private String redirectPage;
    private String[] excludePages;

    public void destroy() {
        // destroy
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        /*HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (excludePages != null && excludePages.length > 0) {
            for (String excludePage : excludePages) {
                if (!excludePage.equals("") && request.getRequestURL().indexOf(excludePage) > -1) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }
        }
        if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String redirect = request.getContextPath() + redirectPage;
            LOG.info("Session Timeout filter: redirect to " + redirectPage + " page");
            String xRequested = request.getHeader("X-Requested-With");
            if (xRequested != null && xRequested.equalsIgnoreCase("XMLHttpRequest")) {
                // handle Ajax requests
                response.setHeader("Location", redirect);
                response.sendError(REDIRECT_ERROR_CODE, "Redirect Error");
            } else {
                response.sendRedirect(redirect);
            }
        } else {*/
            filterChain.doFilter(servletRequest, servletResponse);
        //}
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
        redirectPage = config.getInitParameter("redirectPage");
        if (redirectPage == null) {
            redirectPage = "";
            LOG.warn("Session Timeout filter: could not get an initial parameter 'redirectPage'");
        }
        String excludePagesStr = config.getInitParameter("excludePages");
        if (excludePagesStr != null) {
            excludePages = excludePagesStr.split(",");
        }
    }
}
