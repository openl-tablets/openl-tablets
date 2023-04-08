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

import org.openl.rules.ruleservice.spring.JWTValidator;
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
@WebFilter("/*")
public class SecurityFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

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
            try {
                jwtValidator.validateToken(httpRequest);
            } catch (Exception e) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                log.warn(e.getMessage());
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
        if (pathInfo.startsWith(SwaggerUIResolver.SWAGGER_UI)) {
            return false;
        }
        return !StaticResourceResolver.isResourceExists(pathInfo);
    }

    @Override
    public void destroy() {
    }
}
