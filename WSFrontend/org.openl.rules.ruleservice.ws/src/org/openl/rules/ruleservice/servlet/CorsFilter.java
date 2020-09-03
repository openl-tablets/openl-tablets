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

import org.openl.util.StringUtils;
import org.springframework.core.env.Environment;

@WebFilter(value = "/*")
public class CorsFilter implements Filter {

    private String[] allowedOrigins;
    private boolean allowedAny;
    private String allowedMethods;
    private String allowedHeaders;
    private String maxAge;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestOrigin = httpRequest.getHeader("Origin");

        if (isAllowedOrigin(requestOrigin)) {
            httpResponse.addHeader("Access-Control-Allow-Origin", requestOrigin);
            httpResponse.addHeader("Access-Control-Max-Age", maxAge);
            httpResponse.addHeader("Access-Control-Allow-Methods", allowedMethods);
            httpResponse.addHeader("Access-Control-Allow-Headers", allowedHeaders);
            if ("OPTIONS".equals(httpRequest.getMethod())) {
                httpResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
        }

        if (request.getCharacterEncoding() == null) {
            // In case if charset was not set in the request.
            // UTF-8 is used as default instead of ISO-8859-1
            request.setCharacterEncoding("UTF-8");
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String requestOrigin) {
        if (requestOrigin == null || allowedOrigins == null) {
            return false;
        }
        if (allowedAny) {
            return true;
        }
        for (String allowed : allowedOrigins) {
            if (allowed.equalsIgnoreCase(requestOrigin)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FilterConfig config) {
        Environment env = SpringInitializer.getApplicationContext(config.getServletContext()).getEnvironment();

        String allowed = env.getProperty("cors.allowed.origins");
        allowedAny = "*".equals(allowed);
        allowedOrigins = StringUtils.split(allowed, ',');
        allowedMethods = env.getProperty("cors.allowed.methods");
        allowedHeaders = env.getProperty("cors.allowed.headers");
        maxAge = env.getProperty("cors.preflight.maxage");
    }

    @Override
    public void destroy() {
    }
}
