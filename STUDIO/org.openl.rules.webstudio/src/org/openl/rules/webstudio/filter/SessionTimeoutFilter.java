package org.openl.rules.webstudio.filter;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SessionTimeoutFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger(SessionTimeoutFilter.class);

    private static final int REDIRECT_ERROR_CODE = 399;

    private FilterConfig config;
    private String redirectPage;
    private String[] excludePages;

    @Override
    public void destroy() {
        // destroy
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (excludePages != null && excludePages.length > 0) {
            for (String excludePage : excludePages) {
                if (!excludePage.equals("") && request.getRequestURL().indexOf(excludePage) > -1) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }
        }

        if (request.getRequestedSessionId() != null && !(request.isRequestedSessionIdValid() || request.getSession()
            .isNew())) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String redirectUrl = request.getContextPath() + redirectPage;
            log.info("Session Expired: redirect to {} page", redirectPage);

            // Handle Ajax requests
            if (StringUtils.equals(request.getHeader("x-requested-with"), "XMLHttpRequest") // jQuery / Prototype
                    || StringUtils.equals(request.getHeader("faces-request"), "partial/ajax")) { // JSF 2 / RichFaces
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
            log.warn("Session Timeout filter: could not get an initial parameter 'redirectPage'");
        }
        String excludePagesStr = config.getInitParameter("excludePages");
        if (excludePagesStr != null) {
            excludePages = excludePagesStr.split(",");
        }
    }
}
