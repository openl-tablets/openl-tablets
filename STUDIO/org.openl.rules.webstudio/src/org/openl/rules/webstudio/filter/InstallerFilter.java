package org.openl.rules.webstudio.filter;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.webstudio.util.PreferencesManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

public class InstallerFilter extends GenericFilterBean {

    private final Logger log = LoggerFactory.getLogger(InstallerFilter.class);

    private static final int REDIRECT_ERROR_CODE = 399;

    private final String wizardRoot;

    public InstallerFilter(String wizardRoot) {
        this.wizardRoot = wizardRoot;

        if (wizardRoot == null) {
            log.error("Installer filter: could not get an initial parameter 'wizardRoot'");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String appName = WebStudioUtils.getApplicationName(getServletContext());
        boolean configured = PreferencesManager.INSTANCE.isAppConfigured(appName);

        if (wizardRoot != null && !configured && request.getRequestURL().indexOf(wizardRoot) < 0) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String redirectUrl = request.getContextPath() + wizardRoot + "index.xhtml";

            log.info("WebStudio configuration: Redirect to Installation wizard");

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

}
