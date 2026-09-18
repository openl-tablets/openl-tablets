package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openl.web.AllowedOrigins;

/**
 * Answers the cross-origin requests of the browser pages whose origins {@code cors.allowed.origins} lists.
 *
 * <p>{@code web.xml} registers the filter, first of all the filters. An annotation would register it a second time,
 * and every CORS header would be sent twice, which a browser refuses.
 */
public class CorsFilter implements Filter {

    private AllowedOrigins allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private String exposedHeaders;
    private String maxAge;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;
        var requestOrigin = httpRequest.getHeader("Origin");

        if (allowedOrigins.contains(requestOrigin)) {
            httpResponse.addHeader("Access-Control-Allow-Origin", requestOrigin);
            httpResponse.addHeader("Access-Control-Max-Age", maxAge);
            httpResponse.addHeader("Access-Control-Allow-Methods", allowedMethods);
            httpResponse.addHeader("Access-Control-Allow-Headers", allowedHeaders);
            if (StringUtils.isNotBlank(exposedHeaders)) {
                httpResponse.addHeader("Access-Control-Expose-Headers", exposedHeaders);
            }
            if ("OPTIONS".equals(httpRequest.getMethod())) {
                httpResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig config) {
        var env = SpringInitializer.getApplicationContext(config.getServletContext()).getEnvironment();

        allowedOrigins = AllowedOrigins.parse(env.getProperty("cors.allowed.origins"));
        allowedMethods = env.getProperty("cors.allowed.methods");
        allowedHeaders = env.getProperty("cors.allowed.headers");
        exposedHeaders = env.getProperty("cors.exposed.headers");
        maxAge = env.getProperty("cors.preflight.maxage");
    }
}
