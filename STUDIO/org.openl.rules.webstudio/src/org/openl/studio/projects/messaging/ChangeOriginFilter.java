package org.openl.studio.projects.messaging;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
 */
@Component("changeOriginFilter")
@RequiredArgsConstructor
public class ChangeOriginFilter extends OncePerRequestFilter {

    private final ChangeOriginResolver changeOrigin;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (HttpMethod.GET.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        changeOrigin.remember(request);
        try {
            chain.doFilter(request, response);
        } finally {
            changeOrigin.remember(request);
        }
    }
}
