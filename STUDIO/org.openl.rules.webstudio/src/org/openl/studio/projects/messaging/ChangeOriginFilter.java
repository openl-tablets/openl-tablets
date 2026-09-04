package org.openl.studio.projects.messaging;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import org.openl.rules.webstudio.web.servlet.SpringInitializer;

/**
 * Marks the client of every request that changes something as a recent writer, so the changes that
 * request causes elsewhere can still name it.
 *
 * <p>A write lands in the workspace or the design repository, and what notices it — the files
 * watcher, the repository index — runs on its own thread, where the request is long gone. The mark
 * is taken when the request starts and again when it ends, so a slow write stays attributable for
 * the whole time its consequences are still arriving.
 *
 * <p>A filter rather than a handler interceptor: the writes come from every mount of the
 * application — the REST API an integration or an MCP server calls, and the legacy pages, which the
 * Spring dispatcher never sees.
 *
 * <p>The filter belongs to the container, and it takes the resolver from the application context
 * once per request. Taking it when the filter starts instead would tie the start of the whole web
 * application to a configuration reload running at that moment, and would leave the filter with a
 * resolver of a closed context after every reload (EPBDS-16473).
 */
public class ChangeOriginFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (HttpMethod.GET.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        var changeOrigin = resolverOf(request);
        changeOrigin.remember(request);
        try {
            chain.doFilter(request, response);
        } finally {
            changeOrigin.remember(request);
        }
    }

    /**
     * Returns the resolver of the application configuration in force, waiting out a configuration
     * reload that is running.
     */
    private static ChangeOriginResolver resolverOf(HttpServletRequest request) {
        var servletContext = request.getServletContext();
        var readLock = SpringInitializer.getLock(servletContext);
        readLock.lock();
        try {
            return SpringInitializer.getApplicationContext(servletContext).getBean(ChangeOriginResolver.class);
        } finally {
            readLock.unlock();
        }
    }
}
