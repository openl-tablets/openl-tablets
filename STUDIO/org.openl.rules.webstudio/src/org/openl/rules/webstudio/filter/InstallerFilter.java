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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class InstallerFilter implements Filter {

    private final Log LOG = LogFactory.getLog(InstallerFilter.class);

    private static final int REDIRECT_ERROR_CODE = 399;

    private FilterConfig config;
    private String wizardRoot;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
        wizardRoot = config.getInitParameter("wizardRoot");
        if (wizardRoot == null) {
            LOG.error("Installer filter: could not get an initial parameter 'wizardRoot'");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        boolean configured = System.getProperty("webstudio.configured") != null
                && System.getProperty("webstudio.configured").equals("true");

        if (wizardRoot != null && !configured
                && request.getRequestURL().indexOf(wizardRoot) < 0) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String redirectUrl = request.getContextPath() + wizardRoot + "index.xhtml";

            LOG.info("WebStudio configuration: Redirect to Installation wizard");

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
    public void destroy() {
        // Destroy
    }

}
