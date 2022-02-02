package org.openl.rules.ruleservice.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openl.rules.ruleservice.oauth.JWTValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Requests OAuth2 authentication for deployed OpenL projects.
 * Static web resources such as favicons, css and others supplement resources should be accessed without authentication.
 *
 * @author ybiruk
 */
@WebFilter(value = "/*")
public class SecurityFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private static final String ADMIN_PATH = "/admin/";

    private boolean authOn;
    private JWTValidator jwtValidator;

    @Override
    public void init(FilterConfig filterConfig) {
        ApplicationContext applicationContext = SpringInitializer
            .getApplicationContext(filterConfig.getServletContext());
        Environment env = applicationContext.getEnvironment();
        authOn = Boolean.parseBoolean(env.getProperty("ruleservice.authentication.enabled"));
        jwtValidator = applicationContext.getBean(JWTValidator.class);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (securedUrl(httpRequest)) {
            String jwtToken = httpRequest.getHeader("Authorization");
            if (jwtToken != null) {
                try {
                    jwtValidator.validateToken(jwtToken);
                } catch (Exception e) {
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    log.warn(e.getMessage());
                    return;
                }
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean securedUrl(HttpServletRequest httpRequest) {
        if (!authOn) {
            return false;
        }
        String pathInfo = httpRequest.getPathInfo();
        // Check if home url.
        if (pathInfo == null || pathInfo.equals("/")) {
            return false;
        }
        // Do not check static resources such as images, icons etc.
        if (StaticResourceResolver.isResourceExists(pathInfo)) {
            return false;
        }
        // Swagger and admin actions should be available without authorization.
        // Admin actions such as downloading or deploying via UI should be removed.
        if (pathInfo.startsWith(SwaggerUIResolver.SWAGGER_UI) || pathInfo.startsWith(ADMIN_PATH)) {
            return false;
        }
        // Access to openapi.json and openapi.yam should pass without authorization.
        return !pathInfo.endsWith("openapi.json") && !pathInfo.endsWith("openapi.yaml");
    }

    @Override
    public void destroy() {
    }
}
