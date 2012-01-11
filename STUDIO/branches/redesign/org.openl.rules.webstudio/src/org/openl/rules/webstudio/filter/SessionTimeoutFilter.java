package org.openl.rules.webstudio.filter;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SessionTimeoutFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(SessionTimeoutFilter.class);

    private static final int REDIRECT_ERROR_CODE = 399;

    private FilterConfig config;
    private String redirectPage;
    private String[] excludePages;

    @Override
    public void destroy() {
        // destroy
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
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
            String redirectUrl = request.getContextPath() + redirectPage;
            LOG.info("Session Expired: redirect to " + redirectPage + " page");

            // Handle Ajax requests
            if (StringUtils.equals(request.getHeader("x-requested-with"), "XMLHttpRequest")       // jQuery / Prototype
                   ||  StringUtils.equals(request.getHeader("faces-request"), "partial/ajax")) {  // JSF 2 / RichFaces
                response.setHeader("Location", redirectUrl);
                response.sendError(REDIRECT_ERROR_CODE);
            } else {
                response.sendRedirect(redirectUrl);
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
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
