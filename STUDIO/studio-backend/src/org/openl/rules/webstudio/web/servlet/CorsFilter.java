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

import org.openl.web.CorsPolicy;

/**
 * Gives the CORS answer of the {@code cors.*} settings, as {@link CorsPolicy} describes it. Registered once, in
 * {@code web.xml}, ahead of the security filter, so a pre-flight needs no authentication and a {@code 401} or
 * {@code 403} answer carries the headers a browser needs to read it. A second registration would repeat every
 * header.
 */
public class CorsFilter implements Filter {

    private CorsPolicy cors;

    @Override
    public void init(FilterConfig config) {
        cors = CorsPolicy.read(SpringInitializer.getApplicationContext(config.getServletContext()).getEnvironment());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (cors.handle((HttpServletRequest) request, (HttpServletResponse) response)) {
            return;
        }
        chain.doFilter(request, response);
    }
}
