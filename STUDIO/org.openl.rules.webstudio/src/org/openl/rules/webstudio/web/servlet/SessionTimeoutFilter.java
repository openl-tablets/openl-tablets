package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import java.util.Objects;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTimeoutFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger(SessionTimeoutFilter.class);

    private static final int REDIRECT_ERROR_CODE = 399;

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

        if (request.isRequestedSessionIdValid() && request.getSession(false) == null) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String redirectUrl = request.getContextPath() + redirectPage;
            log.info("Session Expired: redirect to {} page", redirectPage);

            // Handle Ajax requests
            if (Objects.equals(request.getHeader("x-requested-with"), "XMLHttpRequest") // jQuery / Prototype
                    || Objects.equals(request.getHeader("faces-request"), "partial/ajax")) { // JSF 2 / RichFaces
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
        redirectPage = filterConfig.getInitParameter("redirectPage");
        if (redirectPage == null) {
            redirectPage = "";
            log.warn("Session Timeout filter: could not get an initial parameter 'redirectPage'");
        }
        String excludePagesStr = filterConfig.getInitParameter("excludePages");
        if (excludePagesStr != null) {
            excludePages = excludePagesStr.split(",");
        }
    }
}
