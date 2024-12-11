package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;

import org.openl.util.StringUtils;

@WebFilter("/*")
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
