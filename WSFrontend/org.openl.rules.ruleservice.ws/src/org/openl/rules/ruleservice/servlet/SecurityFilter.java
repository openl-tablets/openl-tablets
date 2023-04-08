package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

/**
 * Requests authorization for deployed OpenL projects.
 * Static web resources such as favicons, css and others supplement resources are accessed without authorization.
 *
 * @author Yury Molchan
 */
@WebFilter("/*")
public class SecurityFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private List<AuthorizationChecker> authorizationCheckers;

    @Override
    public void init(FilterConfig config) {
        var applicationContext = SpringInitializer.getApplicationContext(config.getServletContext());
        authorizationCheckers = new ArrayList<>(applicationContext.getBeansOfType(AuthorizationChecker.class).values());
        authorizationCheckers.sort(AnnotationAwareOrderComparator.INSTANCE);
        log.info("Available Authorization checkers: {}", Arrays.toString(authorizationCheckers.toArray()));
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        var request = (HttpServletRequest) req;
        var response = (HttpServletResponse) resp;

        if (authorizationCheckers.isEmpty() || skipAuthorization(request)) {
            chain.doFilter(req, resp);
            return;
        }

        try {
            for (AuthorizationChecker validator : authorizationCheckers) {
                if (validator.authorize(request)) {
                    log.debug("Authorized: {} {}; by: {}", request.getMethod(), request.getRequestURL(), validator);
                    chain.doFilter(req, resp);
                    return;
                }
            }
            log.info("Unauthorized: {} {};", request.getMethod(), request.getRequestURL());
        } catch (Exception e) {
            log.warn("Unauthorized: {} {};", request.getMethod(), request.getRequestURL(), e);
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean skipAuthorization(HttpServletRequest httpRequest) {
        String pathInfo = httpRequest.getPathInfo();
        // Check if home url.
        if (pathInfo == null || pathInfo.equals("/")) {
            return true;
        }
        // Skip authorization for informational and healthcheck urls
        if (pathInfo.startsWith("/admin/healthcheck/") ||
            pathInfo.startsWith("/admin/info/") ||
            pathInfo.startsWith("/admin/config/")) {
            return true;
        }
        // Do not check static resources such as images, icons etc.
        if (pathInfo.startsWith(SwaggerUIResolver.SWAGGER_UI)) {
            return true;
        }
        return StaticResourceResolver.isResourceExists(pathInfo);
    }

    @Override
    public void destroy() {
        authorizationCheckers.clear();
        authorizationCheckers = null;
    }
}
